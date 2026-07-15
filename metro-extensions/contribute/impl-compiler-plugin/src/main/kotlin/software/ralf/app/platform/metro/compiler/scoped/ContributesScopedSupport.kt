package software.ralf.app.platform.metro.compiler.scoped

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation
import software.ralf.app.platform.metro.compiler.fir.resolveDeclaredSuperTypes

internal data class ResolvedScopedSuperType(val classId: ClassId, val coneType: ConeKotlinType)

internal data class ScopedContributionMetadata(val otherSuperType: ResolvedScopedSuperType?)

internal data class ScopedConstructor(
  val owner: FirRegularClassSymbol,
  val symbol: FirConstructorSymbol,
  val parameters: List<FirValueParameter>,
)

internal fun contributesScopedMetadata(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): ScopedContributionMetadata? {
  if (!implementsScoped(classSymbol, session)) return null

  val otherSuperTypes = directOtherSupertypes(classSymbol, session)
  if (otherSuperTypes.size > 1) return null

  return ScopedContributionMetadata(otherSuperType = otherSuperTypes.singleOrNull())
}

internal fun directOtherSupertypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<ResolvedScopedSuperType> {
  return resolveDeclaredSuperTypes(classSymbol, session).mapNotNull { superType ->
    val classId = superType.classId ?: return@mapNotNull null
    if (classId == ClassIds.SCOPED || classId == StandardClassIds.Any) return@mapNotNull null
    ResolvedScopedSuperType(classId = classId, coneType = superType)
  }
}

internal fun implementsScoped(classSymbol: FirRegularClassSymbol, session: FirSession): Boolean {
  return isScopedType(classSymbol.defaultType(), session)
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun scopedConstructors(classSymbol: FirRegularClassSymbol): List<FirConstructorSymbol> {
  return classSymbol.declarationSymbols.filterIsInstance<FirConstructorSymbol>()
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun scopedConstructor(classSymbol: FirRegularClassSymbol): ScopedConstructor? {
  val constructorSymbol =
    scopedConstructors(classSymbol).firstOrNull { it.isPrimary }
      ?: scopedConstructors(classSymbol).firstOrNull()
  return constructorSymbol?.let { ScopedConstructor(classSymbol, it, it.fir.valueParameters) }
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun hasScopedInjectAnnotation(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Boolean {
  return hasAnnotation(classSymbol, ClassIds.INJECT, session) ||
    scopedConstructors(classSymbol).any { constructor ->
      constructor.fir.annotations.any { it.toAnnotationClassIdSafe(session) == ClassIds.INJECT }
    }
}

private fun isScopedType(
  type: ConeKotlinType,
  session: FirSession,
  visited: MutableSet<ConeKotlinType> = mutableSetOf(),
): Boolean {
  val expandedType = type.fullyExpandedType(session)
  if (!visited.add(expandedType)) return false
  if (expandedType.classId == ClassIds.SCOPED) return true

  val classSymbol = expandedType.toRegularClassSymbol(session) ?: return false
  return resolveDeclaredSuperTypes(classSymbol, session, actualType = expandedType).any {
    isScopedType(it, session, visited)
  }
}
