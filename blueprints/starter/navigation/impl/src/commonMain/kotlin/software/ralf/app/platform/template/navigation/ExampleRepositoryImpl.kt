package software.ralf.app.platform.template.navigation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default implementation of [ExampleRepository] that holds an integer [StateFlow] and allows its
 * value to be updated.
 *
 * Useful for testing reactive state flow usage with presenters or other consumers.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ExampleRepositoryImpl : ExampleRepository {
  private val _exampleStateFlow = MutableStateFlow(0)
  override val exampleStateFlow: StateFlow<Int> = _exampleStateFlow.asStateFlow()

  override fun setExampleFlowValue(value: Int) {
    println("value: $value")
    _exampleStateFlow.value = value
  }
}
