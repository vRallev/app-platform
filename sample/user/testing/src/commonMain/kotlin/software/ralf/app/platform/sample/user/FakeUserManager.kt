package software.ralf.app.platform.sample.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.buildTestScope
import software.ralf.app.platform.scope.di.metro.addMetroDependencyGraph

/**
 * Fake implementation of [UserManager], which is useful in unit tests.
 *
 * This class is part of the `:testing` module and shared with other modules.
 */
class FakeUserManager(override val user: MutableStateFlow<User?> = MutableStateFlow(null)) :
  UserManager {

  override fun login(userId: Long) {
    user.value = FakeUser(userId = userId)
  }

  /** Overloaded function to change the coroutine scope and Metro graph for the [FakeUser]. */
  fun login(userId: Long, scope: TestScope, graph: Any) {
    user.value =
      FakeUser(
        userId = userId,
        scope = Scope.buildTestScope(scope) { addMetroDependencyGraph(graph) },
      )
  }

  override fun logout() {
    user.value?.scope?.destroy()
    user.value = null
  }
}
