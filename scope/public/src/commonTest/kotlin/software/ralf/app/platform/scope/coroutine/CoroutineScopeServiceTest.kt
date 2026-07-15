package software.ralf.app.platform.scope.coroutine

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import software.ralf.app.platform.scope.Scope

class CoroutineScopeServiceTest {

  @Test
  fun `a coroutine scope can be registered in a scope`() {
    val coroutineScope = CoroutineScopeScoped(Job() + CoroutineName("abc"))

    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }

    assertThat(scope.coroutineScope().coroutineContext[CoroutineName]?.name).isEqualTo("abc-child")
  }

  @Test
  fun `a coroutine scope is canceled when the Scope is destroyed`() {
    val coroutineScope = CoroutineScopeScoped(Job() + CoroutineName("abc"))

    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }

    val childCoroutineScope = scope.coroutineScope()

    scope.destroy()
    assertThat(coroutineScope.isActive).isFalse()
    assertThat(childCoroutineScope.isActive).isFalse()
  }
}
