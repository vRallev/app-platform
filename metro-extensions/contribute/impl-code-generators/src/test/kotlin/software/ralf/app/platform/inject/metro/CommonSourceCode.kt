@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.inject.metro

import com.tschuchort.compiletesting.JvmCompilationResult
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

internal val JvmCompilationResult.graphInterface: Class<*>
  get() = classLoader.loadClass("software.ralf.test.GraphInterface")

internal fun <T : Any> Class<*>.newMetroGraph(): T {
  val companionObject = fields.single().get(null)
  @Suppress("UNCHECKED_CAST")
  return classes
    .single { it.simpleName == "Companion" }
    .declaredMethods
    .single { it.name == "create" }
    .invoke(companionObject) as T
}
