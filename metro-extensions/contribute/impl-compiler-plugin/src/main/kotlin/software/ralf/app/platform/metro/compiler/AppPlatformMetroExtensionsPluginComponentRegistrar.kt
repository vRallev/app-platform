package software.ralf.app.platform.metro.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@AutoService(CompilerPluginRegistrar::class)
public class AppPlatformMetroExtensionsPluginComponentRegistrar : CompilerPluginRegistrar() {
  override val pluginId: String = "software.ralf.app.platform.metro.compiler"
  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(AppPlatformMetroExtensionsPluginRegistrar())
    IrGenerationExtension.registerExtension(AppPlatformIrDeclarationGenerationExtension())
  }
}
