package software.ralf.app.platform.metro.compiler.robot

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.MetroOptions
import dev.zacsweers.metro.compiler.api.fir.MetroContributionHintExtension
import dev.zacsweers.metro.compiler.api.fir.MetroContributionHintExtension.ContributionHint
import dev.zacsweers.metro.compiler.api.fir.MetroFirDeclarationGenerationExtension
import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys
import software.ralf.app.platform.metro.compiler.fir.addGeneratedDeclaration
import software.ralf.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.ralf.app.platform.metro.compiler.fir.buildClassExpression
import software.ralf.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.ralf.app.platform.metro.compiler.fir.extractScopeArgument
import software.ralf.app.platform.metro.compiler.fir.extractScopeClassId
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation
import software.ralf.app.platform.metro.compiler.fir.resolveTypeRef

/**
 * Generates the declaration shape for `@ContributesRobot` classes.
 *
 * Pseudo Kotlin:
 * ```kotlin
 * @ContributesRobot(AppScope::class)
 * class TestRobot : Robot {
 *
 *   @ContributesTo(AppScope::class)
 *   @BindingContainer
 *   interface RobotContribution {
 *     @Binds
 *     @IntoMap
 *     @RobotKey(TestRobot::class)
 *     fun provideTestRobotIntoMap(robot: TestRobot): Robot
 *
 *     companion object {
 *       @Provides
 *       fun provideTestRobot(dependency: RobotDependency): TestRobot
 *     }
 *   }
 *
 *   @ContributesTo(AppScope::class)
 *   interface RobotGraphContribution : RobotGraph
 * }
 * ```
 *
 * No top-level graph interface is generated. If the robot class is already `@Inject`-constructible,
 * the nested declaration omits `provideTestRobot(...)` and only synthesizes the map-binding method.
 */
public class ContributesRobotFir(session: FirSession) :
  MetroFirDeclarationGenerationExtension(session), MetroContributionHintExtension {

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesRobotIds.PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return annotatedRobotClasses().flatMap { classSymbol ->
      val scopeClassId =
        extractScopeClassId(classSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
          ?: return@flatMap emptyList()
      listOf(
        ContributionHint(
          contributingClassId =
            classSymbol.classId.createNestedClassId(ContributesRobotIds.NESTED_INTERFACE_NAME),
          scope = scopeClassId,
        ),
        ContributionHint(
          contributingClassId =
            classSymbol.classId.createNestedClassId(
              ContributesRobotIds.NESTED_ROBOT_GRAPH_INTERFACE_NAME
            ),
          scope = scopeClassId,
        ),
      )
    }
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    generatedRobotContributionOwner(classSymbol)?.let { owner ->
      return if (hasInjectAnnotation(owner)) {
        emptySet()
      } else {
        setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
      }
    }

    return if (
      hasAnnotation(classSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
    ) {
      setOf(
        ContributesRobotIds.NESTED_INTERFACE_NAME,
        ContributesRobotIds.NESTED_ROBOT_GRAPH_INTERFACE_NAME,
      )
    } else {
      emptySet()
    }
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext,
  ): FirClassLikeSymbol<*>? {
    if (name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      val robotOwner = generatedRobotContributionOwner(owner) ?: return null
      if (hasInjectAnnotation(robotOwner)) return null
      val contributionSymbol = owner as? FirRegularClassSymbol ?: return null
      val companion =
        buildProviderCompanionObject(contributionSymbol) { companionClassId ->
          listOf(buildProvideRobotFunction(companionClassId, robotOwner))
        }
      return companion.symbol
    }

    if (!hasAnnotation(owner, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)) return null

    val scopeArg =
      extractScopeArgument(owner, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
        ?: return null

    if (name == ContributesRobotIds.NESTED_ROBOT_GRAPH_INTERFACE_NAME) {
      val nestedClassId = owner.classId.createNestedClassId(name)
      val classSymbol = FirRegularClassSymbol(nestedClassId)
      val contributionClass = buildRegularClass {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = Keys.ContributesRobotGeneratorKey.origin
        source = owner.source
        classKind = ClassKind.INTERFACE
        scopeProvider = session.kotlinScopeProvider
        this.name = nestedClassId.shortClassName
        symbol = classSymbol
        status =
          FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.ABSTRACT,
            Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
          )
        superTypeRefs += robotGraphType().toFirResolvedTypeRef()
        annotations +=
          buildAnnotationCallWithArgument(
            ClassIds.CONTRIBUTES_TO,
            Name.identifier("scope"),
            scopeArg,
            classSymbol,
            session,
          )
        annotations += buildOriginAnnotation(owner)
      }
      return contributionClass.symbol
    }

    if (name != ContributesRobotIds.NESTED_INTERFACE_NAME) return null

    val nestedClassId = owner.classId.createNestedClassId(name)
    val classSymbol = FirRegularClassSymbol(nestedClassId)

    val contributionClass = buildRegularClass {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = owner.source
      classKind = ClassKind.INTERFACE
      scopeProvider = session.kotlinScopeProvider
      this.name = nestedClassId.shortClassName
      symbol = classSymbol
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.ABSTRACT,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      superTypeRefs += session.builtinTypes.anyType
      annotations += buildSimpleAnnotationCall(ClassIds.BINDING_CONTAINER, classSymbol, session)
      annotations +=
        buildAnnotationCallWithArgument(
          ClassIds.CONTRIBUTES_TO,
          Name.identifier("scope"),
          scopeArg,
          classSymbol,
          session,
        )
      annotations += buildOriginAnnotation(owner)
      declarations += buildProvideRobotIntoMapFunction(nestedClassId, owner)
    }

    return contributionClass.symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    return if (generatedRobotProviderCompanionOwner(classSymbol) != null) {
      setOf(SpecialNames.INIT)
    } else {
      emptySet()
    }
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    if (generatedRobotProviderCompanionOwner(context.owner) == null) {
      return emptyList()
    }
    val constructor: FirConstructor =
      createDefaultPrivateConstructor(context.owner, Keys.ContributesRobotGeneratorKey)
    return listOf(constructor.symbol)
  }

  private fun buildProviderCompanionObject(
    classSymbol: FirRegularClassSymbol,
    buildFunctions: (ClassId) -> List<FirFunction>,
  ) =
    createCompanionObject(classSymbol, Keys.ContributesRobotGeneratorKey).apply {
      for (function in buildFunctions(symbol.classId)) {
        addGeneratedDeclaration(function)
      }
    }

  private fun generatedRobotContributionOwner(
    classSymbol: FirClassSymbol<*>
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != ContributesRobotIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = classSymbol.classId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
    }
  }

  private fun generatedRobotProviderCompanionOwner(
    classSymbol: FirClassSymbol<*>
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      return null
    }
    val contributionClassId = classSymbol.classId.outerClassId ?: return null
    if (contributionClassId.shortClassName != ContributesRobotIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = contributionClassId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session) &&
        !hasInjectAnnotation(it)
    }
  }

  private fun annotatedRobotClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesRobotIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  private fun buildProvideRobotFunction(
    graphClassId: ClassId,
    owner: FirClassSymbol<*>,
  ): FirFunction {
    val functionName = "provide${ContributesRobotIds.generatedClassNamePrefix(owner.classId)}"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val constructor = robotConstructor(owner)
    val generatedParameters =
      constructor
        ?.parameters
        ?.map { it.toGeneratedParameter(constructor.owner, functionSymbol) }
        .orEmpty()
    var returnTarget: FirFunctionTarget? = null

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      annotations += buildSimpleAnnotationCall(ClassIds.PROVIDES, functionSymbol, session)
      valueParameters += generatedParameters
      if (constructor != null) {
        body =
          buildSingleExpressionBlock(
            buildReturnExpression {
              val target = FirFunctionTarget(labelName = null, isLambda = false)
              returnTarget = target
              this.target = target
              result =
                buildConstructorCall(
                  owner,
                  constructor.symbol,
                  constructor.parameters,
                  generatedParameters,
                )
            }
          )
      }
    }
      .also { function -> returnTarget?.bind(function) }
  }

  private fun buildProvideRobotIntoMapFunction(
    graphClassId: ClassId,
    owner: FirClassSymbol<*>,
  ): FirFunction {
    val functionName =
      "provide${ContributesRobotIds.generatedClassNamePrefix(owner.classId)}IntoMap"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val ownerType = owner.defaultType()

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = robotType().toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      valueParameters += buildValueParameter {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = Keys.ContributesRobotGeneratorKey.origin
        source = owner.source
        returnTypeRef = ownerType.toFirResolvedTypeRef()
        name = Name.identifier("robot")
        symbol = FirValueParameterSymbol()
        containingDeclarationSymbol = functionSymbol
      }
      annotations += buildSimpleAnnotationCall(ClassIds.BINDS, functionSymbol, session)
      annotations += buildSimpleAnnotationCall(ClassIds.INTO_MAP, functionSymbol, session)
      annotations += buildRobotKeyAnnotation(owner.classId)
    }
  }

  private data class RobotConstructor(
    val owner: FirRegularClassSymbol,
    val symbol: FirConstructorSymbol,
    val parameters: List<FirValueParameter>,
  )

  @OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
  private fun robotConstructor(owner: FirClassSymbol<*>): RobotConstructor? {
    val ownerClass = owner as? FirRegularClassSymbol ?: return null
    val constructorSymbol =
      ownerClass.declarationSymbols.filterIsInstance<FirConstructorSymbol>().firstOrNull {
        it.isPrimary
      } ?: ownerClass.declarationSymbols.filterIsInstance<FirConstructorSymbol>().firstOrNull()
    return constructorSymbol?.let { RobotConstructor(ownerClass, it, it.fir.valueParameters) }
  }

  @OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
  private fun hasInjectAnnotation(owner: FirClassSymbol<*>): Boolean {
    if (hasAnnotation(owner, ClassIds.INJECT, session)) return true
    val ownerClass = owner as? FirRegularClassSymbol ?: return false
    return ownerClass.declarationSymbols.filterIsInstance<FirConstructorSymbol>().any { constructor
      ->
      constructor.fir.annotations.any { it.toAnnotationClassIdSafe(session) == ClassIds.INJECT }
    }
  }

  private fun FirValueParameter.toGeneratedParameter(
    owner: FirRegularClassSymbol,
    functionSymbol: FirNamedFunctionSymbol,
  ): FirValueParameter {
    val constructorParameter = this
    return buildValueParameter {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = null
      returnTypeRef =
        resolveTypeRef(constructorParameter.returnTypeRef, owner, session)?.toFirResolvedTypeRef()
          ?: constructorParameter.returnTypeRef
      name = constructorParameter.name
      symbol = FirValueParameterSymbol()
      containingDeclarationSymbol = functionSymbol
      isCrossinline = constructorParameter.isCrossinline
      isNoinline = constructorParameter.isNoinline
      isVararg = constructorParameter.isVararg
      annotations += constructorParameter.annotations
    }
  }

  private fun buildConstructorCall(
    owner: FirClassSymbol<*>,
    constructorSymbol: FirConstructorSymbol,
    constructorParameters: List<FirValueParameter>,
    generatedParameters: List<FirValueParameter>,
  ): FirExpression {
    return buildFunctionCall {
      coneTypeOrNull = owner.defaultType()
      calleeReference = buildResolvedNamedReference {
        name = owner.name
        resolvedSymbol = constructorSymbol
      }
      argumentList =
        buildResolvedArgumentList(
          original = null,
          mapping =
            generatedParameters.zip(constructorParameters).associateTo(LinkedHashMap()) {
              (generatedParameter, constructorParameter) ->
              buildParameterAccess(generatedParameter) to constructorParameter
            },
        )
    }
  }

  private fun buildParameterAccess(parameter: FirValueParameter): FirExpression {
    return buildPropertyAccessExpression {
      source = parameter.source
      coneTypeOrNull = parameter.returnTypeRef.coneType
      calleeReference = buildResolvedNamedReference {
        name = parameter.name
        resolvedSymbol = parameter.symbol
      }
    }
  }

  private fun generatedGraphType(graphClassId: ClassId) =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(graphClassId),
      emptyArray(),
      isMarkedNullable = false,
    )

  private fun robotType() =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassIds.ROBOT),
      emptyArray(),
      isMarkedNullable = false,
    )

  private fun robotGraphType() =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassIds.ROBOT_GRAPH),
      emptyArray(),
      isMarkedNullable = false,
    )

  private fun buildOriginAnnotation(owner: FirClassSymbol<*>) = buildAnnotation {
    val originSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.ORIGIN) as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.ORIGIN} not found on the classpath")
    annotationTypeRef = originSymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("value")] = buildClassExpression(owner, session)
    }
  }

  private fun buildRobotKeyAnnotation(robotClassId: ClassId) = buildAnnotation {
    val robotKeySymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.ROBOT_KEY)
        as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.ROBOT_KEY} not found on the classpath")
    annotationTypeRef = robotKeySymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("value")] = buildClassExpression(robotClassId, session)
    }
  }

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesRobotFir(session)
  }
}
