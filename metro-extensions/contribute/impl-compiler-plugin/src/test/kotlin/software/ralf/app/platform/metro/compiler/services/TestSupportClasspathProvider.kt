package software.ralf.app.platform.metro.compiler.services

import java.io.File
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices

private val testSupportClasspath: List<File> =
  System.getProperty("testSupport.classpath")
    ?.split(File.pathSeparator)
    ?.filter { it.isNotBlank() }
    ?.map(::File) ?: error("Unable to get a valid classpath from 'testSupport.classpath' property")

fun TestConfigurationBuilder.configureTestSupportClasspath() {
  useConfigurators(::TestSupportClasspathConfigurator)
  useCustomRuntimeClasspathProviders(::TestSupportRuntimeClasspathProvider)
}

private class TestSupportClasspathConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    configuration.addJvmClasspathRoots(testSupportClasspath)
  }
}

private class TestSupportRuntimeClasspathProvider(testServices: TestServices) :
  RuntimeClasspathProvider(testServices) {
  override fun runtimeClassPaths(module: TestModule): List<File> = testSupportClasspath
}
