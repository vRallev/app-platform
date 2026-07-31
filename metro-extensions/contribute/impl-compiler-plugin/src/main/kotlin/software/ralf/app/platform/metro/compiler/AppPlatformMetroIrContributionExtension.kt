package software.ralf.app.platform.metro.compiler

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.MetroOptions
import dev.zacsweers.metro.compiler.api.ir.MetroIrContributionExtension
import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import software.ralf.app.platform.metro.compiler.renderer.ContributesRendererIds
import software.ralf.app.platform.metro.compiler.robot.ContributesRobotIds
import software.ralf.app.platform.metro.compiler.scoped.ContributesScopedIds

/** Supplies generated App Platform binding containers while Metro merges graphs in IR. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class AppPlatformMetroIrContributionExtension(
  private val generator: AppPlatformIrContributionGenerator,
  private val pluginContext: IrPluginContext,
  private val compatContext: CompatContext,
) : MetroIrContributionExtension {
  override fun contributeBindingContainers(
    scope: ClassId,
    callingDeclaration: IrDeclaration,
  ): List<IrClass> {
    val localContainers = generator.generate(callingDeclaration.file.module)[scope].orEmpty()
    val finder =
      with(compatContext) { pluginContext.finderForSourceCompat(callingDeclaration.file) }
    val hintFunctions = finder.findFunctions(scope.appPlatformHintCallableId())
    val externalContainers = hintFunctions.flatMap { hint ->
      val hintedClass =
        hint.owner.regularParameters().singleOrNull()?.type?.classOrNull?.owner
          ?: return@flatMap emptyList()
      if (hintedClass.hasAnnotation(ClassIds.BINDING_CONTAINER)) {
        return@flatMap listOf(hintedClass)
      }
      buildList {
          if (hintedClass.hasAnnotation(ClassIds.CONTRIBUTES_RENDERER)) {
            add(ContributesRendererIds.NESTED_INTERFACE_NAME)
          }
          if (hintedClass.hasAnnotation(ClassIds.CONTRIBUTES_ROBOT)) {
            add(ContributesRobotIds.NESTED_INTERFACE_NAME)
          }
          if (hintedClass.hasAnnotation(ClassIds.CONTRIBUTES_SCOPED)) {
            add(ContributesScopedIds.NESTED_INTERFACE_NAME)
          }
        }
        .mapNotNull { nestedName ->
          val sourceClassId = hintedClass.classId ?: return@mapNotNull null
          finder.findClass(sourceClassId.createNestedClassId(nestedName))?.owner
        }
    }
    return (localContainers + externalContainers).distinctBy { container ->
      container.classIdOrFail
    }
  }

  private fun IrSimpleFunction.regularParameters(): List<IrValueParameter> {
    return parameters.filter { it.kind == IrParameterKind.Regular }
  }

  private fun IrClass.hasAnnotation(classId: ClassId): Boolean {
    return with(compatContext) {
      annotationsCompat().any { annotation ->
        annotation.symbol.owner.parentAsClass.classId == classId
      }
    }
  }

  private fun ClassId.appPlatformHintCallableId(): CallableId {
    return CallableId(
      FqName("metro.hints"),
      Name.identifier(asFqNameString().replace('.', '_')),
    )
  }

  @AutoService(MetroIrContributionExtension.Factory::class)
  public class Factory : MetroIrContributionExtension.Factory {
    override fun create(
      pluginContext: IrPluginContext,
      compatContext: CompatContext,
      options: MetroOptions,
    ): MetroIrContributionExtension? {
      if (!options.generateClassesInIr) return null
      return AppPlatformMetroIrContributionExtension(
        generator =
          AppPlatformIrContributionGenerator(
            pluginContext = pluginContext,
            compatContext = compatContext,
            generateDeclarationsInIr = true,
          ),
        pluginContext = pluginContext,
        compatContext = compatContext,
      )
    }
  }
}
