package software.ralf.app.platform.metro.compiler

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import software.ralf.app.platform.metro.compiler.renderer.ContributesRendererIrExtension
import software.ralf.app.platform.metro.compiler.robot.ContributesRobotIrExtension
import software.ralf.app.platform.metro.compiler.scoped.ContributesScopedIrExtension

@AutoService(CompilerPluginRegistrar::class)
public class AppPlatformMetroExtensionsPluginComponentRegistrar : CompilerPluginRegistrar() {
  override val pluginId: String = "software.ralf.app.platform.metro.compiler"
  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val compatContext =
      try {
        CompatContext.create()
      } catch (throwable: Throwable) {
        System.err.println(
          "[APP PLATFORM] Unable to load Kotlin compiler compatibility support; " +
            "skipping compiler extensions."
        )
        throwable.printStackTrace()
        return
      }

    with(compatContext) {
      registerFirExtensionCompat(AppPlatformMetroExtensionsPluginRegistrar())
      registerIrExtensionCompat(ContributesRendererIrExtension(compatContext))
      registerIrExtensionCompat(ContributesRobotIrExtension())
      registerIrExtensionCompat(ContributesScopedIrExtension())
    }
  }
}
