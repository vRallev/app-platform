package software.ralf.app.platform.metro.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CommandLineProcessor::class)
public class AppPlatformCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = PLUGIN_ID

  override val pluginOptions: Collection<AbstractCliOption> = listOf(GENERATE_CLASSES_IN_IR)

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ) {
    when (option.optionName) {
      GENERATE_CLASSES_IN_IR.optionName ->
        AppPlatformCompilerConfiguration.setGenerateClassesInIr(configuration, value.toBoolean())
      else -> error("Unknown App Platform compiler option: ${option.optionName}")
    }
  }

  public companion object {
    public const val PLUGIN_ID: String = "software.ralf.app.platform.metro.compiler"

    public val GENERATE_CLASSES_IN_IR: CliOption =
      CliOption(
        optionName = "generate-classes-in-ir",
        valueDescription = "true|false",
        description = "Generate App Platform's Metro binding containers in IR.",
        required = false,
        allowMultipleOccurrences = false,
      )
  }
}
