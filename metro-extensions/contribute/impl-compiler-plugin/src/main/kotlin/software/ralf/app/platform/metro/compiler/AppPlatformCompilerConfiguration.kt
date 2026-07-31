package software.ralf.app.platform.metro.compiler

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object AppPlatformCompilerConfiguration {
  private val generateClassesInIrKey =
    CompilerConfigurationKey<Boolean>("Generate App Platform classes in IR")

  fun generateClassesInIr(configuration: CompilerConfiguration): Boolean {
    return configuration.get(generateClassesInIrKey) ?: true
  }

  fun setGenerateClassesInIr(configuration: CompilerConfiguration, value: Boolean) {
    configuration.put(generateClassesInIrKey, value)
  }
}
