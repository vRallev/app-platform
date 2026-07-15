package software.ralf.app.platform.inject.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
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
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.ralf.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.ralf.app.platform.inject.KotlinInjectContextAware
import software.ralf.app.platform.inject.OPEN_SOURCE_LOOKUP_PACKAGE
import software.ralf.app.platform.inject.addOriginAnnotation
import software.ralf.app.platform.ksp.decapitalize

/**
 * Generates the code for [ContributesBinding] and the `Scoped` type.
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
 * class RealAuthenticator : Authenticator, Scoped
 * ```
 *
 * Will generate:
 * ```
 * package $LOOKUP_PACKAGE
 *
 * @Origin(RealAuthenticator::class)
 * interface SoftwareRalfTestRealAuthenticatorScoped {
 *     @Provides
 *     @IntoSet
 *     @ForScope(AppScope::class)
 *     fun provideRealAuthenticatorAuthenticatorScoped(
 *         realAuthenticator: RealAuthenticator
 *     ): Scoped = realAuthenticator
 * }
 * ```
 */
internal class ContributesBindingScopedProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, KotlinInjectContextAware {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesBinding::class)
      .filterIsInstance<KSClassDeclaration>()
      .filter { clazz ->
        val hasSuperType = clazz.superTypes.any { it.resolve().isScoped() }
        if (hasSuperType) return@filter true

        val annotations = clazz.findAnnotationsAtLeastOne(ContributesBinding::class)
        annotations.any { annotation -> boundType(clazz, annotation).isScoped() }
      }
      .onEach {
        checkIsPublic(it)
        checkHasScope(it)
      }
      .forEach { generateComponentInterface(it) }

    return emptyList()
  }

  @Suppress("LongMethod")
  private fun generateComponentInterface(clazz: KSClassDeclaration) {
    val componentPackage = "${APP_PLATFORM_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val componentClassName =
      ClassName(componentPackage, "${clazz.innerClassNames()}ScopedComponent")

    val scope = clazz.scope()

    val fileSpec =
      FileSpec.builder(componentClassName)
        .addType(
          TypeSpec.interfaceBuilder(componentClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addOriginAnnotation(clazz)
            .addAnnotation(
              AnnotationSpec.builder(ContributesTo::class)
                .addMember("scope = %T::class", scope.type.toClassName())
                .build()
            )
            .addFunction(
              FunSpec.builder("provide${clazz.innerClassNames()}Scoped")
                .addAnnotation(Provides::class)
                .addAnnotation(IntoSet::class)
                .addAnnotation(
                  AnnotationSpec.builder(ForScope::class)
                    .addMember("scope = %T::class", scope.type.toClassName())
                    .build()
                )
                .apply {
                  val parameterName = clazz.innerClassNames().decapitalize()
                  addParameter(
                    ParameterSpec.builder(name = parameterName, type = clazz.toClassName()).build()
                  )

                  addStatement("return $parameterName")
                }
                .returns(scopedClassName)
                .build()
            )
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }
}
