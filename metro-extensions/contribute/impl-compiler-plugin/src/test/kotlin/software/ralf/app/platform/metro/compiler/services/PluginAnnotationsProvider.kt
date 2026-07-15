package software.ralf.app.platform.metro.compiler.services

import java.io.File
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices

private val annotationsRuntimeClasspath: List<File> =
  System.getProperty("annotationsRuntime.classpath")
    ?.split(File.pathSeparator)
    ?.filter { it.isNotBlank() }
    ?.map(::File)
    ?: error("Unable to get a valid classpath from 'annotationsRuntime.classpath' property")

fun TestConfigurationBuilder.configureAnnotations() {
  useConfigurators(::PluginAnnotationsProvider)
  useCustomRuntimeClasspathProviders(::PluginAnnotationsClasspathProvider)
}

private class PluginAnnotationsProvider(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    configuration.addJvmClasspathRoots(annotationsRuntimeClasspath)
  }
}

private class PluginAnnotationsClasspathProvider(testServices: TestServices) :
  RuntimeClasspathProvider(testServices) {
  override fun runtimeClassPaths(module: TestModule): List<File> = annotationsRuntimeClasspath
}
