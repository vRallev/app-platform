package software.ralf.app.platform.gradle.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project

public open class LibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(KmpAndroidPlugin::class.java)
    target.plugins.apply(KmpPlugin::class.java)
  }
}
