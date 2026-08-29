package software.ralf.circuit.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

public open class AppAndroidPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(Plugins.ANDROID_APP)
    target.plugins.apply(BaseAndroidPlugin::class.java)
    target.configureKotlin()
    target.configureDetekt()
  }

  private fun Project.configureKotlin() {
    extensions.getByType(KotlinAndroidProjectExtension::class.java).compilerOptions {
      extraWarnings.set(false)
      allWarningsAsErrors.set(ci)
      jvmTarget.set(this@configureKotlin.jvmTarget)
    }
  }
}
