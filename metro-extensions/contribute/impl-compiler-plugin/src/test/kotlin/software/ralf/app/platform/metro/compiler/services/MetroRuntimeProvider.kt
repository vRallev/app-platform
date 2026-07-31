package software.ralf.app.platform.metro.compiler.services

import dev.zacsweers.metro.compiler.MetroCommandLineProcessor
import java.io.File
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import software.ralf.app.platform.metro.compiler.AppPlatformCommandLineProcessor

private val metroRuntimeClasspath: List<File> =
  System.getProperty("metroRuntime.classpath")
    ?.split(File.pathSeparator)
    ?.filter { it.isNotBlank() }
    ?.map(::File) ?: error("Unable to get a valid classpath from 'metroRuntime.classpath' property")

fun TestConfigurationBuilder.configureMetroRuntime() {
  useConfigurators(::MetroRuntimeEnvironmentConfigurator)
  useCustomRuntimeClasspathProviders(::MetroRuntimeClasspathProvider)
}

fun TestConfigurationBuilder.configurePureIrMetroRuntime() {
  useConfigurators(::PureIrMetroRuntimeEnvironmentConfigurator)
  useCustomRuntimeClasspathProviders(::MetroRuntimeClasspathProvider)
}

private class MetroRuntimeEnvironmentConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    configuration.configureMetro(generateClassesInIr = true, generateContributionHintsInFir = true)
  }
}

private class PureIrMetroRuntimeEnvironmentConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    configuration.configureMetro(
      generateClassesInIr = true,
      generateContributionHintsInFir = false,
    )
  }
}

private fun CompilerConfiguration.configureMetro(
  generateClassesInIr: Boolean,
  generateContributionHintsInFir: Boolean,
) {
  addJvmClasspathRoots(metroRuntimeClasspath)
  MetroCommandLineProcessor().apply {
    mapOf(
        "generate-classes-in-ir" to generateClassesInIr,
        "generate-contribution-hints-in-fir" to generateContributionHintsInFir,
      )
      .forEach { (optionName, value) ->
        val option = pluginOptions.single { it.optionName == optionName }
        processOption(option, value.toString(), this@configureMetro)
      }
  }
  AppPlatformCommandLineProcessor().apply {
    processOption(
      AppPlatformCommandLineProcessor.GENERATE_CLASSES_IN_IR,
      generateClassesInIr.toString(),
      this@configureMetro,
    )
  }
}

private class MetroRuntimeClasspathProvider(testServices: TestServices) :
  RuntimeClasspathProvider(testServices) {
  override fun runtimeClassPaths(module: TestModule): List<File> = metroRuntimeClasspath
}
