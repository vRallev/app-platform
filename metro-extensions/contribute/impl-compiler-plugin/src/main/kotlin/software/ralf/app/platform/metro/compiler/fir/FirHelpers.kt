package software.ralf.app.platform.metro.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildGetClassCall
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(DirectDeclarationsAccess::class)
internal fun FirRegularClass.addGeneratedDeclaration(declaration: FirDeclaration) {
  @Suppress("UNCHECKED_CAST") val mutableDeclarations = declarations as MutableList<FirDeclaration>
  mutableDeclarations += declaration
}

internal fun hasAnnotation(
  classSymbol: FirClassSymbol<*>,
  annotationClassId: ClassId,
  session: FirSession,
): Boolean {
  return classSymbol.resolvedCompilerAnnotationsWithClassIds.any {
    it.toAnnotationClassIdSafe(session) == annotationClassId
  }
}

internal fun findAnnotation(
  classSymbol: FirClassSymbol<*>,
  annotationClassId: ClassId,
  session: FirSession,
): FirAnnotation? {
  return classSymbol.resolvedAnnotationsWithArguments.firstOrNull { annotation ->
    annotation.toAnnotationClassIdSafe(session) == annotationClassId
  }
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun buildAnnotationCallWithArgument(
  classId: ClassId,
  argName: Name,
  argument: FirExpression,
  containingSymbol: FirBasedSymbol<*>,
  session: FirSession,
): FirAnnotationCall {
  val annotationType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val annotationClassSymbol =
    session.symbolProvider.getClassLikeSymbolByClassId(classId)
      ?: error("Annotation class $classId not found on the classpath")
  val constructorSymbol =
    (annotationClassSymbol as? FirClassSymbol<*>)
      ?.declarationSymbols
      ?.filterIsInstance<FirConstructorSymbol>()
      ?.firstOrNull() ?: error("No constructor found for annotation class $classId")
  val argumentParameter = constructorSymbol.fir.valueParameters.first { it.name == argName }

  return org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall {
    annotationTypeRef = annotationType.toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping { mapping[argName] = argument }
    argumentList =
      buildResolvedArgumentList(
        original = null,
        mapping = linkedMapOf(argument to argumentParameter),
      )
    calleeReference = buildResolvedNamedReference {
      name = classId.shortClassName
      resolvedSymbol = constructorSymbol
    }
    containingDeclarationSymbol = containingSymbol
    annotationResolvePhase = FirAnnotationResolvePhase.Types
  }
}

@OptIn(DirectDeclarationsAccess::class)
internal fun buildSimpleAnnotationCall(
  classId: ClassId,
  containingSymbol: FirBasedSymbol<*>,
  session: FirSession,
): FirAnnotationCall {
  val annotationType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val annotationClassSymbol =
    session.symbolProvider.getClassLikeSymbolByClassId(classId)
      ?: error("Annotation class $classId not found on the classpath")
  val constructorSymbol =
    (annotationClassSymbol as? FirClassSymbol<*>)
      ?.declarationSymbols
      ?.filterIsInstance<FirConstructorSymbol>()
      ?.firstOrNull() ?: error("No constructor found for annotation class $classId")

  return org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall {
    annotationTypeRef = annotationType.toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping()
    calleeReference = buildResolvedNamedReference {
      name = classId.shortClassName
      resolvedSymbol = constructorSymbol
    }
    containingDeclarationSymbol = containingSymbol
    annotationResolvePhase = FirAnnotationResolvePhase.Types
  }
}

internal fun extractScopeArgument(
  classSymbol: FirClassSymbol<*>,
  annotationClassId: ClassId,
  session: FirSession,
): FirExpression? {
  val annotation = findAnnotation(classSymbol, annotationClassId, session) ?: return null
  val annotationCall = annotation as? FirAnnotationCall ?: return null
  val firstArgument = annotationCall.argumentList.arguments.firstOrNull() ?: return null
  return if (firstArgument is FirNamedArgumentExpression) {
    firstArgument.expression
  } else {
    firstArgument
  }
}

internal fun extractScopeClassId(
  classSymbol: FirRegularClassSymbol,
  annotationClassId: ClassId,
  session: FirSession,
): ClassId? {
  val annotation = findAnnotation(classSymbol, annotationClassId, session) ?: return null
  val annotationCall = annotation as? FirAnnotationCall ?: return null

  val rawScopeExpression =
    annotationCall.argumentMapping.mapping[Name.identifier("scope")]
      ?: annotationCall.argumentList.arguments.firstOrNull()
      ?: return null

  return resolveClassIdArgument(rawScopeExpression, classSymbol, session)
}

internal fun unwrapArgumentExpression(expression: FirExpression): FirExpression {
  return if (expression is FirNamedArgumentExpression) expression.expression else expression
}

internal data class ResolvedClassReference(
  val classId: ClassId,
  val classSymbol: FirRegularClassSymbol?,
)

internal fun resolveClassIdArgument(
  rawExpression: FirExpression,
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): ClassId? {
  return resolveClassReferenceArgument(rawExpression, classSymbol, session)?.classId
}

internal fun resolveClassReferenceArgument(
  rawExpression: FirExpression,
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): ResolvedClassReference? {
  val expression = unwrapArgumentExpression(rawExpression)
  val getClassCall = expression as? FirGetClassCall ?: return null
  val innerArgument = getClassCall.argumentList.arguments.firstOrNull() ?: return null

  return when (innerArgument) {
    is FirResolvedQualifier ->
      innerArgument.qualifierSymbol?.classId?.let { classId ->
        ResolvedClassReference(
          classId = classId,
          classSymbol =
            (innerArgument.qualifierSymbol as? FirRegularClassSymbol)
              ?: (session.symbolProvider.getClassLikeSymbolByClassId(classId)
                as? FirRegularClassSymbol)
              ?: findClassLikeSymbolInContainingFile(classSymbol, classId, session)
              ?: findClassLikeSymbolInPackageFiles(
                classSymbol.classId.packageFqName,
                classId,
                session,
              ),
        )
      }

    is FirPropertyAccessExpression -> {
      val reference = innerArgument.calleeReference
      if (
        reference is FirResolvedNamedReference && reference.resolvedSymbol is FirRegularClassSymbol
      ) {
        val resolvedSymbol = reference.resolvedSymbol as FirRegularClassSymbol
        ResolvedClassReference(resolvedSymbol.classId, resolvedSymbol)
      } else {
        val name = reference.name
        val file = session.firProvider.getFirClassifierContainerFileIfAny(classSymbol)
        val samePackageClassId =
          if (name.asString() == "Unit") {
            ClassId(FqName("kotlin"), name)
          } else {
            ClassId(classSymbol.classId.packageFqName, name)
          }
        val explicitImportClassIds =
          file
            ?.imports
            ?.filter { !it.isAllUnder }
            ?.mapNotNull { import ->
              val importedFqName = import.importedFqName ?: return@mapNotNull null
              ClassId.topLevel(importedFqName).takeIf { importedFqName.shortName() == name }
            }
            .orEmpty()
        val allUnderImportClassIds =
          file
            ?.imports
            ?.filter { it.isAllUnder }
            ?.mapNotNull { import ->
              val importedFqName = import.importedFqName ?: return@mapNotNull null
              ClassId(importedFqName, name)
            }
            .orEmpty()
        val classId =
          sequenceOf(samePackageClassId)
            .plus(explicitImportClassIds.asSequence())
            .plus(allUnderImportClassIds.asSequence())
            .firstOrNull { candidateClassId ->
              session.symbolProvider.getClassLikeSymbolByClassId(candidateClassId) != null ||
                findClassLikeSymbolInContainingFile(classSymbol, candidateClassId, session) !=
                  null ||
                findClassLikeSymbolInPackageFiles(
                  classSymbol.classId.packageFqName,
                  candidateClassId,
                  session,
                ) != null
            } ?: samePackageClassId
        ResolvedClassReference(
          classId,
          (session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol)
            ?: findClassLikeSymbolInContainingFile(classSymbol, classId, session)
            ?: findClassLikeSymbolInPackageFiles(
              classSymbol.classId.packageFqName,
              classId,
              session,
            ),
        )
      }
    }

    else -> null
  }
}

internal fun buildClassExpression(
  classSymbol: FirClassSymbol<*>,
  session: FirSession,
): FirExpression {
  val classId = classSymbol.classId
  val classType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val kClassClassId = ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))
  val kClassType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(kClassClassId),
      arrayOf(classType),
      isMarkedNullable = false,
    )

  return buildGetClassCall {
    coneTypeOrNull = kClassType
    val qualifier = buildResolvedQualifier {
      packageFqName = classId.packageFqName
      relativeClassFqName = classId.relativeClassName
      coneTypeOrNull = classType
      qualifierSymbol = classSymbol
      resolvedToCompanionObject = false
    }
    argumentList =
      buildResolvedArgumentList(
        original = null,
        mapping =
          linkedMapOf(
            qualifier to buildSyntheticClassLiteralParameter(classType, classSymbol, session)
          ),
      )
  }
}

internal fun buildClassExpression(
  classId: ClassId,
  session: FirSession,
  ownerSymbol: FirRegularClassSymbol? = null,
): FirExpression {
  val classType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val kClassClassId = ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))
  val kClassType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(kClassClassId),
      arrayOf(classType),
      isMarkedNullable = false,
    )

  val classSymbol =
    session.symbolProvider.getClassLikeSymbolByClassId(classId)
      ?: ownerSymbol?.let { findClassLikeSymbolInContainingFile(it, classId, session) }
      ?: ownerSymbol?.let {
        findClassLikeSymbolInPackageFiles(it.classId.packageFqName, classId, session)
      }

  return buildGetClassCall {
    coneTypeOrNull = kClassType
    val qualifier = buildResolvedQualifier {
      packageFqName = classId.packageFqName
      relativeClassFqName = classId.relativeClassName
      coneTypeOrNull = classType
      qualifierSymbol = classSymbol
      resolvedToCompanionObject = false
    }
    argumentList =
      buildResolvedArgumentList(
        original = null,
        mapping =
          linkedMapOf(
            qualifier to
              buildSyntheticClassLiteralParameter(
                classType = classType,
                containingSymbol = classSymbol ?: ownerSymbol,
                session = session,
              )
          ),
      )
  }
}

private fun buildSyntheticClassLiteralParameter(
  classType: ConeClassLikeTypeImpl,
  containingSymbol: FirBasedSymbol<*>?,
  session: FirSession,
) = buildValueParameter {
  moduleData = session.moduleData
  resolvePhase = FirResolvePhase.BODY_RESOLVE
  origin = FirDeclarationOrigin.Synthetic.PluginFile
  returnTypeRef = classType.toFirResolvedTypeRef()
  name = Name.identifier("value")
  symbol = FirValueParameterSymbol()
  containingDeclarationSymbol =
    containingSymbol ?: error("Unable to determine containing symbol for generated class literal")
}

@OptIn(DirectDeclarationsAccess::class)
internal fun findClassLikeSymbolInContainingFile(
  ownerSymbol: FirRegularClassSymbol,
  classId: ClassId,
  session: FirSession,
): FirRegularClassSymbol? {
  val file = session.firProvider.getFirClassifierContainerFileIfAny(ownerSymbol) ?: return null
  return findClassLikeSymbolInFile(file, classId)
}

@OptIn(DirectDeclarationsAccess::class)
internal fun findClassLikeSymbolInPackageFiles(
  packageFqName: FqName,
  classId: ClassId,
  session: FirSession,
): FirRegularClassSymbol? {
  return session.firProvider.getFirFilesByPackage(packageFqName).firstNotNullOfOrNull { file ->
    findClassLikeSymbolInFile(file, classId)
  }
}

@OptIn(DirectDeclarationsAccess::class)
private fun findClassLikeSymbolInFile(file: FirFile, classId: ClassId): FirRegularClassSymbol? {
  return file.declarations.firstNotNullOfOrNull { declaration ->
    findClassLikeSymbolInDeclaration(declaration, classId)
  }
}

@OptIn(DirectDeclarationsAccess::class)
private fun findClassLikeSymbolInDeclaration(
  declaration: FirDeclaration,
  classId: ClassId,
): FirRegularClassSymbol? {
  val regularClass = declaration as? FirRegularClass
  if (regularClass?.symbol?.classId == classId) {
    return regularClass.symbol
  }
  if (regularClass != null) {
    return regularClass.declarations.firstNotNullOfOrNull { nestedDeclaration ->
      findClassLikeSymbolInDeclaration(nestedDeclaration, classId)
    }
  }
  return null
}

internal fun hasTransitiveSupertype(
  type: ConeKotlinType,
  session: FirSession,
  targetIds: Collection<ClassId>,
  visited: MutableSet<ClassId> = mutableSetOf(),
): Boolean {
  val classSymbol = type.toRegularClassSymbol(session) ?: return false
  val classId = classSymbol.classId
  if (!visited.add(classId)) return false
  if (classId in targetIds) return true

  return classSymbol.resolvedSuperTypeRefs.any { superTypeRef ->
    val superConeType = superTypeRef.coneType.fullyExpandedType(session)
    hasTransitiveSupertype(superConeType, session, targetIds, visited)
  }
}
