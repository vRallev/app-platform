package software.ralf.circuit.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public open class AppWebPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(KmpPlugin::class.java)
  }
}
