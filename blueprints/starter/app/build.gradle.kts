@file:OptIn(ExperimentalWasmDsl::class)

import dev.zacsweers.metro.gradle.DiagnosticSeverity
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import software.ralf.app.platform.gradle.AppPlatformPlugin

plugins {
  alias(libs.plugins.appPlatform)
  alias(libs.plugins.androidKmpLibrary)
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

configure<MetroPluginExtension> {
  unusedGraphInputsSeverity.set(DiagnosticSeverity.NONE)
}

kotlin {
  jvm("desktop") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  androidLibrary {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  iosArm64()
  iosSimulatorArm64()

  targets.withType<KotlinNativeTarget>().configureEach {
    binaries.framework {
      baseName = "TemplateApp"
      AppPlatformPlugin.exportedDependencies().forEach { export(it) }
    }
  }

  wasmJs {
    outputModuleName = project.path.removePrefix(":").replace(":", "-")
    binaries.executable()

    browser {
      commonWebpackConfig {
        outputFileName = "template-app.js"
      }
    }
  }

  sourceSets {
    val desktopMain by getting
    commonMain {
      dependencies {
        implementation(project(":navigation:impl"))
        implementation(project(":templates:impl"))

        AppPlatformPlugin.exportedDependencies().forEach { api(it) }
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.activity.compose)
      }
    }

    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.coroutines.swing)
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
