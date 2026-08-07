package software.ralf.app.platform.metro.compiler.renderer

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys

/**
 * Fills in the bodies for FIR-generated nested `@ContributesRenderer` graph functions.
 *
 * Pseudo Kotlin for `TestRenderer.RendererContribution`:
 * ```kotlin
 * @ContributesRenderer
 * class TestRenderer : Renderer<Model> {
 *
 *   @ContributesTo(RendererScope::class)
 *   @Origin(TestRenderer::class)
 *   interface RendererContribution {
 *     @Binds
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     fun provideTestRendererModel(renderer: TestRenderer): Renderer<*>
 *
 *     companion object {
 *       @Provides
 *       fun provideTestRenderer(): TestRenderer = TestRenderer()
 *
 *       @Provides
 *       @IntoMap
 *       @RendererKey(Model::class)
 *       @ForScope(RendererScope::class)
 *       fun provideTestRendererModelKey(): KClass<out Renderer<*>> = TestRenderer::class
 *     }
 *   }
 * }
 * ```
 *
 * The direct constructor call forwards the generated provider parameters to the renderer
 * constructor. The `@IntoMap` renderer binding stays abstract and is handled by Metro's normal
 * `@Binds` support.
 */
@Suppress("DEPRECATION")
internal class ContributesRendererIrExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.transformChildrenVoid(ContributesRendererIrTransformer(pluginContext))
  }
}

@Suppress("DEPRECATION")
@OptIn(UnsafeDuringIrConstructionAPI::class)
private class ContributesRendererIrTransformer(private val pluginContext: IrPluginContext) :
  IrElementTransformerVoid() {

  override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
    val origin = declaration.origin
    if (
      origin !is IrDeclarationOrigin.GeneratedByPlugin ||
        origin.pluginKey != Keys.ContributesRendererGeneratorKey
    ) {
      return super.visitSimpleFunction(declaration)
    }
    if (declaration.body != null) return super.visitSimpleFunction(declaration)
    if (!declaration.name.asString().startsWith("provide")) {
      return super.visitSimpleFunction(declaration)
    }

    when {
      declaration.name.asString().endsWith("Key") -> generateProvideRendererKeyBody(declaration)
      declaration.parameters.none { it.name.asString() == "renderer" } ->
        generateProvideRendererBody(declaration)
    }

    return super.visitSimpleFunction(declaration)
  }

  private fun generateProvideRendererBody(declaration: IrSimpleFunction) {
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

  private fun generateProvideRendererKeyBody(declaration: IrSimpleFunction) {
    val ownerClassSymbol = generatedOwnerClass(declaration)?.symbol ?: return
    val irBuilder = irBuilderFor(declaration)

    declaration.body = irBuilder.irBlockBody {
      +irReturn(
        IrClassReferenceImpl(
          UNDEFINED_OFFSET,
          UNDEFINED_OFFSET,
          pluginContext.irBuiltIns.kClassClass.typeWith(ownerClassSymbol.defaultType),
          ownerClassSymbol,
          ownerClassSymbol.defaultType,
        )
      )
    }
  }

  private fun generatedOwnerClass(declaration: IrSimpleFunction): IrClass? {
    val parentClass = declaration.parent as? IrClass ?: return null
    val contributionClass =
      if (parentClass.isCompanion) {
        parentClass.parentAsClass
      } else {
        parentClass
      }
    val originAnnotation =
      contributionClass.annotations.firstOrNull { annotation ->
        annotation.symbol.owner.parentAsClass.name == ClassIds.ORIGIN.shortClassName
      } ?: return null
    val classReference = originAnnotation.arguments[0] as? IrClassReference ?: return null
    return classReference.classType.classOrNull?.owner
  }

  private fun irBuilderFor(declaration: IrSimpleFunction) =
    DeclarationIrBuilder(pluginContext, declaration.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
}
