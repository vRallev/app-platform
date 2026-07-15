package software.ralf.app.platform.metro.compiler.renderer

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.fir.allSessions
import software.ralf.app.platform.metro.compiler.fir.findAnnotation
import software.ralf.app.platform.metro.compiler.fir.findClassLikeSymbolInContainingFile
import software.ralf.app.platform.metro.compiler.fir.findClassLikeSymbolInPackageFiles
import software.ralf.app.platform.metro.compiler.fir.findContainingFile
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation
import software.ralf.app.platform.metro.compiler.fir.resolveClassIdArgument
import software.ralf.app.platform.metro.compiler.fir.resolveClassReferenceArgument
import software.ralf.app.platform.metro.compiler.fir.resolveDeclaredSuperTypes
import software.ralf.app.platform.metro.compiler.fir.unwrapArgumentExpression

internal data class ResolvedModelClass(
  val classId: ClassId,
  val classSymbol: FirRegularClassSymbol?,
)

internal data class RendererContributionMetadata(
  val hasInjectAnnotation: Boolean,
  val modelClasses: List<ResolvedModelClass>,
)

internal data class RendererConstructor(
  val owner: FirRegularClassSymbol,
  val symbol: FirConstructorSymbol,
  val parameters: List<FirValueParameter>,
)

internal sealed interface RendererModelTypeResolution {
  data class Success(val modelClass: ResolvedModelClass) : RendererModelTypeResolution

  data class Error(val message: String) : RendererModelTypeResolution
}

internal fun rendererContributionMetadata(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): RendererContributionMetadata? {
  val modelType =
    resolveRendererModelType(classSymbol, session) as? RendererModelTypeResolution.Success
      ?: return null
  val includeSealedSubtypes = contributesRendererIncludeSealedSubtypes(classSymbol, session)

  return RendererContributionMetadata(
    hasInjectAnnotation = hasRendererInjectAnnotation(classSymbol, session),
    modelClasses =
      if (includeSealedSubtypes) {
        collectModelClasses(modelType.modelClass, session)
      } else {
        listOf(modelType.modelClass)
      },
  )
}

internal fun resolveRendererModelType(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): RendererModelTypeResolution {
  explicitRendererModelType(classSymbol, session)?.let {
    return RendererModelTypeResolution.Success(it)
  }

  val implicitModelTypes = implicitRendererModelTypes(classSymbol, session)
  return if (implicitModelTypes.size == 1) {
    RendererModelTypeResolution.Success(implicitModelTypes.single())
  } else {
    RendererModelTypeResolution.Error(
      buildString {
        append(
          "Couldn't find BaseModel type for ${classSymbol.name.asString()}. Consider adding " +
            "an explicit parameter."
        )
        if (implicitModelTypes.size > 1) {
          append("Found: ")
          append(implicitModelTypes.joinToString { it.classId.asSingleFqName().asString() })
        }
      }
    )
  }
}

internal fun isSingleInRendererScope(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Boolean {
  val annotation =
    findAnnotation(classSymbol, ClassIds.SINGLE_IN, session) as? FirAnnotationCall ?: return false
  val rawScopeArgument =
    annotation.argumentMapping.mapping[Name.identifier("scope")]
      ?: annotation.argumentList.arguments.firstOrNull()
      ?: return false
  return resolveClassIdArgument(rawScopeArgument, classSymbol, session) == ClassIds.RENDERER_SCOPE
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun rendererConstructors(classSymbol: FirRegularClassSymbol): List<FirConstructorSymbol> {
  return classSymbol.declarationSymbols.filterIsInstance<FirConstructorSymbol>()
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun rendererConstructor(classSymbol: FirRegularClassSymbol): RendererConstructor? {
  val constructorSymbol =
    rendererConstructors(classSymbol).firstOrNull { it.isPrimary }
      ?: rendererConstructors(classSymbol).firstOrNull()
  return constructorSymbol?.let { RendererConstructor(classSymbol, it, it.fir.valueParameters) }
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun hasRendererInjectAnnotation(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Boolean {
  return hasAnnotation(classSymbol, ClassIds.INJECT, session) ||
    rendererConstructors(classSymbol).any { constructor ->
      constructor.fir.annotations.any { it.toAnnotationClassIdSafe(session) == ClassIds.INJECT }
    }
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun injectedRendererConstructorParameterCount(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Int {
  val constructorSymbol =
    rendererConstructors(classSymbol).firstOrNull { constructor ->
      constructor.fir.annotations.any { it.toAnnotationClassIdSafe(session) == ClassIds.INJECT }
    }
  return constructorSymbol?.fir?.valueParameters?.size
    ?: rendererConstructor(classSymbol)?.parameters?.size
    ?: 0
}

private fun explicitRendererModelType(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): ResolvedModelClass? {
  val annotation =
    findAnnotation(classSymbol, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
      as? FirAnnotationCall ?: return null
  val rawModelTypeArgument =
    annotation.argumentMapping.mapping[Name.identifier("modelType")]
      ?: annotation.argumentList.arguments.firstOrNull()
      ?: return null

  return resolveClassReferenceArgument(rawModelTypeArgument, classSymbol, session)
    ?.takeIf { it.classId != ClassIds.UNIT }
    ?.let {
      val resolvedClassSymbol =
        it.classSymbol
          ?: (session.symbolProvider.getClassLikeSymbolByClassId(it.classId)
            as? FirRegularClassSymbol)
          ?: findClassLikeSymbolInContainingFile(classSymbol, it.classId, session)
          ?: findClassLikeSymbolInPackageFiles(
            classSymbol.classId.packageFqName,
            it.classId,
            session,
          )
      ResolvedModelClass(classId = it.classId, classSymbol = resolvedClassSymbol)
    }
}

private fun contributesRendererIncludeSealedSubtypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Boolean {
  val annotation =
    findAnnotation(classSymbol, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
      as? FirAnnotationCall ?: return true
  val rawArgument =
    annotation.argumentMapping.mapping[Name.identifier("includeSealedSubtypes")]
      ?: annotation.argumentList.arguments.firstOrNull { argument ->
        (argument as? FirNamedArgumentExpression)?.name == Name.identifier("includeSealedSubtypes")
      }
  val expression = rawArgument?.let(::unwrapArgumentExpression) ?: return true
  return (expression as? FirLiteralExpression)?.value as? Boolean ?: true
}

private fun implicitRendererModelTypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<ResolvedModelClass> {
  val collected = linkedMapOf<ClassId, ResolvedModelClass>()
  val visited = mutableSetOf<ConeKotlinType>()
  val queue = ArrayDeque<ConeKotlinType>()

  queue += resolveDeclaredSuperTypes(classSymbol, session)

  while (queue.isNotEmpty()) {
    val type = queue.removeFirst().fullyExpandedType(session)
    if (!visited.add(type)) continue

    collectImplicitModelTypes(type, session).forEach { modelClass ->
      collected.putIfAbsent(modelClass.classId, modelClass)
    }

    val typeSymbol = type.toRegularClassSymbol(session) ?: continue
    queue += resolveDeclaredSuperTypes(typeSymbol, session, actualType = type)
  }

  return collected.values.toList()
}

private fun collectImplicitModelTypes(
  type: ConeKotlinType,
  session: FirSession,
): List<ResolvedModelClass> {
  return type.typeArguments
    .asSequence()
    .mapNotNull { projection ->
      val candidateType =
        (projection as? ConeKotlinTypeProjection)?.type?.fullyExpandedType(session)
          ?: return@mapNotNull null
      val candidateSymbol = candidateType.toRegularClassSymbol(session)
      if (candidateSymbol == null || candidateSymbol.classId == ClassIds.BASE_MODEL) {
        return@mapNotNull null
      }
      if (!isBaseModelSubtype(candidateType, session)) return@mapNotNull null
      ResolvedModelClass(candidateSymbol.classId, candidateSymbol)
    }
    .toList()
}

private fun isBaseModelSubtype(
  type: ConeKotlinType,
  session: FirSession,
  visited: MutableSet<ConeKotlinType> = mutableSetOf(),
): Boolean {
  val expandedType = type.fullyExpandedType(session)
  if (!visited.add(expandedType)) return false

  val classSymbol = expandedType.toRegularClassSymbol(session) ?: return false
  if (classSymbol.classId == ClassIds.BASE_MODEL) return true

  return resolveDeclaredSuperTypes(classSymbol, session, actualType = expandedType).any {
    isBaseModelSubtype(it, session, visited)
  }
}

@OptIn(SymbolInternals::class)
private fun collectModelClasses(
  rootModelClass: ResolvedModelClass,
  session: FirSession,
): List<ResolvedModelClass> {
  val collected = linkedMapOf<ClassId, ResolvedModelClass>()
  val queue = ArrayDeque<ResolvedModelClass>()
  queue += rootModelClass

  while (queue.isNotEmpty()) {
    val modelClass = queue.removeFirst()
    if (collected.putIfAbsent(modelClass.classId, modelClass) != null) continue

    val classSymbol =
      modelClass.classSymbol
        ?: (session.symbolProvider.getClassLikeSymbolByClassId(modelClass.classId)
          as? FirRegularClassSymbol)
        ?: continue
    val sealedInheritors = findDirectSealedInheritors(classSymbol, session)
    if (sealedInheritors.isEmpty()) continue

    for ((classId, symbol) in sealedInheritors) {
      queue += ResolvedModelClass(classId = classId, classSymbol = symbol)
    }
  }

  return collected.values.toList()
}

@OptIn(SymbolInternals::class)
private fun findDirectSealedInheritors(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<Pair<ClassId, FirRegularClassSymbol?>> {
  if (!classSymbol.fir.isSealed) return emptyList()

  val collected = linkedMapOf<ClassId, FirRegularClassSymbol?>()

  findDirectSealedInheritorsFromMetadata(classSymbol, session).forEach { (classId, symbol) ->
    collected.putIfAbsent(classId, symbol)
  }
  findDirectSealedInheritorsInSource(classSymbol, session).forEach { symbol ->
    collected[symbol.classId] = collected[symbol.classId] ?: symbol
  }

  return collected.map { (classId, symbol) -> classId to symbol }
}

@OptIn(SymbolInternals::class)
private fun findDirectSealedInheritorsFromMetadata(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<Pair<ClassId, FirRegularClassSymbol?>> {
  val ownerSession = classSymbol.fir.moduleData.session
  return classSymbol.fir.getSealedClassInheritors(ownerSession).distinct().map { classId ->
    classId to
      ((ownerSession.symbolProvider.getClassLikeSymbolByClassId(classId)
        ?: session.symbolProvider.getClassLikeSymbolByClassId(classId))
        as? FirRegularClassSymbol)
  }
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
private fun findDirectSealedInheritorsInSource(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<FirRegularClassSymbol> {
  val visitedFiles = linkedSetOf<FirFile>()
  val candidates = linkedMapOf<ClassId, FirRegularClassSymbol>()

  fun visitFile(file: FirFile) {
    if (!visitedFiles.add(file)) return

    collectRegularClasses(file.declarations).forEach { candidate ->
      if (candidate.classId == classSymbol.classId) return@forEach
      val hasDirectSupertype =
        resolveDeclaredSuperTypes(candidate, session).any { superType ->
          superType.toRegularClassSymbol(session)?.classId == classSymbol.classId
        }
      if (hasDirectSupertype) {
        candidates.putIfAbsent(candidate.classId, candidate)
      }
    }
  }

  findContainingFile(classSymbol, session)?.let(::visitFile)
  allSessions(session).forEach { candidateSession ->
    candidateSession.firProvider
      .getFirFilesByPackage(classSymbol.classId.packageFqName)
      .forEach(::visitFile)
  }

  return candidates.values.toList()
}

@OptIn(DirectDeclarationsAccess::class)
private fun collectRegularClasses(
  declarations: List<FirDeclaration>
): Sequence<FirRegularClassSymbol> {
  return declarations.asSequence().flatMap { declaration ->
    val regularClass = declaration as? FirRegularClass ?: return@flatMap emptySequence()
    sequenceOf(regularClass.symbol) + collectRegularClasses(regularClass.declarations)
  }
}
