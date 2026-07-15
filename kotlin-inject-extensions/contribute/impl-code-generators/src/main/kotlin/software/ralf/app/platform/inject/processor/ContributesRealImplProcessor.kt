package software.ralf.app.platform.inject.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
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
import com.squareup.kotlinpoet.LambdaTypeName
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
import software.ralf.app.platform.inject.addOriginAnnotation
import software.ralf.app.platform.inject.mock.ContributesRealImpl
import software.ralf.app.platform.inject.mock.MockMode
import software.ralf.app.platform.inject.mock.RealImpl

/**
 * Generates the necessary code in order to support [ContributesRealImpl].
 *
 * If the class implements `Scoped`, then based on the mock mode flag the real implementation gets
 * called or not.
 *
 * ```
 * package app.platform.inject.software.ralf.test
 *
 * @ContributesTo(scope = AppScope::class)
 * public interface RealVtsRealImplComponent {
 *      @Provides
 *      @RealImpl
 *      public fun provideVtsRealImpl(realImpl: RealVts): Vts = realVts
 *
 *      @Provides
 *      @IntoSet
 *      @ForScope(AppScope::class)
 *      fun provideVtsRealImplScoped(
 *          @MockMode mockMode: Boolean,
 *          realImpl: () -> RealVts,
 *      ): Scoped = if (mockMode) Scoped.NO_OP else realImpl()
 * }
 * ```
 */
internal class ContributesRealImplProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, KotlinInjectContextAware {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesRealImpl::class)
      .filterIsInstance<KSClassDeclaration>()
      .onEach { checkIsPublic(it) }
      .forEach { generateComponentInterface(it) }

    return emptyList()
  }

  @OptIn(KspExperimental::class)
  @Suppress("LongMethod")
  private fun generateComponentInterface(clazz: KSClassDeclaration) {
    val packageName = "${APP_PLATFORM_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val componentClassName = ClassName(packageName, "${clazz.innerClassNames()}RealImplComponent")

    val annotations = clazz.findAnnotationsAtLeastOne(ContributesRealImpl::class)
    checkNoDuplicateBoundTypes(clazz, annotations)

    val fileSpec =
      FileSpec.builder(componentClassName)
        .addType(
          TypeSpec.interfaceBuilder(componentClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addOriginAnnotation(clazz)
            .addAnnotation(
              AnnotationSpec.builder(ContributesTo::class)
                .addMember("%T::class", clazz.scope().type.toClassName())
                .build()
            )
            .addFunctions(
              annotations.map { annotation ->
                val boundType = boundType(clazz, annotation)

                check(!boundType.isScoped(), clazz) { "Scoped cannot be used as bound type." }

                FunSpec.builder(
                    "provide${boundType.declaration.simpleName.asString()}" + "RealImpl"
                  )
                  .addAnnotation(Provides::class)
                  .addAnnotation(RealImpl::class)
                  .addParameter("realImpl", clazz.toClassName())
                  .returns(boundType.toClassName())
                  .addStatement("return realImpl")
                  .build()
              }
            )
            .apply {
              if (
                clazz.superTypes.any { it.resolve().isScoped() } &&
                  !clazz.isAnnotationPresent(ContributesBinding::class)
              ) {
                addFunction(
                  FunSpec.builder("provide${clazz.innerClassNames()}Scoped")
                    .addAnnotation(Provides::class)
                    .addAnnotation(IntoSet::class)
                    .addAnnotation(
                      AnnotationSpec.builder(ForScope::class)
                        .addMember("scope = %T::class", clazz.scope().type.toClassName())
                        .build()
                    )
                    .addParameter(
                      ParameterSpec.builder("mockMode", Boolean::class)
                        .addAnnotation(MockMode::class)
                        .build()
                    )
                    .addParameter("realImpl", LambdaTypeName.get(returnType = clazz.toClassName()))
                    .returns(scopedClassName)
                    .addStatement("return if (mockMode) %T.NO_OP else realImpl()", scopedClassName)
                    .build()
                )
              }
            }
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }
}
