package software.ralf.app.platform.inject

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import software.ralf.app.platform.inject.processor.ContributesBindingProcessor
import software.ralf.app.platform.inject.processor.ContributesBindingScopedProcessor
import software.ralf.app.platform.inject.processor.ContributesMockImplProcessor
import software.ralf.app.platform.inject.processor.ContributesRealImplProcessor
import software.ralf.app.platform.inject.processor.ContributesRendererProcessor
import software.ralf.app.platform.inject.processor.ContributesRobotProcessor
import software.ralf.app.platform.ksp.CompositeSymbolProcessor

/** Entry point for KSP to pick up our [SymbolProcessor]. */
@AutoService(SymbolProcessorProvider::class)
@Suppress("unused")
public class KotlinInjectExtensionSymbolProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return CompositeSymbolProcessor(
      ContributesBindingProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
      ),
      ContributesBindingScopedProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
      ),
      ContributesRendererProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
      ),
      ContributesRealImplProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
      ),
      ContributesMockImplProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
      ),
      ContributesRobotProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
      ),
    )
  }
}
