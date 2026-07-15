package software.ralf.app.platform.robot.internal

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope

class RootScopeTest {

  @BeforeTest
  fun before() {
    rootScopeProvider = null
  }

  @AfterTest
  fun after() {
    rootScopeProvider = null
  }

  @Test
  fun `if no root scope can be found then an error is thrown`() {
    val throwable = assertFailsWith<Throwable> { rootScope }

    if (throwable.stackTraceToString().contains("No instrumentation registered!")) {
      // This is for Android, but we're not running on a real Android device, so stop
      // the test.
      return
    }

    assertThat(throwable.message.toString())
      .contains(
        "The root scope could not be found. Consider overriding the " +
          "RootScopeProvider through RobotInternals."
      )
  }

  @Test
  fun `the root scope provider can be changed`() {
    rootScopeProvider =
      object : RootScopeProvider {
        override val rootScope: Scope = Scope.buildRootScope("Test Scope")
      }

    assertThat(rootScope.name).isEqualTo("Test Scope")
  }
}
