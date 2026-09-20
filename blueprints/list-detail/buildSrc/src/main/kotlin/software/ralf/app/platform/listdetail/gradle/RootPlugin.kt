package software.ralf.app.platform.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import software.ralf.app.platform.gradle.AppPlatformExtension

public open class RootPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.configureAppPlatform()
  }

  private fun Project.configureAppPlatform() {
    plugins.apply(Plugins.APP_PLATFORM)
    extensions.getByType(AppPlatformExtension::class.java).enableModuleStructureNestingCheck(true)
    releaseTask.configure { it.dependsOn("checkModuleStructureNesting") }
  }
}
