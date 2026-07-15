package software.ralf.app.platform.inject.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.ralf.app.platform.inject.KotlinInjectContextAware
import software.ralf.app.platform.inject.OPEN_SOURCE_LOOKUP_PACKAGE
import software.ralf.app.platform.inject.addOriginAnnotation
import software.ralf.app.platform.ksp.argumentOfTypeAt
import software.ralf.app.platform.ksp.decapitalize

/**
 * Generates the code for [ContributesBinding].
 *
 * In the lookup package [OPEN_SOURCE_LOOKUP_PACKAGE] a new interface is generated with a provider
 * method for the annotated type. To avoid name clashes the package name of the original interface
 * is encoded in the interface name. E.g.
 *
 * ```
 * package software.ralf.test
 *
 * @Inject
 * @SingleIn(AppScope::class)
 * @ContributesBinding(AppScope::class)
 * class RealAuthenticator : Authenticator
 * ```
 *
 * Will generate:
 * ```
 * package $LOOKUP_PACKAGE
 *
 * @Origin(RealAuthenticator::class)
 * interface SoftwareRalfTestRealAuthenticator {
 *     @Provides fun provideRealAuthenticatorAuthenticator(
 *         realAuthenticator: RealAuthenticator
 *     ): Authenticator = realAuthenticator
 * }
 * ```
 */
internal class ContributesBindingProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, KotlinInjectContextAware {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesBinding::class)
      .filterIsInstance<KSClassDeclaration>()
      .onEach {
        checkIsPublic(it)
        checkHasScope(it)
      }
      .forEach { generateComponentInterface(it) }

    return emptyList()
  }

  @Suppress("LongMethod")
  private fun generateComponentInterface(clazz: KSClassDeclaration) {
    val componentClassName = ClassName(OPEN_SOURCE_LOOKUP_PACKAGE, clazz.safeClassName)

    val annotations = clazz.findAnnotationsAtLeastOne(ContributesBinding::class)
    checkNoDuplicateBoundTypes(clazz, annotations)

    val boundTypes =
      annotations
        .mapNotNull { annotation ->
          val boundType =
            boundType(clazz, annotation).takeUnless { it.isScoped() } ?: return@mapNotNull null

          GeneratedFunction(
            boundType = boundType,
            multibinding = annotation.argumentOfTypeAt<Boolean>(this, "multibinding") ?: false,
          )
        }
        .distinctBy { it.bindingMethodReturnType.canonicalName + it.multibinding }

    // The only boundType was Scoped, which is handled by a separate processor.
    if (boundTypes.isEmpty()) return

    val fileSpec =
      FileSpec.builder(componentClassName)
        .addType(
          TypeSpec.interfaceBuilder(componentClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addOriginAnnotation(clazz)
            .addFunctions(
              boundTypes.map { function ->
                val multibindingSuffix =
                  if (function.multibinding) {
                    "Multibinding"
                  } else {
                    ""
                  }
                FunSpec.builder(
                    "provide${clazz.innerClassNames()}" +
                      function.bindingMethodReturnType.simpleName +
                      multibindingSuffix
                  )
                  .addAnnotation(Provides::class)
                  .apply {
                    if (function.multibinding) {
                      addAnnotation(IntoSet::class)
                    }
                  }
                  .apply {
                    val parameterName = clazz.innerClassNames().decapitalize()
                    addParameter(
                      ParameterSpec.builder(name = parameterName, type = clazz.toClassName())
                        .build()
                    )

                    addStatement("return $parameterName")
                  }
                  .returns(function.bindingMethodReturnType)
                  .build()
              }
            )
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }

  private inner class GeneratedFunction(boundType: KSType, val multibinding: Boolean) {
    val bindingMethodReturnType by lazy { boundType.toClassName() }
  }
}
