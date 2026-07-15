package software.ralf.app.platform.presenter

import dev.zacsweers.metro.Qualifier as MetroQualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import me.tatarka.inject.annotations.Qualifier as KiQualifier
import software.ralf.app.platform.scope.coroutine.MainCoroutineDispatcher

/**
 * A qualifier to identify the coroutine scope used to run presenters. This scope is commonly
 * injected when converting a `Flow` to a `StateFlow`, see `stateInPresenter` for more details.
 *
 * This scope uses the [MainCoroutineDispatcher] by default, because presenters produce state for
 * the UI and computing their models should have the highest priority.
 *
 * Never cancel this scope yourself, otherwise the application comes to a halt.
 */
@KiQualifier
@MetroQualifier
@Retention(RUNTIME)
@Target(CLASS, FUNCTION, PROPERTY_GETTER, VALUE_PARAMETER, TYPE, PROPERTY)
public annotation class PresenterCoroutineScope
