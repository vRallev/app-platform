package software.ralf.app.platform.gradle.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project

public open class LibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)

    target.plugins.apply(Plugins.ANDROID_LIBRARY)
    target.plugins.apply(KmpPlugin::class.java)
    target.plugins.apply(BaseAndroidPlugin::class.java)
  }
}
