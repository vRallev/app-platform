package software.ralf.circuit.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

public open class AppDesktopPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(KmpPlugin::class.java)
    target.plugins.apply(Plugins.COMPOSE_HOT_RELOAD)

    target.plugins.withId(Plugins.COMPOSE_MULTIPLATFORM) { target.configureDesktopApplication() }
  }

  private fun Project.configureDesktopApplication() {
    val composeExtension = extensions.getByType(ComposeExtension::class.java)
    composeExtension.extensions.getByType(DesktopExtension::class.java).application.apply {
      mainClass = "software.ralf.circuit.listdetail.MainKt"

      nativeDistributions.apply {
        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        packageName = "ListDetailCircuit"
        packageVersion = versionName
        macOS.iconFile.set(project.file("src/desktopMain/resources/app-icon.icns"))
        windows.iconFile.set(project.file("src/desktopMain/resources/app-icon.ico"))
        linux.iconFile.set(project.file("src/desktopMain/resources/app-icon.png"))
      }
    }
  }
}
