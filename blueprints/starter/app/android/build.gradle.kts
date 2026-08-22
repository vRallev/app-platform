import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.appPlatform)
  alias(libs.plugins.androidApplication)
}

appPlatform {
  enableMetro(true)
  enableModuleStructure(true)
  addImplModuleDependencies(true)
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

android {
  namespace = "software.ralf.app.platform.template"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "software.ralf.app.platform.template"
    versionCode = 1
    versionName = "1.0"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
}

dependencies {
  implementation(project(":app-framework:impl"))
}
