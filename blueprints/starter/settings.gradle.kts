pluginManagement {
  repositories {
    google {
      content {
        includeGroupAndSubgroups("androidx")
        includeGroupAndSubgroups("com.android")
        includeGroup("com.google.testing.platform")
      }
    }
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositories {
    google {
      content {
        includeGroupAndSubgroups("androidx")
        includeGroupAndSubgroups("com.android")
        includeGroup("com.google.testing.platform")
      }
    }
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
