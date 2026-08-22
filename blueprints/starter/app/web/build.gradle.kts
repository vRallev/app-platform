@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
    commonMain {
      dependencies {
        implementation(project(":app-framework:impl"))
      }
    }
  }
}
