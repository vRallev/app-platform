package software.ralf.app.platform.template.navigation

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface of an example repository to show how to correctly contribute, inject, and use within
 * presenters.
 */
interface ExampleRepository {
  val exampleStateFlow: StateFlow<Int>

  fun setExampleFlowValue(value: Int)
}
