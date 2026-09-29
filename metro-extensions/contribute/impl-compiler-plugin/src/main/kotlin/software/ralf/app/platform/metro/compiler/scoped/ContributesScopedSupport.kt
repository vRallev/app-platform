package software.ralf.app.platform.metro.compiler.scoped

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.fir.extractScopeClassId
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation
import software.ralf.app.platform.metro.compiler.fir.resolveDeclaredSuperTypes
import software.ralf.app.platform.metro.compiler.fir.resolveTypeRef
import software.ralf.app.platform.metro.compiler.fir.unwrapArgumentExpression

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

internal fun hasExplicitScopedSetBinding(
  annotations: List<FirAnnotation>,
  contributesBindingAnnotation: FirAnnotation,
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Boolean {
  val contributesBindingScope =
    extractScopeClassId(contributesBindingAnnotation, classSymbol, session) ?: return false

  return annotations.any { annotation ->
    if (annotation.toAnnotationClassIdSafe(session) != ClassIds.CONTRIBUTES_INTO_SET) {
      return@any false
    }
    if (extractScopeClassId(annotation, classSymbol, session) != contributesBindingScope) {
      return@any false
    }

    val bindingTypeRef = annotation.explicitBindingTypeRef() ?: return@any false
    val bindingType = resolveTypeRef(bindingTypeRef, classSymbol, session) ?: return@any false
    if (bindingType.classId != ClassIds.SCOPED || bindingType.isMarkedNullable) {
      return@any false
    }

    val forScopeAnnotation =
      bindingTypeRef.annotations.firstOrNull {
        it.toAnnotationClassIdSafe(session) == ClassIds.FOR_SCOPE
      } ?: return@any false
    extractScopeClassId(forScopeAnnotation, classSymbol, session) == contributesBindingScope
  }
}

private fun FirAnnotation.explicitBindingTypeRef(): FirTypeRef? {
  val annotationCall = this as? FirAnnotationCall ?: return null
  val bindingExpression =
    annotationCall.argumentMapping.mapping[Name.identifier("binding")] ?: return null
  val bindingCall = unwrapArgumentExpression(bindingExpression) as? FirFunctionCall ?: return null
  return (bindingCall.typeArguments.singleOrNull() as? FirTypeProjectionWithVariance)?.typeRef
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
