@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackScope.BackstackChange
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackScope.BackstackChange.Action
import software.ralf.app.platform.presenter.compose.ComposePresenter

/**
 * Test fake for [PresenterBackstackScope].
 *
 * The fake starts with [rootPresenter] as the root backstack entry, matching [presenterBackstack],
 * which always starts with a non-empty stack. It records the root entry plus every stack-changing
 * [push], [pop], and [replaceTop] call in [recordedBackstackChanges]. Root pops are ignored,
 * matching the production [presenterBackstack] behavior.
 *
 * The default [rootPresenter] is a placeholder presenter. This is useful when the presenter under
 * test needs to call [pop]: create the fake with the default root, [push] the presenter under test,
 * then invoke the model callback. If the presenter under test is the root entry, [pop] is a no-op.
 *
 * [lastBackstackChange] is backed by Compose snapshot state so Compose presenters that read
 * `lastBackstackChange.value` recompose when the fake changes. The state uses referential equality
 * because every recorded mutation represents a new backstack change, even when the resulting
 * presenter list and action match the previous change.
 *
 * [withPresenterBackstackScope] only provides this fake as a composition local. It does not add the
 * receiver presenter to the fake backstack. If a test depends on the presenter under test being in
 * the stack, call [push] explicitly.
 *
 * ```kotlin
 * val scope = FakePresenterBackstackScope(rootPresenter)
 *
 * presenter.withPresenterBackstackScope(scope).test(this) {
 *   val model = awaitItem()
 *
 *   model.onContinue()
 *
 *   assertThat(scope.recordedBackstackChanges.value.last().backstack)
 *     .containsExactly(rootPresenter, signInPresenter)
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
public class FakePresenterBackstackScope(
  rootPresenter: ComposePresenter<Unit, out BaseModel> =
    object : ComposePresenter<Unit, BaseModel> {
      @Composable
      override fun present(input: Unit): BaseModel {
        return object : BaseModel {}
      }
    }
) : PresenterBackstackScope {
  private val _recordedBackstackChanges: MutableStateFlow<List<BackstackChange>> =
    MutableStateFlow(
      listOf(BackstackChangeImpl(backstack = listOf(rootPresenter), action = Action.PUSH))
    )

  private val _lastBackstackChange =
    mutableStateOf(_recordedBackstackChanges.value[0], referentialEqualityPolicy())

  override val lastBackstackChange: State<BackstackChange> = _lastBackstackChange

  /**
   * The initial backstack change plus every stack-changing mutation recorded by this fake.
   *
   * Mutate the fake through [push], [pop], or [replaceTop] so this history and
   * [lastBackstackChange] stay in sync.
   */
  public val recordedBackstackChanges: StateFlow<List<BackstackChange>> = _recordedBackstackChanges

  override fun push(presenter: ComposePresenter<Unit, out BaseModel>) {
    updateBackstack(backstack = backstack + presenter, action = Action.PUSH)
  }

  override fun pop() {
    if (backstack.size > 1) {
      updateBackstack(backstack = backstack.dropLast(1), action = Action.POP)
    }
  }

  override fun replaceTop(presenter: ComposePresenter<Unit, out BaseModel>) {
    updateBackstack(backstack = backstack.dropLast(1) + presenter, action = Action.REPLACE)
  }

  private fun updateBackstack(
    backstack: List<ComposePresenter<Unit, out BaseModel>>,
    action: Action,
  ) {
    val backstackChange = BackstackChangeImpl(backstack = backstack.toList(), action = action)
    _lastBackstackChange.value = backstackChange
    _recordedBackstackChanges.update { it + backstackChange }
  }

  private class BackstackChangeImpl(
    override val backstack: List<ComposePresenter<Unit, out BaseModel>>,
    override val action: Action,
  ) : BackstackChange
}
