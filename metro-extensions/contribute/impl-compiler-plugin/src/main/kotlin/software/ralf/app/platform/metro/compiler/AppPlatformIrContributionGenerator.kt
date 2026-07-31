package software.ralf.app.platform.metro.compiler

import dev.zacsweers.metro.compiler.compat.CompatContext
import dev.zacsweers.metro.compiler.compat.IrGeneratedDeclarationsRegistrarCompat
import java.util.IdentityHashMap
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import software.ralf.app.platform.metro.compiler.renderer.ContributesRendererIds
import software.ralf.app.platform.metro.compiler.robot.ContributesRobotIds
import software.ralf.app.platform.metro.compiler.scoped.ContributesScopedIds

@Suppress("DEPRECATION")
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class AppPlatformIrContributionGenerator(
  private val pluginContext: IrPluginContext,
  private val compatContext: CompatContext,
  private val generateDeclarationsInIr: Boolean,
) {
  private val generatedByModule = IdentityHashMap<IrModuleFragment, Map<ClassId, List<IrClass>>>()

  private val metadataRegistrar: IrGeneratedDeclarationsRegistrarCompat by lazy {
    compatContext.createIrGeneratedDeclarationsRegistrar(pluginContext)
  }

  private val targetResolver = AppPlatformIrContributionTargetResolver(compatContext)

  @Synchronized
  fun generate(moduleFragment: IrModuleFragment): Map<ClassId, List<IrClass>> {
    return generatedByModule.getOrPut(moduleFragment) {
      moduleFragment
        .sourceClasses()
        .flatMap { sourceClass ->
          buildList {
            sourceClass.generateRendererContribution()?.let { add(it) }
            sourceClass.generateRobotContribution()?.let { add(it) }
            sourceClass.generateScopedContribution()?.let { add(it) }
          }
        }
        .groupBy(keySelector = { contribution -> contribution.scope }) { contribution ->
          contribution.container
        }
    }
  }

  private fun IrModuleFragment.sourceClasses(): List<IrClass> {
    val result = mutableListOf<IrClass>()

    fun collect(irClass: IrClass) {
      result += irClass
      irClass.declarations.filterIsInstance<IrClass>().toList().forEach { nestedClass ->
        collect(nestedClass)
      }
    }

    files.toList().forEach { file ->
      file.declarations.filterIsInstance<IrClass>().toList().forEach { sourceClass ->
        collect(sourceClass)
      }
    }
    return result
  }

  private fun IrClass.generateRendererContribution(): GeneratedContribution? {
    val target = targetResolver.resolveRenderer(this) ?: return null
    val classId = classId ?: return null
    val container =
      getOrCreateContainer(
        name = ContributesRendererIds.NESTED_INTERFACE_NAME,
        key = Keys.ContributesRendererGeneratorKey,
        scope = ClassIds.RENDERER_SCOPE,
      ) ?: return null

    if (!target.hasInjectAnnotation) {
      container
        .getOrCreateCompanion(Keys.ContributesRendererGeneratorKey)
        ?.addConstructorProvider(
          sourceClass = this,
          name =
            Name.identifier(
              "provide${ContributesRendererIds.generatedSafeClassNamePrefix(classId)}"
            ),
          key = Keys.ContributesRendererGeneratorKey,
        )
    }

    val rendererType = requireClass(ClassIds.RENDERER).starProjectedType
    target.modelClasses.forEach { modelClass ->
      val modelClassId = modelClass.owner.classId ?: return@forEach
      val namePrefix =
        "provide${ContributesRendererIds.generatedSafeClassNamePrefix(classId)}" +
          ContributesRendererIds.generatedModelClassNameSuffix(modelClassId)
      container.addBindsFunction(
        name = Name.identifier(namePrefix),
        parameterName = Name.identifier("renderer"),
        parameterType = defaultType,
        returnType = rendererType,
        key = Keys.ContributesRendererGeneratorKey,
        annotations =
          listOf(
            annotation(ClassIds.BINDS, container.symbol),
            annotation(ClassIds.INTO_MAP, container.symbol),
            annotation(ClassIds.RENDERER_KEY, container.symbol, modelClass),
          ),
      )
      container
        .getOrCreateCompanion(Keys.ContributesRendererGeneratorKey)
        ?.addRendererKeyFunction(
          sourceClass = this,
          modelClass = modelClass,
          name = Name.identifier("${namePrefix}Key"),
        )
    }

    return GeneratedContribution(ClassIds.RENDERER_SCOPE, container)
  }

  private fun IrClass.generateRobotContribution(): GeneratedContribution? {
    val target = targetResolver.resolveRobot(this) ?: return null
    val classId = classId ?: return null
    val container =
      getOrCreateContainer(
        name = ContributesRobotIds.NESTED_INTERFACE_NAME,
        key = Keys.ContributesRobotGeneratorKey,
        scope = target.scope,
      ) ?: return null

    if (!target.hasInjectAnnotation) {
      container
        .getOrCreateCompanion(Keys.ContributesRobotGeneratorKey)
        ?.addConstructorProvider(
          sourceClass = this,
          name = Name.identifier("provide${ContributesRobotIds.generatedClassNamePrefix(classId)}"),
          key = Keys.ContributesRobotGeneratorKey,
        )
    }

    container.addBindsFunction(
      name = Name.identifier("provide${ContributesRobotIds.generatedClassNamePrefix(classId)}IntoMap"),
      parameterName = Name.identifier("robot"),
      parameterType = defaultType,
      returnType = requireClass(ClassIds.ROBOT).defaultType,
      key = Keys.ContributesRobotGeneratorKey,
      annotations =
        listOf(
          annotation(ClassIds.BINDS, container.symbol),
          annotation(ClassIds.INTO_MAP, container.symbol),
          annotation(ClassIds.ROBOT_KEY, container.symbol, symbol),
        ),
    )

    return GeneratedContribution(target.scope, container)
  }

  private fun IrClass.generateScopedContribution(): GeneratedContribution? {
    val target = targetResolver.resolveScoped(this) ?: return null
    val classId = classId ?: return null
    val container =
      getOrCreateContainer(
        name = ContributesScopedIds.NESTED_INTERFACE_NAME,
        key = Keys.ContributesScopedGeneratorKey,
        scope = target.scope,
      ) ?: return null

    if (!target.hasInjectAnnotation) {
      container
        .getOrCreateCompanion(Keys.ContributesScopedGeneratorKey)
        ?.addConstructorProvider(
          sourceClass = this,
          name = Name.identifier("provide${ContributesScopedIds.generatedOwnerName(classId)}"),
          key = Keys.ContributesScopedGeneratorKey,
          additionalAnnotations = target.scopeAnnotations.map { it.deepCopyWithSymbols() },
        )
    }

    target.otherSuperType?.let { otherSuperType ->
      val otherSuperTypeClassId = otherSuperType.classOrNull?.owner?.classId ?: return@let
      container.addBindsFunction(
        name = Name.identifier("bind${ContributesScopedIds.generatedTypeName(otherSuperTypeClassId)}"),
        parameterName = Name.identifier("instance"),
        parameterType = defaultType,
        returnType = otherSuperType,
        key = Keys.ContributesScopedGeneratorKey,
        annotations = listOf(annotation(ClassIds.BINDS, container.symbol)),
      )
    }

    container.addBindsFunction(
      name = Name.identifier("bind${ContributesScopedIds.generatedOwnerName(classId)}Scoped"),
      parameterName = Name.identifier("instance"),
      parameterType = defaultType,
      returnType = requireClass(ClassIds.SCOPED).defaultType,
      key = Keys.ContributesScopedGeneratorKey,
      annotations =
        listOf(
          annotation(ClassIds.BINDS, container.symbol),
          annotation(ClassIds.INTO_SET, container.symbol),
          annotation(ClassIds.FOR_SCOPE, container.symbol, requireClass(target.scope)),
        ),
    )

    return GeneratedContribution(target.scope, container)
  }

  private fun IrClass.getOrCreateContainer(
    name: Name,
    key: org.jetbrains.kotlin.GeneratedDeclarationKey,
    scope: ClassId,
  ): IrClass? {
    declarations.filterIsInstance<IrClass>().firstOrNull { it.name == name }?.let {
      return it
    }
    if (!generateDeclarationsInIr) return null

    val sourceClass = this
    val container =
      pluginContext.irFactory
        .buildClass {
          this.name = name
          origin = IrDeclarationOrigin.GeneratedByPlugin(key)
          kind = ClassKind.INTERFACE
          visibility = sourceClass.visibility
          modality = Modality.ABSTRACT
        }
        .apply {
          parent = sourceClass
          createThisReceiverParameter()
          addAnnotation(annotation(ClassIds.BINDING_CONTAINER, symbol))
          addAnnotation(annotation(ClassIds.CONTRIBUTES_TO, symbol, requireClass(scope)))
          addAnnotation(annotation(ClassIds.ORIGIN, symbol, sourceClass.symbol))
        }
    addChild(container)
    metadataRegistrar.registerClassAsMetadataVisible(container)
    return container
  }

  private fun IrClass.addConstructorProvider(
    sourceClass: IrClass,
    name: Name,
    key: org.jetbrains.kotlin.GeneratedDeclarationKey,
    additionalAnnotations: List<IrConstructorCall> = emptyList(),
  ) {
    val constructor = sourceClass.providerConstructor() ?: return
    val existingFunction = declarations.filterIsInstance<IrSimpleFunction>().firstOrNull {
      it.name == name
    }
    val function =
      existingFunction
        ?: addFunction(
          name = name,
          returnType = sourceClass.defaultType,
          modality = Modality.OPEN,
          key = key,
          annotations = listOf(annotation(ClassIds.PROVIDES, symbol)) + additionalAnnotations,
        )?.also { generatedFunction ->
          constructor.regularParameters().forEach { constructorParameter ->
            generatedFunction
              .addValueParameter(constructorParameter.name.asString(), constructorParameter.type)
              .copyAnnotationsFrom(constructorParameter)
          }
          metadataRegistrar.registerFunctionAsMetadataVisible(generatedFunction)
        } ?: return
    if (function.body != null) return

    val functionParameters = function.regularParameters()
    function.body =
      DeclarationIrBuilder(pluginContext, function.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        .irBlockBody {
          val call = irCallConstructor(constructor.symbol, emptyList())
          functionParameters.forEachIndexed { index, parameter ->
            call.arguments[index] = irGet(parameter)
          }
          +irReturn(call)
        }
  }

  private fun IrClass.getOrCreateCompanion(
    key: org.jetbrains.kotlin.GeneratedDeclarationKey
  ): IrClass? {
    declarations.filterIsInstance<IrClass>().firstOrNull { it.isCompanion }?.let {
      return it
    }
    if (!generateDeclarationsInIr) return null

    val container = this
    val companion =
      pluginContext.irFactory
        .buildClass {
          name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
          origin = IrDeclarationOrigin.GeneratedByPlugin(key)
          kind = ClassKind.OBJECT
          visibility = DescriptorVisibilities.PUBLIC
          modality = Modality.FINAL
          isCompanion = true
        }
        .apply {
          parent = container
          createThisReceiverParameter()
          superTypes += pluginContext.irBuiltIns.anyType
        }
    container.addChild(companion)
    metadataRegistrar.registerClassAsMetadataVisible(companion)
    companion
      .addSimpleDelegatingConstructor(
        pluginContext.irBuiltIns.anyClass.owner.primaryConstructor!!,
        pluginContext.irBuiltIns,
        isPrimary = true,
      )
      .apply {
        visibility = DescriptorVisibilities.PRIVATE
        metadataRegistrar.registerConstructorAsMetadataVisible(this)
      }
    return companion
  }

  private fun IrClass.addBindsFunction(
    name: Name,
    parameterName: Name,
    parameterType: IrType,
    returnType: IrType,
    key: org.jetbrains.kotlin.GeneratedDeclarationKey,
    annotations: List<IrConstructorCall>,
  ) {
    if (declarations.filterIsInstance<IrSimpleFunction>().any { it.name == name }) return
    val function =
      addFunction(
        name = name,
        returnType = returnType,
        modality = Modality.ABSTRACT,
        key = key,
        annotations = annotations,
      ) ?: return
    function.addValueParameter(parameterName.asString(), parameterType)
    metadataRegistrar.registerFunctionAsMetadataVisible(function)
  }

  private fun IrClass.addRendererKeyFunction(
    sourceClass: IrClass,
    modelClass: IrClassSymbol,
    name: Name,
  ) {
    val existingFunction = declarations.filterIsInstance<IrSimpleFunction>().firstOrNull {
      it.name == name
    }
    val rendererType = requireClass(ClassIds.RENDERER).starProjectedType
    val function =
      existingFunction
        ?: addFunction(
          name = name,
          returnType =
            pluginContext.irBuiltIns.kClassClass.typeWithArguments(
              listOf(makeTypeProjection(rendererType, Variance.OUT_VARIANCE))
            ),
          modality = Modality.OPEN,
          key = Keys.ContributesRendererGeneratorKey,
          annotations =
            listOf(
              annotation(ClassIds.PROVIDES, symbol),
              annotation(ClassIds.INTO_MAP, symbol),
              annotation(ClassIds.RENDERER_KEY, symbol, modelClass),
              annotation(ClassIds.FOR_SCOPE, symbol, requireClass(ClassIds.RENDERER_SCOPE)),
            ),
        )?.also { generatedFunction ->
          metadataRegistrar.registerFunctionAsMetadataVisible(generatedFunction)
        } ?: return
    if (function.body != null) return

    function.body =
      DeclarationIrBuilder(pluginContext, function.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        .irBlockBody { +irReturn(classReference(sourceClass.symbol)) }
  }

  private fun IrClass.addFunction(
    name: Name,
    returnType: IrType,
    modality: Modality,
    key: org.jetbrains.kotlin.GeneratedDeclarationKey,
    annotations: List<IrConstructorCall>,
  ): IrSimpleFunction? {
    if (!generateDeclarationsInIr) return null
    val container = this
    return pluginContext.irFactory
      .buildFun {
        this.name = name
        origin = IrDeclarationOrigin.GeneratedByPlugin(key)
        this.returnType = returnType
        visibility = DescriptorVisibilities.PUBLIC
        this.modality = modality
      }
      .apply {
        parent = container
        container.thisReceiver?.copyTo(this)?.let { receiver -> parameters += receiver }
        annotations.forEach { annotation ->
          with(compatContext) { addAnnotationCompat(annotation) }
        }
        container.addChild(this)
      }
  }

  private fun IrClass.providerConstructor(): IrConstructor? {
    return primaryConstructor ?: constructors.firstOrNull()
  }

  private fun IrValueParameter.copyAnnotationsFrom(source: IrValueParameter) {
    source.annotations().forEach { annotation ->
      with(compatContext) {
        this@copyAnnotationsFrom.addAnnotationCompat(annotation.deepCopyWithSymbols())
      }
    }
  }

  private fun IrAnnotationContainer.annotations(): List<IrConstructorCall> {
    return with(compatContext) { annotationsCompat() }
  }

  private fun IrAnnotationContainer.addAnnotation(annotation: IrConstructorCall) {
    with(compatContext) { addAnnotationCompat(annotation) }
  }

  private fun IrConstructor.regularParameters(): List<IrValueParameter> {
    return parameters.filter { it.kind == IrParameterKind.Regular }
  }

  private fun IrSimpleFunction.regularParameters(): List<IrValueParameter> {
    return parameters.filter { it.kind == IrParameterKind.Regular }
  }

  private fun requireClass(classId: ClassId): IrClassSymbol {
    return requireNotNull(pluginContext.referenceClass(classId)) { "Could not find $classId" }
  }

  private fun annotation(
    classId: ClassId,
    parentSymbol: IrSymbol,
    classArgument: IrClassSymbol? = null,
  ): IrConstructorCall {
    val annotationClass = requireClass(classId)
    val constructor =
      annotationClass.owner.primaryConstructor?.symbol ?: annotationClass.constructors.first()
    val builder =
      DeclarationIrBuilder(pluginContext, parentSymbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
    return with(compatContext) {
        builder.irAnnotationCompat(constructor, typeArguments = emptyList())
      }
      .apply {
        classArgument?.let { arguments[0] = classReference(it) }
      }
  }

  private fun classReference(classSymbol: IrClassSymbol): IrClassReference {
    return IrClassReferenceImpl(
      UNDEFINED_OFFSET,
      UNDEFINED_OFFSET,
      pluginContext.irBuiltIns.kClassClass.typeWith(classSymbol.defaultType),
      classSymbol,
      classSymbol.defaultType,
    )
  }

  private data class GeneratedContribution(val scope: ClassId, val container: IrClass)
}
