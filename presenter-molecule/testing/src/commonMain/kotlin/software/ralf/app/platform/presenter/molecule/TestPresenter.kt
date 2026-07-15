@file:Suppress("RedundantSuppression", "RedundantSuspendModifier")

package software.ralf.app.platform.presenter.molecule

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import software.ralf.app.platform.presenter.BaseModel

/**
 * Assert the emitted models of the given [MoleculePresenter] in the provided [validate] lambda.
 *
 * The used [CoroutineContext] for the presenter can be changed with [coroutineContext]. By default,
 * a [UnconfinedTestDispatcher] will be used, which means coroutines aren't confined to any thread
 * and usually execute immediately. This avoids unnecessary calls like `advanceUntilIdle`.
 *
 * This function can be used inside a [TestScope], e.g.
 *
 * ```
 * @Test fun myTest() = runTest {
 *     MyPresenter(input).test {
 *         // assert models
 *     }
 * }
 * ```
 */
public suspend fun <InputT : Any, ModelT : BaseModel> MoleculePresenter<InputT, ModelT>.test(
  testScope: TestScope,
  input: InputT,
  coroutineContext: CoroutineContext = EmptyCoroutineContext,
  timeout: Duration? = null,
  validate: suspend ReceiveTurbine<ModelT>.() -> Unit,
) {
  testScope
    .moleculeScope(coroutineContext)
    .launchMoleculePresenter(this, input)
    .model
    .test(validate = validate, timeout = timeout)
}

/**
 * Assert the emitted models of the given [MoleculePresenter] in the provided [validate] lambda.
 *
 * The used [CoroutineContext] for the presenter can be changed with [coroutineContext]. By default,
 * a [UnconfinedTestDispatcher] will be used, which means coroutines aren't confined to any thread
 * and usually execute immediately. This avoids unnecessary calls like `advanceUntilIdle`.
 *
 * This function can be used inside of a [TestScope], e.g.
 *
 * ```
 * @Test fun myTest() = runTest {
 *     MyPresenter(input).test {
 *         // assert models
 *     }
 * }
 * ```
 */
public suspend fun <InputT : Any, ModelT : BaseModel> MoleculePresenter<InputT, ModelT>.test(
  testScope: TestScope,
  input: StateFlow<InputT>,
  coroutineContext: CoroutineContext = EmptyCoroutineContext,
  validate: suspend ReceiveTurbine<ModelT>.() -> Unit,
) {
  testScope
    .moleculeScope(coroutineContext)
    .launchMoleculePresenter(this, input)
    .model
    .test(validate = validate)
}

/**
 * Assert the emitted models of the given [MoleculePresenter] in the provided [validate] lambda.
 *
 * The used [CoroutineContext] for the presenter can be changed with [coroutineContext]. By default,
 * a [UnconfinedTestDispatcher] will be used, which means coroutines aren't confined to any thread
 * and usually execute immediately. This avoids unnecessary calls like `advanceUntilIdle`.
 *
 * This function can be used inside of a [TestScope], e.g.
 *
 * ```
 * @Test fun myTest() = runTest {
 *     MyPresenter().test {
 *         // assert models
 *     }
 * }
 * ```
 */
public suspend fun <ModelT : BaseModel> MoleculePresenter<Unit, ModelT>.test(
  testScope: TestScope,
  coroutineContext: CoroutineContext = EmptyCoroutineContext,
  timeout: Duration? = null,
  validate: suspend ReceiveTurbine<ModelT>.() -> Unit,
) {
  test(testScope, Unit, coroutineContext, timeout, validate)
}
