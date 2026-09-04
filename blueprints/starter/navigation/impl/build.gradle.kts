@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.appPlatform)
  alias(libs.plugins.androidKmpLibrary)
  alias(libs.plugins.kotlinMultiplatform)
}

appPlatform {
  enableComposeUi(true)
  enableModuleStructure(true)
  enableMetro(true)
  enableMoleculePresenters(true)
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

  wasmJs {
    outputModuleName = project.path.removePrefix(":").replace(":", "-")
    binaries.executable()

    browser()
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.compose.material)
        implementation(libs.compose.material.icons)
        implementation(project(":templates:public"))
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.assertk)
        implementation(libs.coroutines.test)
        implementation(project(":navigation:testing"))
      }
    }
  }
}
