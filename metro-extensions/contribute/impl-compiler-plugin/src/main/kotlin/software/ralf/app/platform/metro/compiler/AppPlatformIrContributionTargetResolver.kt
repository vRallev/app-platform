package software.ralf.app.platform.metro.compiler

import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class AppPlatformIrContributionTargetResolver(
  private val compatContext: CompatContext,
) {
  data class RendererTarget(
    val hasInjectAnnotation: Boolean,
    val modelClasses: List<IrClassSymbol>,
  )

  data class RobotTarget(
    val scope: ClassId,
    val hasInjectAnnotation: Boolean,
  )

  data class ScopedTarget(
    val scope: ClassId,
    val hasInjectAnnotation: Boolean,
    val otherSuperType: IrType?,
    val scopeAnnotations: List<IrConstructorCall>,
  )

  fun resolveRenderer(sourceClass: IrClass): RendererTarget? {
    val annotation = sourceClass.findAnnotation(ClassIds.CONTRIBUTES_RENDERER) ?: return null
    val explicitModelClass =
      annotation.classArgument(0)?.takeUnless { it.owner.classId == ClassIds.UNIT }
    val modelClass = explicitModelClass ?: sourceClass.implicitRendererModelClasses().singleOrNull()
      ?: return null
    val includeSealedSubtypes = (annotation.arguments.getOrNull(1) as? IrConst)?.value != false
    return RendererTarget(
      hasInjectAnnotation = sourceClass.hasInjectAnnotation(),
      modelClasses =
        if (includeSealedSubtypes) {
          collectSealedModelClasses(modelClass)
        } else {
          listOf(modelClass)
        },
    )
  }

  fun resolveRobot(sourceClass: IrClass): RobotTarget? {
    val annotation = sourceClass.findAnnotation(ClassIds.CONTRIBUTES_ROBOT) ?: return null
    val scope = annotation.classArgument(0)?.owner?.classId ?: return null
    return RobotTarget(scope = scope, hasInjectAnnotation = sourceClass.hasInjectAnnotation())
  }

  fun resolveScoped(sourceClass: IrClass): ScopedTarget? {
    val annotation = sourceClass.findAnnotation(ClassIds.CONTRIBUTES_SCOPED) ?: return null
    val scope = annotation.classArgument(0)?.owner?.classId ?: return null
    if (!sourceClass.isSubtypeOf(ClassIds.SCOPED)) return null
    val otherSuperTypes =
      sourceClass.superTypes.filter { superType ->
        val classId = superType.classOrNull?.owner?.classId
        classId != ClassIds.SCOPED && classId != ClassIds.ANY
      }
    if (otherSuperTypes.size > 1) return null
    return ScopedTarget(
      scope = scope,
      hasInjectAnnotation = sourceClass.hasInjectAnnotation(),
      otherSuperType = otherSuperTypes.singleOrNull(),
      scopeAnnotations =
        sourceClass.annotations().filter { candidate ->
          candidate.symbol.owner.parentAsClass.hasAnnotation(ClassIds.SCOPE)
        },
    )
  }

  private fun IrClass.implicitRendererModelClasses(): List<IrClassSymbol> {
    val collected = linkedMapOf<ClassId, IrClassSymbol>()
    val visited = mutableSetOf<IrType>()
    val queue = ArrayDeque<IrType>()
    queue += superTypes

    while (queue.isNotEmpty()) {
      val type = queue.removeFirst()
      if (!visited.add(type)) continue

      type.arguments().forEach { argument ->
        val candidateClass = argument.type.classOrNull?.owner ?: return@forEach
        val candidateClassId = candidateClass.classId ?: return@forEach
        if (candidateClassId != ClassIds.BASE_MODEL && candidateClass.isSubtypeOf(ClassIds.BASE_MODEL)) {
          collected.putIfAbsent(candidateClassId, candidateClass.symbol)
        }
      }

      val superClass = type.classOrNull?.owner ?: continue
      queue += superClass.substitutedSuperTypes(type)
    }
    return collected.values.toList()
  }

  private fun IrType.arguments(): List<IrTypeProjection> {
    return (this as? IrSimpleType)?.arguments?.filterIsInstance<IrTypeProjection>().orEmpty()
  }

  private fun IrClass.substitutedSuperTypes(type: IrType): List<IrType> {
    val arguments = (type as? IrSimpleType)?.arguments.orEmpty()
    val substitutions =
      typeParameters.zip(arguments).mapNotNull { (parameter, argument) ->
        val projection = argument as? IrTypeProjection ?: return@mapNotNull null
        parameter.symbol to projection.type
      }
    if (substitutions.isEmpty()) return superTypes
    val substitutor = IrTypeSubstitutor(substitutions.toMap<IrTypeParameterSymbol, IrType>())
    return superTypes.map { superType -> substitutor.substitute(superType) }
  }

  private fun collectSealedModelClasses(rootClass: IrClassSymbol): List<IrClassSymbol> {
    val collected = linkedMapOf<ClassId, IrClassSymbol>()
    val queue = ArrayDeque<IrClassSymbol>()
    queue += rootClass
    while (queue.isNotEmpty()) {
      val modelClass = queue.removeFirst()
      val classId = modelClass.owner.classId ?: continue
      if (collected.putIfAbsent(classId, modelClass) != null) continue
      if (modelClass.owner.modality == Modality.SEALED) {
        queue += modelClass.owner.sealedSubclasses
      }
    }
    return collected.values.toList()
  }

  private fun IrClass.isSubtypeOf(
    target: ClassId,
    visited: MutableSet<ClassId> = mutableSetOf(),
  ): Boolean {
    val classId = classId ?: return false
    if (classId == target) return true
    if (!visited.add(classId)) return false
    return superTypes.any { superType ->
      superType.classOrNull?.owner?.isSubtypeOf(target, visited) == true
    }
  }

  private fun IrClass.hasInjectAnnotation(): Boolean {
    return hasAnnotation(ClassIds.INJECT) || constructors.any { it.hasAnnotation(ClassIds.INJECT) }
  }

  private fun IrAnnotationContainer.hasAnnotation(classId: ClassId): Boolean {
    return findAnnotation(classId) != null
  }

  private fun IrAnnotationContainer.findAnnotation(classId: ClassId): IrConstructorCall? {
    return annotations().firstOrNull { annotation ->
      annotation.symbol.owner.parentAsClass.classId == classId
    }
  }

  private fun IrAnnotationContainer.annotations(): List<IrConstructorCall> {
    return with(compatContext) { annotationsCompat() }
  }

  private fun IrConstructorCall.classArgument(index: Int): IrClassSymbol? {
    return (arguments.getOrNull(index) as? IrClassReference)?.symbol as? IrClassSymbol
  }
}
