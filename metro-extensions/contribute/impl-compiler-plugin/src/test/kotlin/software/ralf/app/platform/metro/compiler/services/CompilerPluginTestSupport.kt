package software.ralf.app.platform.metro.compiler.services

import dev.zacsweers.metro.compiler.MetroCommandLineProcessor
import dev.zacsweers.metro.compiler.MetroCompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import software.ralf.app.platform.metro.compiler.AppPlatformMetroExtensionsPluginComponentRegistrar

fun TestConfigurationBuilder.configurePlugin() {
  useConfigurators(::ExtensionRegistrarConfigurator)
  configureAnnotations()
  configureMetroRuntime()
}

fun TestConfigurationBuilder.configureMetroImports() {
  useSourcePreprocessor(::MetroImportsPreprocessor)
}

fun TestConfigurationBuilder.configureKotlinTestImports() {
  useSourcePreprocessor(::KotlinTestImportsPreprocessor)
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  private val metroRegistrar = MetroCompilerPluginRegistrar()
  private val extensionsRegistrar = AppPlatformMetroExtensionsPluginComponentRegistrar()

  override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
    module: TestModule,
    configuration: CompilerConfiguration,
  ) {
    val metroCommandLineProcessor = MetroCommandLineProcessor()
    listOf("generate-classes-in-ir", "generate-contribution-hints-in-fir").forEach { optionName ->
      val option = metroCommandLineProcessor.pluginOptions.single { it.optionName == optionName }
      metroCommandLineProcessor.processOption(option, "true", configuration)
    }
    with(metroRegistrar) { registerExtensions(configuration) }
    with(extensionsRegistrar) { registerExtensions(configuration) }
  }
}
