pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Template"

include(":app:android")
include(":app:desktop")
include(":app:web")
include(":app-framework:impl")
include(":navigation:impl")
include(":navigation:public")
include(":navigation:testing")
include(":templates:impl")
include(":templates:public")
