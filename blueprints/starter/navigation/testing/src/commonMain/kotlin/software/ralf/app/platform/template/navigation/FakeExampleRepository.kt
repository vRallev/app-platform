package software.ralf.app.platform.template.navigation

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [ExampleRepository], which is useful in unit tests.
 *
 * This class is part of the `:testing` module and shared with other modules.
 */
class FakeExampleRepository(
  override val exampleStateFlow: MutableStateFlow<Int> = MutableStateFlow(0)
) : ExampleRepository {

  override fun setExampleFlowValue(value: Int) {
    exampleStateFlow.value = value
  }
}
