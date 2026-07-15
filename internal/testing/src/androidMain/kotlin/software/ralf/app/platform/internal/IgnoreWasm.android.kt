package software.ralf.app.platform.internal

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/** Skips annotated tests on Wasm. */
@Target(CLASS, FUNCTION) actual annotation class IgnoreWasm actual constructor()
