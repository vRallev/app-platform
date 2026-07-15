package software.ralf.app.platform.inject.mock

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import me.tatarka.inject.annotations.Qualifier

/**
 * A qualifier that is used when generating a binds method using [ContributesRealImpl] to denote the
 * realImpl of an interface.
 *
 * This annotation should not be used directly and only used within ContributesRealImplGenerator and
 * ContributesMockImplGenerator.
 */
@Qualifier
@Retention(RUNTIME)
@Target(CLASS, FUNCTION, PROPERTY_GETTER, VALUE_PARAMETER, TYPE, PROPERTY)
public annotation class RealImpl
