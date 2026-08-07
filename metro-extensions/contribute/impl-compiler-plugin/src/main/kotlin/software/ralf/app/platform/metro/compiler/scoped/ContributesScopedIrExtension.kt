package software.ralf.app.platform.metro.compiler.scoped

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

/** Fills in FIR-generated nested `@ContributesScoped` provider functions. */
@Suppress("DEPRECATION")
internal class ContributesScopedIrExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.transformChildrenVoid(ContributesScopedIrTransformer(pluginContext))
  }
}

@Suppress("DEPRECATION")
@OptIn(UnsafeDuringIrConstructionAPI::class)
private class ContributesScopedIrTransformer(private val pluginContext: IrPluginContext) :
  IrElementTransformerVoid() {

  override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
    val origin = declaration.origin
    if (
      origin !is IrDeclarationOrigin.GeneratedByPlugin ||
        origin.pluginKey != Keys.ContributesScopedGeneratorKey
    ) {
      return super.visitSimpleFunction(declaration)
    }
    if (declaration.body != null) return super.visitSimpleFunction(declaration)
    if (!declaration.name.asString().startsWith("provide")) {
      return super.visitSimpleFunction(declaration)
    }

    generateProvideScopedBody(declaration)

    return super.visitSimpleFunction(declaration)
  }

  private fun generateProvideScopedBody(declaration: IrSimpleFunction) {
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
