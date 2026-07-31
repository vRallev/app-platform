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

private val metroRuntimeClasspath: List<File> =
  System.getProperty("metroRuntime.classpath")
    ?.split(File.pathSeparator)
    ?.filter { it.isNotBlank() }
    ?.map(::File) ?: error("Unable to get a valid classpath from 'metroRuntime.classpath' property")

fun TestConfigurationBuilder.configureMetroRuntime() {
  useConfigurators(::MetroRuntimeEnvironmentConfigurator)
  useCustomRuntimeClasspathProviders(::MetroRuntimeClasspathProvider)
}

private class MetroRuntimeEnvironmentConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    configuration.addJvmClasspathRoots(metroRuntimeClasspath)
    val processor = MetroCommandLineProcessor()
    listOf("generate-classes-in-ir", "generate-contribution-hints-in-fir").forEach { optionName ->
      val option = processor.pluginOptions.single { it.optionName == optionName }
      processor.processOption(option, "true", configuration)
    }
  }
}

private class MetroRuntimeClasspathProvider(testServices: TestServices) :
  RuntimeClasspathProvider(testServices) {
  override fun runtimeClassPaths(module: TestModule): List<File> = metroRuntimeClasspath
}
