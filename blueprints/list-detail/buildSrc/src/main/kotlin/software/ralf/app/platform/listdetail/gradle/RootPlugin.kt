package software.ralf.app.platform.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public open class RootPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
  }
}
