package software.ralf.app.platform.gradle.buildsrc

internal object Plugins {
  const val ANDROID_APP = "com.android.application"
  const val ANDROID_LIBRARY = "com.android.library"
  const val APP_PLATFORM = "software.ralf.app.platform"
  const val BINARY_COMPAT_VALIDATOR = "org.jetbrains.kotlinx.binary-compatibility-validator"
  const val COMPOSE_COMPILER = "org.jetbrains.kotlin.plugin.compose"
  const val COMPOSE_MULTIPLATFORM = "org.jetbrains.compose"
  const val DETEKT = "dev.detekt"
  const val KOTLIN_MULTIPLATFORM = "org.jetbrains.kotlin.multiplatform"
  const val KOTLIN_HIERARCHY = "io.github.terrakok.kmp-hierarchy"
  const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
  const val KSP = "com.google.devtools.ksp"
  const val MAVEN_PUBLISH = "com.vanniktech.maven.publish"
  const val METRO = "dev.zacsweers.metro"
}
