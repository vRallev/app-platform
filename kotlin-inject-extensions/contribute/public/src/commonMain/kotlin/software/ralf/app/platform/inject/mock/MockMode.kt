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
 * Annotation to produce a Boolean that signals if available mocked implementations should be used.
 *
 * Example of @MockMode being used to decide the implementation to use for ExampleService:
 * ```
 * @Provide
 * fun provideExampleService (
 *      realService: () -> RealExampleService,
 *      mockService: () -> FakeExampleService,
 *      @MockMode mockMode: Boolean,
 * ): ExampleService {
 *      return if (mockMode) mockService() else realService()
 * }
 * ```
 */
@Qualifier
@Retention(RUNTIME)
@Target(CLASS, FUNCTION, PROPERTY_GETTER, VALUE_PARAMETER, TYPE, PROPERTY)
public annotation class MockMode
