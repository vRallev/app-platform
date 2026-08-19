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
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.ksp.decapitalize
import software.ralf.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.ralf.app.platform.metro.MetroContextAware
import software.ralf.app.platform.metro.addMetroOriginAnnotation
import software.ralf.app.platform.renderer.metro.RobotKey

/**
 * Generates the necessary code in order to support [ContributesRobot].
 *
 * If you use `@ContributesRobot(AbcScope::class)`, then this processor will generate a graph
 * interface, which gets contributed to this scope.
 *
 * ```
 * package app.platform.inject.metro.software.ralf.test
 *
 * @ContributesTo(scope = AbcScope::class)
 * public interface AbcRobotGraph {
 *     @Provide
 *     fun provideAbcRobot(): AbcRobot = AbcRobot()
 *
 *     @Provides
 *     @IntoMap
 *     @RobotKey(AbcRobot::class)
 *     fun provideAbcRobotIntoMap(
 *         robot: () -> AbcRobot,
 *     ): Robot = robot()
 * }
 * ```
 */
@OptIn(KspExperimental::class)
internal class ContributesRobotProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, MetroContextAware {

  private val robotClassName = ClassName("software.ralf.app.platform.robot", "Robot")
  private val robotFqName = robotClassName.canonicalName
  private val robotGraphClassName = ClassName("software.ralf.app.platform.robot", "RobotGraph")

  private val robotKey = RobotKey::class.asClassName()

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesRobot::class)
      .filterIsInstance<KSClassDeclaration>()
      .onEach {
        checkIsPublic(it)
        checkNotSingleton(it)
        checkSuperType(it)
        checkSingleConstructorOrInject(it)
      }
      .forEach { generateGraph(it) }

    return emptyList()
  }

  private fun generateGraph(clazz: KSClassDeclaration) {
    val packageName = "${METRO_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val graphClassName = ClassName(packageName, "${clazz.innerClassNames()}Graph")

    val fileSpec =
      FileSpec.builder(graphClassName)
        .addType(
          TypeSpec.interfaceBuilder(graphClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addSuperinterface(robotGraphClassName)
            .addMetroOriginAnnotation(clazz)
            .addAnnotation(
              AnnotationSpec.builder(ContributesTo::class)
                .addMember("%T::class", clazz.scope().type.toClassName())
                .build()
            )
            .apply {
              if (!clazz.hasInjectAnnotation()) {
                addFunction(
                  FunSpec.builder("provide${clazz.innerClassNames()}")
                    .addAnnotation(Provides::class)
                    .returns(clazz.toClassName())
                    .addParameters(clazz.constructorParameters().map { it.toParameterSpec() })
                    .addCode(clazz.constructorCall())
                    .build()
                )
              }
            }
            .addFunction(
              FunSpec.builder("provide${clazz.innerClassNames()}IntoMap")
                .addAnnotation(Provides::class)
                .addAnnotation(IntoMap::class)
                .addAnnotation(
                  AnnotationSpec.builder(robotKey)
                    .addMember("%T::class", clazz.toClassName())
                    .build()
                )
                .addParameter(
                  name = "robot",
                  type = LambdaTypeName.get(returnType = clazz.toClassName()),
                )
                .returns(robotClassName)
                .addStatement("return robot()")
                .build()
            )
            .addProperty(name = clazz.innerClassNames().decapitalize(), type = clazz.toClassName())
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }

  private fun checkNotSingleton(clazz: KSClassDeclaration) {
    check(clazz.annotations.none { it.isMetroScopeAnnotation() }, clazz) {
      "It's not allowed allowed for a robot to be a singleton, because the lifetime " +
        "of the robot is scoped to the robot() factory function. Remove the @" +
        clazz.annotations.first { it.isMetroScopeAnnotation() }.shortName.asString() +
        " annotation."
    }
  }

  private fun checkSuperType(clazz: KSClassDeclaration) {
    val extendsRobot =
      clazz.getAllSuperTypes().any { it.declaration.requireQualifiedName() == robotFqName }

    check(extendsRobot, clazz) {
      "In order to use @ContributesRobot, ${clazz.simpleName.asString()} must " +
        "implement $robotFqName."
    }
  }

  private fun checkSingleConstructorOrInject(clazz: KSClassDeclaration) {
    if (!clazz.hasInjectAnnotation() && clazz.constructors().count() > 1) {
      check(false, clazz) {
        "${clazz.simpleName.asString()} has multiple constructors. Annotate the constructor " +
          "to use with @Inject, or remove the extra constructors so @ContributesRobot can " +
          "generate a provider."
      }
    }
  }

  private fun KSClassDeclaration.constructorParameters(): List<KSValueParameter> {
    return primaryConstructor?.parameters.orEmpty()
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
