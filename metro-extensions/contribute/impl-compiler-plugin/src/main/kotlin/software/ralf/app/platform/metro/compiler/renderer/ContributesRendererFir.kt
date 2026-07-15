package software.ralf.app.platform.metro.compiler.renderer

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
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
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
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys
import software.ralf.app.platform.metro.compiler.fir.addGeneratedDeclaration
import software.ralf.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.ralf.app.platform.metro.compiler.fir.buildClassExpression
import software.ralf.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation
import software.ralf.app.platform.metro.compiler.fir.resolveTypeRef

/**
 * Generates the declaration shape for `@ContributesRenderer` classes.
 *
 * Pseudo Kotlin:
 * ```kotlin
 * @ContributesRenderer
 * class TestRenderer : Renderer<Model> {
 *
 *   @ContributesTo(RendererScope::class)
 *   @BindingContainer
 *   @Origin(TestRenderer::class)
 *   interface RendererContribution {
 *     @Binds
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     fun provideTestRendererModel(renderer: TestRenderer): Renderer<*>
 *
 *     companion object {
 *       @Provides
 *       fun provideTestRenderer(): TestRenderer
 *
 *       @Provides
 *       @IntoMap
 *       @RendererKey(Model::class)
 *       @ForScope(RendererScope::class)
 *       fun provideTestRendererModelKey(): KClass<out Renderer<*>>
 *     }
 *   }
 * }
 * ```
 *
 * No top-level graph interface is generated. If the renderer has an `@Inject` constructor, the
 * nested declaration omits `provideTestRenderer()` and only the map bindings are generated. When
 * `includeSealedSubtypes` is enabled, the `Model` binding pair is generated once per collected
 * model subtype.
 */
public class ContributesRendererFir(session: FirSession) :
  MetroFirDeclarationGenerationExtension(session), MetroContributionHintExtension {

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesRendererIds.PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return annotatedRendererClasses().map { classSymbol ->
      ContributionHint(
        contributingClassId =
          classSymbol.classId.createNestedClassId(ContributesRendererIds.NESTED_INTERFACE_NAME),
        scope = ClassIds.RENDERER_SCOPE,
      )
    }
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    generatedRendererContributionOwner(classSymbol)?.let { rendererOwner ->
      val metadata = rendererContributionMetadata(rendererOwner, session) ?: return emptySet()
      return if (!metadata.hasInjectAnnotation || metadata.modelClasses.isNotEmpty()) {
        setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
      } else {
        emptySet()
      }
    }

    return if (
      hasAnnotation(classSymbol, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
    ) {
      setOf(ContributesRendererIds.NESTED_INTERFACE_NAME)
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
      val rendererOwner = generatedRendererContributionOwner(owner) ?: return null
      val metadata = rendererContributionMetadata(rendererOwner, session) ?: return null
      val contributionSymbol = owner as? FirRegularClassSymbol ?: return null
      val companion =
        buildProviderCompanionObject(contributionSymbol) { companionClassId ->
          buildProviderFunctions(companionClassId, rendererOwner, metadata)
        }
      return companion?.symbol
    }

    if (name != ContributesRendererIds.NESTED_INTERFACE_NAME) return null
    if (!hasAnnotation(owner, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session))
      return null

    val rendererOwner = owner as? FirRegularClassSymbol ?: return null
    val metadata = rendererContributionMetadata(rendererOwner, session) ?: return null
    val nestedClassId = rendererOwner.classId.createNestedClassId(name)
    val graphSymbol = FirRegularClassSymbol(nestedClassId)

    val contributionClass = buildRegularClass {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = rendererOwner.source
      classKind = ClassKind.INTERFACE
      scopeProvider = session.kotlinScopeProvider
      this.name = nestedClassId.shortClassName
      symbol = graphSymbol
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.ABSTRACT,
          Visibilities.Public.toEffectiveVisibility(rendererOwner, forClass = true),
        )
      superTypeRefs += session.builtinTypes.anyType
      annotations += buildSimpleAnnotationCall(ClassIds.BINDING_CONTAINER, graphSymbol, session)
      annotations += buildReflectionContributesToAnnotation()
      annotations += buildOriginAnnotation(rendererOwner, graphSymbol)
      for (function in buildBindFunctions(nestedClassId, rendererOwner, metadata)) {
        declarations += function
      }
    }

    return contributionClass.symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    return if (generatedRendererProviderCompanionOwner(classSymbol) != null) {
      setOf(SpecialNames.INIT)
    } else {
      emptySet()
    }
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    if (generatedRendererProviderCompanionOwner(context.owner) == null) {
      return emptyList()
    }
    val constructor: FirConstructor =
      createDefaultPrivateConstructor(context.owner, Keys.ContributesRendererGeneratorKey)
    return listOf(constructor.symbol)
  }

  private fun buildProviderCompanionObject(
    classSymbol: FirRegularClassSymbol,
    buildFunctions: (ClassId) -> List<FirFunction>,
  ) =
    createCompanionObject(classSymbol, Keys.ContributesRendererGeneratorKey).let { companion ->
      val functions = buildFunctions(companion.symbol.classId)
      if (functions.isEmpty()) {
        null
      } else {
        companion.apply {
          for (function in functions) {
            addGeneratedDeclaration(function)
          }
        }
      }
    }

  private fun generatedRendererContributionOwner(
    classSymbol: FirClassSymbol<*>
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != ContributesRendererIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = classSymbol.classId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
    }
  }

  private fun generatedRendererProviderCompanionOwner(
    classSymbol: FirClassSymbol<*>
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      return null
    }
    val contributionClassId = classSymbol.classId.outerClassId ?: return null
    if (contributionClassId.shortClassName != ContributesRendererIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = contributionClassId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    val metadata = rendererContributionMetadata(ownerSymbol, session) ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session) &&
        (!metadata.hasInjectAnnotation || metadata.modelClasses.isNotEmpty())
    }
  }

  private fun buildBindFunctions(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    metadata: RendererContributionMetadata,
  ): List<FirFunction> {
    return metadata.modelClasses.map { modelClass ->
      buildProvideRendererIntoMapFunction(graphClassId, owner, modelClass)
    }
  }

  private fun annotatedRendererClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesRendererIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  private fun buildProviderFunctions(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    metadata: RendererContributionMetadata,
    includeRendererProvider: Boolean = true,
  ): List<FirFunction> {
    val functions = mutableListOf<FirFunction>()
    if (includeRendererProvider && !metadata.hasInjectAnnotation) {
      functions += buildProvideRendererFunction(graphClassId, owner)
    }
    metadata.modelClasses.forEach { modelClass ->
      functions += buildProvideRendererKeyFunction(graphClassId, owner, modelClass)
    }
    return functions
  }

  private fun buildProvideRendererFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
  ): FirFunction {
    val functionName =
      "provide${ContributesRendererIds.generatedSafeClassNamePrefix(owner.classId)}"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val constructor = rendererConstructor(owner)
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
      origin = Keys.ContributesRendererGeneratorKey.origin
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

  private fun buildProvideRendererIntoMapFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
  ): FirFunction {
    val functionName =
      "provide${ContributesRendererIds.generatedSafeClassNamePrefix(owner.classId)}" +
        ContributesRendererIds.generatedModelClassNameSuffix(modelClass.classId)
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val ownerType = owner.defaultType()

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = rendererStarType().toFirResolvedTypeRef()
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
        origin = Keys.ContributesRendererGeneratorKey.origin
        source = owner.source
        returnTypeRef = ownerType.toFirResolvedTypeRef()
        name = Name.identifier("renderer")
        symbol = FirValueParameterSymbol()
        containingDeclarationSymbol = functionSymbol
      }
      annotations += buildSimpleAnnotationCall(ClassIds.BINDS, functionSymbol, session)
      annotations += buildSimpleAnnotationCall(ClassIds.INTO_MAP, functionSymbol, session)
      annotations += buildRendererKeyAnnotation(owner, modelClass, functionSymbol)
    }
  }

  private fun buildProvideRendererKeyFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
  ): FirFunction {
    val functionName =
      "provide${ContributesRendererIds.generatedSafeClassNamePrefix(owner.classId)}" +
        ContributesRendererIds.generatedModelClassNameSuffix(modelClass.classId) +
        "Key"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = kClassProducerOf(rendererStarType()).toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      annotations += buildSimpleAnnotationCall(ClassIds.PROVIDES, functionSymbol, session)
      annotations += buildSimpleAnnotationCall(ClassIds.INTO_MAP, functionSymbol, session)
      annotations += buildRendererKeyAnnotation(owner, modelClass, functionSymbol)
      annotations += buildForScopeAnnotation(functionSymbol)
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
      origin = Keys.ContributesRendererGeneratorKey.origin
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

  private fun rendererStarType() =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassIds.RENDERER),
      arrayOf(ConeStarProjection),
      isMarkedNullable = false,
    )

  private fun kClassProducerOf(type: ConeKotlinType) =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))),
      arrayOf(ConeKotlinTypeProjectionOut(type)),
      isMarkedNullable = false,
    )

  private fun buildReflectionContributesToAnnotation() = buildAnnotation {
    val contributesToSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.CONTRIBUTES_TO)
        as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.CONTRIBUTES_TO} not found on the classpath")
    annotationTypeRef = contributesToSymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("scope")] = buildClassExpression(ClassIds.RENDERER_SCOPE, session)
    }
  }

  private fun buildForScopeAnnotation(containingSymbol: FirNamedFunctionSymbol) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.FOR_SCOPE,
      argName = Name.identifier("scope"),
      argument = buildClassExpression(ClassIds.RENDERER_SCOPE, session),
      containingSymbol = containingSymbol,
      session = session,
    )

  private fun buildOriginAnnotation(
    owner: FirRegularClassSymbol,
    containingSymbol: FirRegularClassSymbol,
  ) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.ORIGIN,
      argName = Name.identifier("value"),
      argument = buildClassExpression(owner, session),
      containingSymbol = containingSymbol,
      session = session,
    )

  private fun buildRendererKeyAnnotation(
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
    containingSymbol: FirNamedFunctionSymbol,
  ) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.RENDERER_KEY,
      argName = Name.identifier("value"),
      argument =
        modelClass.classSymbol?.let { buildClassExpression(it, session) }
          ?: buildClassExpression(modelClass.classId, session, owner),
      containingSymbol = containingSymbol,
      session = session,
    )

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesRendererFir(session)
  }
}
