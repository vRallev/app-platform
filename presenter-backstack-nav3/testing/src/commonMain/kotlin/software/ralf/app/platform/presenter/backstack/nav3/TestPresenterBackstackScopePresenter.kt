@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocal
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/**
 * Wraps the receiver presenter with another presenter to provide a [PresenterBackstackScope] as
 * composition local.
 *
 * This is useful for unit tests of presenters that call `LocalBackstackScope.requireNotNull` and
 * expect to run inside [presenterBackstack]. This wrapper only provides the
 * [PresenterBackstackScope] composition local. It does not add the receiver presenter to the fake
 * backstack. By default, a new [FakePresenterBackstackScope] is created with a placeholder root.
 * Pass a [FakePresenterBackstackScope] when the test needs to assert calls to
 * [PresenterBackstackScope.push], [PresenterBackstackScope.pop], or
 * [PresenterBackstackScope.replaceTop].
 *
 * When asserting that the receiver presenter calls [PresenterBackstackScope.pop], pass a
 * [FakePresenterBackstackScope] created with its default placeholder root and push the receiver
 * presenter onto it before invoking the callback. A root presenter cannot be popped.
 *
 * ```kotlin
 * val scope = FakePresenterBackstackScope()
 *
 * presenter.withPresenterBackstackScope(scope).test(this) {
 *   val model = awaitItem()
 *
 *   model.onContinue()
 *
 *   assertThat(scope.recordedBackstackChanges.value.last().action).isEqualTo(Action.PUSH)
 * }
 * ```
 *
 * Example 2:
 * ```kotlin
 * val scope = FakePresenterBackstackScope()
 * scope.push(presenter)
 *
 * presenter.withPresenterBackstackScope(scope).test(this) {
 *   val model = awaitItem()
 *
 *   model.onBack()
 *
 *   assertThat(scope.recordedBackstackChanges.value.map { it.action })
 *     .containsExactly(Action.PUSH, Action.PUSH, Action.POP)
 * }
 * ```
 */
@ExperimentalAppPlatform
public fun <InputT : Any, ModelT : BaseModel> MoleculePresenter<InputT, ModelT>
  .withPresenterBackstackScope(
  scope: PresenterBackstackScope = FakePresenterBackstackScope()
): MoleculePresenter<InputT, ModelT> {
  val delegate = this
  return object : MoleculePresenter<InputT, ModelT> {
    @Composable
    override fun present(input: InputT): ModelT {
      return withCompositionLocal(LocalBackstackScope provides scope) {
        delegate.present(input)
      }
    }
  }
}
