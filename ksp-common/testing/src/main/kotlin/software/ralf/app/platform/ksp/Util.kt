@file:JvmName("UtilUnitTest")
@file:OptIn(ExperimentalCompilerApi::class)

package software.ralf.app.platform.ksp

import assertk.Assert
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import java.lang.reflect.AnnotatedElement
import kotlin.reflect.KClass
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

fun Assert<AnnotatedElement>.isAnnotatedWith(annotation: KClass<*>) {
  transform { element -> element.annotations.map { it.annotationClass } }.contains(annotation)
}

fun Assert<ExitCode>.isOk() {
  isEqualTo(ExitCode.OK)
}

fun Assert<ExitCode>.isError() {
  transform { element ->
      when (element) {
        ExitCode.OK -> element
        ExitCode.INTERNAL_ERROR,
        ExitCode.COMPILATION_ERROR,
        ExitCode.SCRIPT_EXECUTION_ERROR -> ExitCode.COMPILATION_ERROR
      }
    }
    .isEqualTo(ExitCode.COMPILATION_ERROR)
}

fun Assert<AnnotatedElement>.isNotAnnotatedWith(annotation: KClass<*>) {
  transform { element -> element.annotations.map { it.annotationClass } }.doesNotContain(annotation)
}
