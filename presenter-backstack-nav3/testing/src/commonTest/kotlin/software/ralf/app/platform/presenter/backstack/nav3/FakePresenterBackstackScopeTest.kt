@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.Composable
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackScope.BackstackChange.Action
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

class FakePresenterBackstackScopeTest {
  @Test
  fun `push adds a presenter to the backstack`() {
    val rootPresenter = TestPresenter("root")
    val childPresenter = TestPresenter("child")
    val scope = FakePresenterBackstackScope(rootPresenter)

    scope.push(childPresenter)

    val changes = scope.recordedBackstackChanges.value
    assertThat(changes.map { it.action }).containsExactly(Action.PUSH, Action.PUSH)
    assertThat(changes.last().backstack).containsExactly(rootPresenter, childPresenter)
    assertThat(scope.lastBackstackChange.value).isSameInstanceAs(changes.last())
  }

  @Test
  fun `pop removes the top presenter from the backstack`() {
    val rootPresenter = TestPresenter("root")
    val childPresenter = TestPresenter("child")
    val scope = FakePresenterBackstackScope(rootPresenter)

    scope.push(childPresenter)

    scope.pop()

    val changes = scope.recordedBackstackChanges.value
    assertThat(changes.map { it.action }).containsExactly(Action.PUSH, Action.PUSH, Action.POP)
    assertThat(changes.last().backstack).containsExactly(rootPresenter)
    assertThat(scope.lastBackstackChange.value).isSameInstanceAs(changes.last())
  }

  @Test
  fun `pop ignores root pop without recording a change`() {
    val rootPresenter = TestPresenter("root")
    val scope = FakePresenterBackstackScope(rootPresenter)

    scope.pop()

    val changes = scope.recordedBackstackChanges.value
    assertThat(changes.map { it.action }).containsExactly(Action.PUSH)
    assertThat(changes.single().backstack).containsExactly(rootPresenter)
    assertThat(scope.lastBackstackChange.value).isSameInstanceAs(changes.single())
  }

  @Test
  fun `default root presenter keeps pushed presenter poppable`() {
    val presenter = TestPresenter("presenter")
    val scope = FakePresenterBackstackScope()
    val rootPresenter = scope.backstack.single()

    scope.push(presenter)
    scope.pop()

    val changes = scope.recordedBackstackChanges.value
    assertThat(changes.map { it.action }).containsExactly(Action.PUSH, Action.PUSH, Action.POP)
    assertThat(changes[1].backstack).containsExactly(rootPresenter, presenter)
    assertThat(changes.last().backstack).containsExactly(rootPresenter)
    assertThat(scope.lastBackstackChange.value).isSameInstanceAs(changes.last())
  }

  @Test
  fun `replaceTop replaces the top presenter`() {
    val rootPresenter = TestPresenter("root")
    val childPresenter = TestPresenter("child")
    val replacementPresenter = TestPresenter("replacement")
    val scope = FakePresenterBackstackScope(rootPresenter)

    scope.push(childPresenter)

    scope.replaceTop(replacementPresenter)

    val changes = scope.recordedBackstackChanges.value
    assertThat(changes.map { it.action }).containsExactly(Action.PUSH, Action.PUSH, Action.REPLACE)
    assertThat(changes.last().backstack).containsExactly(rootPresenter, replacementPresenter)
    assertThat(scope.lastBackstackChange.value).isSameInstanceAs(changes.last())
  }

  private class TestPresenter(private val id: String) : MoleculePresenter<Unit, BaseModel> {
    @Composable
    override fun present(input: Unit): BaseModel {
      return TestModel(id)
    }
  }

  private data class TestModel(val id: String) : BaseModel
}
