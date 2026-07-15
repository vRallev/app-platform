package software.ralf.app.platform.metro.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.ralf.app.platform.metro.MetroContextAware
import software.ralf.app.platform.metro.addMetroOriginAnnotation

/**
 * Generates the necessary code in order to support [ContributesScoped].
 *
 * ```
 * package app.platform.inject.metro.software.ralf.test
 *
 * @ContributesTo(scope = AbcScope::class)
 * public interface TestClassGraph {
 *
 *   @Binds
 *   val TestClass.bindSuperType: SuperType
 *
 *   @Binds @IntoSet @ForScope(UserScope::class)
 *   val TestClass.bindScoped: Scoped
 * }
 * ```
 */
@OptIn(KspExperimental::class)
internal class ContributesScopedProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, MetroContextAware {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesScoped::class)
      .filterIsInstance<KSClassDeclaration>()
      .onEach {
        checkIsPublic(it)
        checkImplementsScoped(it)
        checkSuperType(it)
        checkSingleConstructorOrInject(it)
      }
      .forEach { generateGraph(it) }

    resolver
      .getSymbolsWithAnnotation(ContributesBinding::class)
      .filterIsInstance<KSClassDeclaration>()
      .forEach { checkDoesNotImplementScoped(it) }

    return emptyList()
  }

  private fun generateGraph(clazz: KSClassDeclaration) {
    val packageName = "${METRO_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val graphClassName = ClassName(packageName, "${clazz.innerClassNames()}Graph")
    val scopeClassName = clazz.scope().type.toClassName()

    val fileSpec =
      FileSpec.builder(graphClassName)
        .addType(
          TypeSpec.interfaceBuilder(graphClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addMetroOriginAnnotation(clazz)
            .addAnnotation(
              AnnotationSpec.builder(ContributesTo::class)
                .addMember("%T::class", scopeClassName)
                .build()
            )
            .apply {
              if (!clazz.hasInjectAnnotation()) {
                addFunction(
                  FunSpec.builder("provide${clazz.innerClassNames()}")
                    .addAnnotation(Provides::class)
                    .addAnnotations(
                      clazz.annotations
                        .filter { it.isMetroScopeAnnotation() }
                        .map { it.toAnnotationSpec() }
                        .toList()
                    )
                    .returns(clazz.toClassName())
                    .addParameters(clazz.constructorParameters().map { it.toParameterSpec() })
                    .addCode(clazz.constructorCall())
                    .build()
                )
              }
            }
            .addProperties(
              clazz.superTypes
                .filter { it.resolve().declaration.requireQualifiedName() != scopedFqName }
                .map {
                  val type = it.resolve()
                  PropertySpec.builder(
                      "bind${type.declaration.innerClassNames()}",
                      type.toClassName(),
                    )
                    .addAnnotation(Binds::class)
                    .receiver(clazz.toClassName())
                    .build()
                }
                .toList()
            )
            .addProperty(
              PropertySpec.builder("bind${clazz.innerClassNames()}Scoped", scopedClassName)
                .addAnnotation(Binds::class)
                .addAnnotation(IntoSet::class)
                .addAnnotation(
                  AnnotationSpec.builder(ForScope::class)
                    .addMember("%T::class", scopeClassName)
                    .build()
                )
                .receiver(clazz.toClassName())
                .build()
            )
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }

  private fun checkImplementsScoped(clazz: KSClassDeclaration) {
    val extendsScoped =
      clazz.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() == scopedFqName }

    check(extendsScoped, clazz) {
      "In order to use @ContributesScoped, ${clazz.simpleName.asString()} must " +
        "implement $scopedFqName."
    }
  }

  private fun checkSuperType(clazz: KSClassDeclaration) {
    val superTypeCount =
      clazz.superTypes
        .filter { it.resolve().declaration.requireQualifiedName() != scopedFqName }
        .count()

    check(superTypeCount < 2, clazz) {
      "In order to use @ContributesScoped, ${clazz.simpleName.asString()} is allowed to have only one " +
        "other super type besides Scoped."
    }
  }

  private fun checkSingleConstructorOrInject(clazz: KSClassDeclaration) {
    if (!clazz.hasInjectAnnotation() && clazz.constructors().count() > 1) {
      check(false, clazz) {
        "${clazz.simpleName.asString()} has multiple constructors. Annotate the constructor " +
          "to use with @Inject, or remove the extra constructors so @ContributesScoped can " +
          "generate a provider."
      }
    }
  }

  private fun checkDoesNotImplementScoped(clazz: KSClassDeclaration) {
    check(
      clazz.superTypes.none { it.resolve().declaration.requireQualifiedName() == scopedFqName },
      clazz,
    ) {
      "${clazz.simpleName.asString()} implements Scoped, but uses @ContributesBinding instead " +
        "of @ContributesScoped. When implementing Scoped the annotation @ContributesScoped " +
        "must be used instead of @ContributesBinding to bind both super types correctly. It's " +
        "not necessary to use @ContributesBinding."
    }
  }

  private fun KSClassDeclaration.constructorParameters(): List<KSValueParameter> {
    return providerConstructor()?.parameters.orEmpty()
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
}
