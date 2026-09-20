package software.ralf.app.platform.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import software.ralf.app.platform.gradle.AppPlatformExtension

public open class RootPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(Plugins.APP_PLATFORM)
    target.extensions
      .getByType(AppPlatformExtension::class.java)
      .enableModuleStructureNestingCheck(true)
    target.releaseTask.configure { it.dependsOn("checkModuleStructureNesting") }
  }
}
