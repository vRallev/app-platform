package software.ralf.app.platform.scope

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * This function is similar to [runTest] and will additionally create a [Scope] using
 * [Scope.Companion.buildTestScope] for you. The [Scope] will be destroyed before the test finishes
 * and clean up all resources. A common pattern to test classes implementing [Scoped] looks like the
 * following
 *
 * ```
 * @Test
 * fun `test lifecycle`() = runTestWithScope { scope ->
 *    val myScoped = MyScoped()
 *
 *    // This calls myScoped.onEnterScope() for you.
 *    scope.register(myScoped)
 *
 *    ...
 *    // myScoped.onExitScope() will be called for you.
 * }
 * ```
 *
 * Similar to [Scope.Companion.buildTestScope], this function will use [StandardTestDispatcher] by
 * default, but this can be overridden using [context] parameter:
 * ```
 * runTestWithScope(UnconfinedTestDispatcher()) { ... }
 * ```
 */
public fun runTestWithScope(
  context: CoroutineContext = EmptyCoroutineContext,
  builder: (Scope.Builder.() -> Unit)? = null,
  testBody: suspend TestScope.(scope: Scope) -> Unit,
): TestResult {
  return runTest(context) {
    val rootScope =
      Scope.buildTestScope(
        testScope = this,
        name = "test-root-scope",
        context = backgroundScope.coroutineContext + context,
        builder = builder,
      )

    try {
      testBody.invoke(this, rootScope)
    } finally {
      rootScope.destroy()
    }
  }
}

/**
 * This function is similar to [runTest] and will additionally create a [Scope] using
 * [Scope.Companion.buildTestScope] for you. The [Scope] will be destroyed before the test finishes
 * and clean up all resources. The given [scoped] will be registered automatically in created
 * [Scope] and its [Scoped.onEnterScope] function will be called before the block [testBody] is
 * called. If this behavior is not desired, then you can use [runTestWithScope] and register your
 * [scoped] object manually.
 *
 * A common pattern to test classes implementing [Scoped] looks like the following:
 * ```
 * private lateinit var myScoped: MyScoped
 *
 * @Before
 * fun prepare() {
 *    myScoped = MyScoped()
 * }
 *
 * @Test
 * fun `test lifecycle`() = runTestWithScoped(myScoped) {
 *    // myScoped.onEnterScope() was already called at this point.
 *    ...
 *    // myScoped.onExitScope() will be called for you.
 * }
 * ```
 *
 * Similar to [Scope.Companion.buildTestScope], this function will use [StandardTestDispatcher] by
 * default, but this can be overridden using [context] parameter:
 * ```
 * runTestWithScoped(myScoped, UnconfinedTestDispatcher()) { ... }
 * ```
 */
public fun runTestWithScoped(
  scoped: Scoped,
  context: CoroutineContext = EmptyCoroutineContext,
  builder: (Scope.Builder.() -> Unit)? = null,
  testBody: suspend TestScope.(scope: Scope) -> Unit,
): TestResult =
  runTestWithScope(context = context, builder = builder) { scope ->
    scope.register(scoped)
    testBody(this, scope)
  }
