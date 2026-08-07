package software.ralf.app.platform.metro.compiler.robot

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import software.ralf.app.platform.metro.compiler.Keys

/**
 * Fills in the bodies for FIR-generated nested `@ContributesRobot` provider functions.
 *
 * Pseudo Kotlin for `TestRobot.RobotContribution`:
 * ```kotlin
 * @ContributesRobot(AppScope::class)
 * class TestRobot : Robot {
 *
 *   @ContributesTo(AppScope::class)
 *   interface RobotContribution {
 *     @Provides
 *     fun provideTestRobot(dependency: RobotDependency): TestRobot = TestRobot(dependency)
 *
 *     @Binds
 *     @IntoMap
 *     @RobotKey(TestRobot::class)
 *     fun provideTestRobotIntoMap(robot: TestRobot): Robot
 *   }
 * }
 * ```
 *
 * The direct constructor call forwards the generated provider parameters to the robot constructor.
 * The `@IntoMap` binding stays abstract and is handled by Metro's normal `@Binds` support.
 */
@Suppress("DEPRECATION")
internal class ContributesRobotIrExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.transformChildrenVoid(ContributesRobotIrTransformer(pluginContext))
  }
}

@Suppress("DEPRECATION")
@OptIn(UnsafeDuringIrConstructionAPI::class)
private class ContributesRobotIrTransformer(private val pluginContext: IrPluginContext) :
  IrElementTransformerVoid() {

  override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
    val origin = declaration.origin
    if (
      origin !is IrDeclarationOrigin.GeneratedByPlugin ||
        origin.pluginKey != Keys.ContributesRobotGeneratorKey
    ) {
      return super.visitSimpleFunction(declaration)
    }
    if (declaration.body != null) return super.visitSimpleFunction(declaration)
    if (!declaration.name.asString().startsWith("provide"))
      return super.visitSimpleFunction(declaration)

    if (!declaration.name.asString().endsWith("IntoMap")) {
      generateProvideRobotBody(declaration)
    }

    return super.visitSimpleFunction(declaration)
  }

  private fun generateProvideRobotBody(declaration: IrSimpleFunction) {
    val classSymbol = (declaration.returnType as? IrSimpleType)?.classOrNull ?: return
    val constructor =
      classSymbol.owner.primaryConstructor?.symbol
        ?: classSymbol.constructors.firstOrNull()
        ?: return
    val constructorParameters =
      constructor.owner.parameters.filter { it.kind == IrParameterKind.Regular }
    val functionParameters = declaration.parameters.filter { it.kind == IrParameterKind.Regular }
    if (constructorParameters.size != functionParameters.size) return
    val irBuilder = irBuilderFor(declaration)

    declaration.body = irBuilder.irBlockBody {
      val constructorCall = irCallConstructor(constructor, emptyList())
      constructorCall.startOffset = UNDEFINED_OFFSET
      constructorCall.endOffset = UNDEFINED_OFFSET
      functionParameters.forEachIndexed { index, parameter ->
        constructorCall.arguments[index] = irGet(parameter)
      }
      +irReturn(constructorCall)
    }
  }

  private fun irBuilderFor(declaration: IrSimpleFunction) =
    DeclarationIrBuilder(pluginContext, declaration.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
}
