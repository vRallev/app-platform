package software.ralf.app.platform.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public open class LibraryKmpPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(KmpPlugin::class.java)
  }
}
