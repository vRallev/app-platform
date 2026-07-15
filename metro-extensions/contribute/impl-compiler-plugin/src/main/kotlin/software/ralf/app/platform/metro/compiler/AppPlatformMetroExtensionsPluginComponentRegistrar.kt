package software.ralf.app.platform.metro.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import software.ralf.app.platform.metro.compiler.renderer.ContributesRendererIrExtension
import software.ralf.app.platform.metro.compiler.robot.ContributesRobotIrExtension
import software.ralf.app.platform.metro.compiler.scoped.ContributesScopedIrExtension

@AutoService(CompilerPluginRegistrar::class)
public class AppPlatformMetroExtensionsPluginComponentRegistrar : CompilerPluginRegistrar() {
  override val pluginId: String = "software.ralf.app.platform.metro.compiler"
  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(AppPlatformMetroExtensionsPluginRegistrar())
    IrGenerationExtension.registerExtension(ContributesRendererIrExtension())
    IrGenerationExtension.registerExtension(ContributesRobotIrExtension())
    IrGenerationExtension.registerExtension(ContributesScopedIrExtension())
  }
}
