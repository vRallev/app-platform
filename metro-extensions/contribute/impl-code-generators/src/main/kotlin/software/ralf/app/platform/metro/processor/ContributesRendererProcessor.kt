package software.ralf.app.platform.metro.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.ralf.app.platform.metro.MetroContextAware
import software.ralf.app.platform.metro.addMetroOriginAnnotation
import software.ralf.app.platform.renderer.metro.RendererKey

/**
 * Generates the code for [ContributesRenderer].
 *
 * In the lookup package [METRO_LOOKUP_PACKAGE] a new interface is generated with a provider method
 * for the renderer, e.g.
 *
 * ```
 * package software.ralf.test
 *
 * @ContributesRenderer
 * class TestRenderer : Renderer<Model>
 * ```
 *
 * Will generate:
 * ```
 * package $METRO_LOOKUP_PACKAGE.software.ralf.test
 *
 * @ContributesTo(RendererScope::class)
 * interface TestRendererGraph {
 *     @Provides
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     fun provideTestRendererIntoMap(
 *         renderer: () -> TestRenderer,
 *     ): Renderer<*> = renderer()
 *
 *     @Provides
 *     fun provideTestRenderer(): TestRenderer = TestRenderer()
 *
 *     @Provides
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     @ForScope(RendererScope::class)
 *     fun provideRendererModelKey(): KClass<out Renderer<*>> =
 *         TestRenderer::class
 * }
 * ```
 */
@OptIn(KspExperimental::class)
internal class ContributesRendererProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, MetroContextAware {

  private val baseModel = ClassName("software.ralf.app.platform.presenter", "BaseModel")
  private val baseModelFqName = baseModel.canonicalName

  private val rendererWildcard =
    ClassName("software.ralf.app.platform.renderer", "Renderer").parameterizedBy(STAR)

  private val rendererScope = ClassName("software.ralf.app.platform.renderer", "RendererScope")
  private val rendererKey = RendererKey::class.asClassName()

  private val singleIn = SingleIn::class.asClassName()

  private val unitFqName = Unit::class.requireQualifiedName()

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesRenderer::class)
      .filterIsInstance<KSClassDeclaration>()
      .onEach {
        checkIsPublic(it)
        checkNoSingleton(it)
        checkNoZeroArgInjectConstructor(it)
        checkSingleConstructorOrInject(it)
      }
      .forEach { generateGraphInterface(it) }

    return emptyList()
  }

  private fun generateGraphInterface(clazz: KSClassDeclaration) {
    val packageName = "${METRO_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val graphClassName = ClassName(packageName, "${clazz.innerClassNames()}Graph")

    val includeSealedSubtypes =
      try {
        clazz.getAnnotationsByType(ContributesRenderer::class).single().includeSealedSubtypes
      } catch (_: NoSuchElementException) {
        /*
        Caused by: java.util.NoSuchElementException: Collection contains no element matching the predicate.
          at com.google.devtools.ksp.UtilsKt.createInvocationHandler$lambda$8(utils.kt:591)
          at jdk.proxy105/jdk.proxy105.$Proxy1029.includeSealedSubtypes(Unknown Source)
          at software.ralf.app.platform.inject.processor.ContributesRendererProcessor.generateComponentInterface(ContributesRendererProcessor.kt:120)

        We're seeing this exception when trying to read 'includeSealedSubtypes' for an annotation
        where the value is not declared, e.g. '@ContributesRenderer' (without any arguments).
        This happens only on iOS for some reason. Fallback to the default value 'true'.
         */
        true
      }

    val allModels =
      if (includeSealedSubtypes) {
        generateSequence(listOf(modelType(clazz))) { classes ->
            classes.flatMap { it.getSealedSubclasses() }.takeIf { it.isNotEmpty() }
          }
          .flatten()
      } else {
        sequenceOf(modelType(clazz))
      }

    val fileSpec =
      FileSpec.builder(graphClassName)
        .addType(
          TypeSpec.interfaceBuilder(graphClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addMetroOriginAnnotation(clazz)
            .addAnnotation(
              AnnotationSpec.builder(ContributesTo::class)
                .addMember("%T::class", rendererScope)
                .build()
            )
            .apply {
              if (!clazz.hasInjectAnnotation()) {
                addFunction(
                  FunSpec.builder("provide${clazz.safeClassName}")
                    .addAnnotation(Provides::class)
                    .returns(clazz.toClassName())
                    .addParameters(clazz.constructorParameters().map { it.toParameterSpec() })
                    .addCode(clazz.constructorCall())
                    .build()
                )
              }
            }
            .addFunctions(allModels.map { createModelBindingFunction(clazz, it) }.toList())
            .addFunctions(allModels.map { createModelKeyFunction(clazz, it) }.toList())
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }

  private fun modelType(clazz: KSClassDeclaration): KSClassDeclaration {
    val annotation = clazz.findAnnotation(ContributesRenderer::class)
    val explicitModelType =
      (annotation.arguments.firstOrNull { it.name?.asString() == "modelType" }
          ?: annotation.arguments.firstOrNull())
        ?.let { (it.value as? KSType)?.declaration as? KSClassDeclaration }
        ?.takeIf { it.requireQualifiedName() != unitFqName }

    if (explicitModelType != null) {
      return explicitModelType
    }

    val implicitModelTypes =
      clazz
        .getAllSuperTypes()
        .flatMap { superType ->
          superType.arguments.filter { it.type?.resolve()?.extendsBaseModel() ?: false }
        }
        .mapNotNull { it.type?.resolve()?.declaration as? KSClassDeclaration }
        .distinctBy { it.requireQualifiedName() }
        .toList()

    check(implicitModelTypes.size == 1, clazz) {
      buildString {
        append(
          "Couldn't find BaseModel type for ${clazz.simpleName.asString()}. " +
            "Consider adding an explicit parameter."
        )
        if (implicitModelTypes.size > 1) {
          append("Found: ")
          append(implicitModelTypes.joinToString { it.requireQualifiedName() })
        }
      }
    }

    return implicitModelTypes[0]
  }

  private fun createModelBindingFunction(
    clazz: KSClassDeclaration,
    modelType: KSClassDeclaration,
  ): FunSpec {
    return FunSpec.builder("provide${clazz.safeClassName}" + modelType.innerClassNames())
      .addAnnotation(Provides::class)
      .addAnnotation(IntoMap::class)
      .addAnnotation(
        AnnotationSpec.builder(rendererKey).addMember("%T::class", modelType.toClassName()).build()
      )
      .addParameter(name = "renderer", type = LambdaTypeName.get(returnType = clazz.toClassName()))
      .returns(rendererWildcard)
      .addStatement("return renderer()")
      .build()
  }

  private fun createModelKeyFunction(
    clazz: KSClassDeclaration,
    modelType: KSClassDeclaration,
  ): FunSpec {
    return FunSpec.builder("provide${clazz.safeClassName}" + modelType.innerClassNames() + "Key")
      .addAnnotation(Provides::class)
      .addAnnotation(IntoMap::class)
      .addAnnotation(
        AnnotationSpec.builder(rendererKey).addMember("%T::class", modelType.toClassName()).build()
      )
      .addAnnotation(
        AnnotationSpec.builder(ForScope::class)
          .addMember("scope = %T::class", rendererScope)
          .build()
      )
      .returns(
        KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(rendererWildcard))
      )
      .addStatement("return %T::class", clazz.toClassName())
      .build()
  }

  private fun checkNoSingleton(clazz: KSClassDeclaration) {
    val hasSingleInAnnotation =
      clazz.annotations.any { annotation ->
        annotation.isAnnotation(singleIn.canonicalName) &&
          clazz.scope().type.declaration.requireQualifiedName() == rendererScope.canonicalName
      }

    if (hasSingleInAnnotation) {
      logger.error(
        "Renderers should not be singletons in the RendererScope. The " +
          "RendererFactory will cache the Renderer when necessary. Remove the " +
          "@SingleIn(RendererScope::class) annotation.",
        clazz,
      )
    }
  }

  private fun checkNoZeroArgInjectConstructor(clazz: KSClassDeclaration) {
    if (clazz.hasInjectAnnotation()) {
      check(clazz.injectConstructorParameters().isNotEmpty(), clazz) {
        "It's redundant to use @Inject when using " +
          "@ContributesRenderer for a Renderer with a zero-arg constructor."
      }
    }
  }

  private fun checkSingleConstructorOrInject(clazz: KSClassDeclaration) {
    if (!clazz.hasInjectAnnotation() && clazz.constructors().count() > 1) {
      check(false, clazz) {
        "${clazz.simpleName.asString()} has multiple constructors. Annotate the constructor " +
          "to use with @Inject, or remove the extra constructors so @ContributesRenderer can " +
          "generate a provider."
      }
    }
  }

  private fun KSClassDeclaration.constructorParameters(): List<KSValueParameter> {
    return providerConstructor()?.parameters.orEmpty()
  }

  private fun KSClassDeclaration.injectConstructorParameters(): List<KSValueParameter> {
    return constructors().firstOrNull { it.isAnnotationPresent(Inject::class) }?.parameters
      ?: constructorParameters()
  }

  private fun KSClassDeclaration.providerConstructor(): KSFunctionDeclaration? {
    return primaryConstructor ?: constructors().singleOrNull()
  }

  private fun KSClassDeclaration.hasInjectAnnotation(): Boolean {
    return isAnnotationPresent(Inject::class) ||
      constructors().any { it.isAnnotationPresent(Inject::class) }
  }

  private fun KSClassDeclaration.constructors(): Sequence<KSFunctionDeclaration> {
    return declarations.filterIsInstance<KSFunctionDeclaration>().filter {
      it.simpleName.asString() == "<init>"
    }
  }

  private fun KSValueParameter.toParameterSpec(): ParameterSpec {
    val parameterName = name?.asString() ?: "parameter"
    return ParameterSpec.builder(parameterName, type.toTypeName())
      .addAnnotations(annotations.map { it.toAnnotationSpec() }.toList())
      .build()
  }

  private fun KSClassDeclaration.constructorCall(): CodeBlock {
    return CodeBlock.builder()
      .add("return %T(", toClassName())
      .apply {
        constructorParameters().forEachIndexed { index, parameter ->
          if (index > 0) {
            add(", ")
          }
          val parameterName = parameter.name?.asString() ?: "parameter"
          add("%N = %N", parameterName, parameterName)
        }
      }
      .add(")\n")
      .build()
  }

  private fun KSType.extendsBaseModel(): Boolean {
    val superTypes =
      (this.declaration as? KSClassDeclaration)?.getAllSuperTypes() ?: emptySequence()

    return superTypes.any { it.declaration.qualifiedName?.asString() == baseModelFqName }
  }
}
