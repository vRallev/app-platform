package software.ralf.app.platform.gradle.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import software.ralf.app.platform.gradle.buildsrc.AppPlatformExtension.Companion.appPlatformGradlePlugin

public open class RootPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(Plugins.APP_PLATFORM)
    target.appPlatformGradlePlugin.enableModuleStructureNestingCheck(true)
  }
}
