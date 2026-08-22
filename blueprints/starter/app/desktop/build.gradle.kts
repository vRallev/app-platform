import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.appPlatform)
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

appPlatform {
  enableComposeUi(true)
  enableMetro(true)
  enableModuleStructure(true)
  enableMoleculePresenters(true)
  addImplModuleDependencies(true)
}

kotlin {
  jvm("desktop") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":app-framework:impl"))
      }
    }

    val desktopMain by getting {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.coroutines.swing)
      }
    }
  }
}

compose.desktop {
  application {
    mainClass = "software.ralf.app.platform.template.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "TemplateApp"
      packageVersion = "1.0.0"
    }
  }
}
