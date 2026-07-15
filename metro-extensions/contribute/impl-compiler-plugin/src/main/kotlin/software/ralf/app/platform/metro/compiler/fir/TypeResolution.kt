package software.ralf.app.platform.metro.compiler.fir

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.TypeResolutionConfiguration
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeConflictingProjection
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

@OptIn(SymbolInternals::class)
internal fun resolveDeclaredSuperTypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
  actualType: ConeKotlinType? = null,
): List<ConeKotlinType> {
  val resolutionSession = classSymbol.fir.moduleData.session
  val substitution =
    actualType
      ?.let { actual ->
        classSymbol.typeParameterSymbols.zip(actual.typeArguments).mapNotNull { (parameter, arg) ->
          (arg as? ConeKotlinTypeProjection)?.type?.let { parameter to it }
        }
      }
      .orEmpty()
  val substitutor =
    substitution.takeIf { it.isNotEmpty() }?.let { substitutorByMap(it.toMap(), session) }

  return classSymbol.fir.superTypeRefs.mapNotNull { superTypeRef ->
    val resolvedType =
      resolveSuperTypeRef(superTypeRef, classSymbol, resolutionSession) ?: return@mapNotNull null
    val substitutedType = substitutor?.substituteOrNull(resolvedType) ?: resolvedType
    val expandedType = substitutedType.fullyExpandedType(session)
    expandedType.takeUnless { it.toRegularClassSymbol(session)?.classId == StandardClassIds.Any }
  }
}

internal fun findContainingFile(classSymbol: FirClassLikeSymbol<*>, session: FirSession): FirFile? {
  return allSessions(session).firstNotNullOfOrNull { candidate ->
    candidate.firProvider.getFirClassifierContainerFileIfAny(classSymbol)
  }
}

internal fun resolveTypeRef(
  typeRef: FirTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ConeKotlinType? {
  return resolveSuperTypeRef(typeRef, owner, session)
}

internal fun allSessions(session: FirSession): List<FirSession> {
  val visitedModules = linkedSetOf<FirModuleData>()
  val visited = linkedSetOf<FirSession>()

  fun visit(moduleData: FirModuleData) {
    visited.add(moduleData.session)
    if (!visitedModules.add(moduleData)) return

    moduleData.dependencies.forEach(::visit)
    moduleData.friendDependencies.forEach(::visit)
    moduleData.dependsOnDependencies.forEach(::visit)
  }

  visit(session.moduleData)
  return visited.toList()
}

private fun resolveSuperTypeRef(
  typeRef: FirTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ConeKotlinType? {
  return when (typeRef) {
    is FirResolvedTypeRef -> typeRef.coneType
    is FirUserTypeRef -> resolveUserType(typeRef, owner, session)
    else -> typeRef.coneTypeOrNull
  }?.fullyExpandedType(session)
}

private fun resolveUserType(
  typeRef: FirUserTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ConeKotlinType? {
  val manualType = resolveUserTypeManually(typeRef, owner, session)
  val file = findContainingFile(owner, session) ?: return typeRef.coneTypeOrNull ?: manualType
  val scopes = createImportingScopes(file, session, ScopeSession())
  val configuration = TypeResolutionConfiguration(scopes, emptyList(), useSiteFile = file)
  val resolvedType = runCatching {
    session.typeResolver
      .resolveType(
        typeRef = typeRef,
        configuration = configuration,
        areBareTypesAllowed = true,
        isOperandOfIsOperator = false,
        resolveDeprecations = false,
        supertypeSupplier = SupertypeSupplier.Default,
        expandTypeAliases = false,
      )
      .type
  }
    .getOrElse {
      return manualType ?: throw it
    }

  if (resolvedType.classId?.shortClassName?.asString() != "<error>") {
    return resolvedType
  }

  return manualType ?: resolvedType
}

private fun resolveUserTypeManually(
  typeRef: FirUserTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ConeKotlinType? {
  val fallbackClassId = resolveUserTypeClassId(typeRef, owner, session) ?: return null
  val typeArguments =
    typeRef.qualifier.lastOrNull()?.typeArgumentList?.typeArguments?.map { argument ->
      resolveTypeProjection(argument, owner, session)
    }
  return ConeClassLikeTypeImpl(
    ConeClassLikeLookupTagImpl(fallbackClassId),
    typeArguments.orEmpty().toTypedArray(),
    isMarkedNullable = typeRef.isMarkedNullable,
  )
}

private fun resolveTypeProjection(
  projection: FirTypeProjection,
  owner: FirRegularClassSymbol,
  session: FirSession,
) =
  when (projection) {
    is FirStarProjection -> ConeStarProjection
    is FirTypeProjectionWithVariance -> {
      val resolvedType =
        resolveSuperTypeRef(projection.typeRef, owner, session) ?: projection.typeRef.coneTypeOrNull
      when {
        resolvedType == null -> ConeStarProjection
        projection.variance == Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(resolvedType)
        projection.variance == Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(resolvedType)
        else -> ConeKotlinTypeConflictingProjection(resolvedType)
      }
    }
    else -> ConeStarProjection
  }

private fun resolveUserTypeClassId(
  typeRef: FirUserTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ClassId? {
  val qualifierNames = typeRef.qualifier.map { it.name }
  if (qualifierNames.isEmpty()) return null

  val importedClassId =
    findContainingFile(owner, session)?.let { file ->
      val scopes = createImportingScopes(file, session, ScopeSession())
      scopes.firstNotNullOfOrNull { scope ->
        val importedSymbol =
          scope.getSingleClassifier(qualifierNames.first()) as? FirClassLikeSymbol<*>
            ?: return@firstNotNullOfOrNull null
        qualifierNames.drop(1).fold(importedSymbol.classId) { classId, name ->
          classId.createNestedClassId(name)
        }
      }
    }
  if (importedClassId != null) return importedClassId

  return userTypeClassIdCandidates(owner, qualifierNames).firstOrNull { classId ->
    session.symbolProvider.getClassLikeSymbolByClassId(classId) != null ||
      findClassLikeSymbolInContainingFile(owner, classId, session) != null ||
      findClassLikeSymbolInPackageFiles(owner.classId.packageFqName, classId, session) != null
  }
}

private fun userTypeClassIdCandidates(
  owner: FirRegularClassSymbol,
  qualifierNames: List<Name>,
): Sequence<ClassId> {
  fun nestedClassId(base: ClassId, nestedNames: List<Name>): ClassId {
    return nestedNames.fold(base) { classId, name -> classId.createNestedClassId(name) }
  }

  val topLevelCandidate =
    nestedClassId(
      ClassId.topLevel(owner.classId.packageFqName.child(qualifierNames.first())),
      qualifierNames.drop(1),
    )

  return sequence {
    yield(topLevelCandidate)

    var outerClassId = owner.classId.outerClassId
    while (outerClassId != null) {
      yield(nestedClassId(outerClassId, qualifierNames))
      outerClassId = outerClassId.outerClassId
    }
  }
}
