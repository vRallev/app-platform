@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.Composable
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import assertk.assertions.messageContains
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackScope.BackstackChange.Action
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.test

class TestPresenterBackstackScopePresenterTest {
  @Test
  fun `a presenter cannot be tested without the backstack scope wrapper`() = runTest {
    val presenter =
      object : MoleculePresenter<Unit, BaseModel> {
        @Composable
        override fun present(input: Unit): BaseModel {
          LocalBackstackScope.requireNotNull()
          return object : BaseModel {}
        }
      }

    assertFailure { presenter.test(this) {} }
      .messageContains("Couldn't find the PresenterBackstackScope in the presenter hierarchy.")
  }

  @Test
  fun `a presenter can be tested with the backstack scope wrapper`() = runTest {
    data class Model(val scope: PresenterBackstackScope) : BaseModel

    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          return Model(LocalBackstackScope.requireNotNull())
        }
      }
    val scope = FakePresenterBackstackScope(rootPresenter = presenter)

    presenter.withPresenterBackstackScope(scope).test(this) {
      assertThat(awaitItem().scope).isSameInstanceAs(scope)
    }
  }

  @Test
  fun `the default fake does not add the receiver presenter to the backstack`() = runTest {
    data class Model(val backstack: List<MoleculePresenter<Unit, out BaseModel>>) : BaseModel

    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          return Model(LocalBackstackScope.requireNotNull().backstack)
        }
      }

    presenter.withPresenterBackstackScope().test(this) {
      val backstack = awaitItem().backstack

      assertThat(backstack.size).isEqualTo(1)
      assertThat(backstack.single()).isNotSameInstanceAs(presenter)
    }
  }

  @Test
  fun `a provided fake with default root can test receiver presenter pop`() = runTest {
    data class Model(val onBack: () -> Unit) : BaseModel

    val scope = FakePresenterBackstackScope()
    val rootPresenter = scope.backstack.single()
    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          val backstackScope = LocalBackstackScope.requireNotNull()
          return Model(onBack = { backstackScope.pop() })
        }
      }
    scope.push(presenter)

    presenter.withPresenterBackstackScope(scope).test(this) {
      val model = awaitItem()

      model.onBack()

      val changes = scope.recordedBackstackChanges.value
      assertThat(changes.map { it.action }).containsExactly(Action.PUSH, Action.PUSH, Action.POP)
      assertThat(changes[1].backstack).containsExactly(rootPresenter, presenter)
      assertThat(changes[2].backstack).containsExactly(rootPresenter)
    }
  }

  @Test
  fun `a provided fake records backstack interactions`() = runTest {
    data class Model(val onPush: () -> Unit, val onBack: () -> Unit) : BaseModel

    val rootPresenter = TestPresenter("root")
    val childPresenter = TestPresenter("child")
    val scope = FakePresenterBackstackScope(rootPresenter)
    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          val backstackScope = LocalBackstackScope.requireNotNull()
          return Model(
            onPush = { backstackScope.push(childPresenter) },
            onBack = { backstackScope.pop() },
          )
        }
      }

    presenter.withPresenterBackstackScope(scope).test(this) {
      val model = awaitItem()

      model.onPush()
      model.onBack()

      val changes = scope.recordedBackstackChanges.value
      assertThat(changes.map { it.action }).containsExactly(Action.PUSH, Action.PUSH, Action.POP)
      assertThat(changes[1].backstack).containsExactly(rootPresenter, childPresenter)
      assertThat(changes[2].backstack).containsExactly(rootPresenter)
    }
  }

  @Test
  fun `a presenter recomposes when fake backstack state changes`() = runTest {
    data class Model(val backstackSize: Int) : BaseModel

    val rootPresenter = TestPresenter("root")
    val childPresenter = TestPresenter("child")
    val scope = FakePresenterBackstackScope(rootPresenter)
    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          val backstackScope = LocalBackstackScope.requireNotNull()
          return Model(backstackScope.lastBackstackChange.value.backstack.size)
        }
      }

    presenter.withPresenterBackstackScope(scope).test(this) {
      assertThat(awaitItem().backstackSize).isEqualTo(1)

      scope.push(childPresenter)

      assertThat(awaitItem().backstackSize).isEqualTo(2)
    }
  }

  @Test
  fun `a presenter recomposes when fake records an equivalent backstack change`() = runTest {
    data class Model(val change: PresenterBackstackScope.BackstackChange) : BaseModel

    val rootPresenter = TestPresenter("root")
    val scope = FakePresenterBackstackScope(rootPresenter)
    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          val backstackScope = LocalBackstackScope.requireNotNull()
          return Model(backstackScope.lastBackstackChange.value)
        }
      }

    presenter.withPresenterBackstackScope(scope).test(this) {
      assertThat(awaitItem().change.action).isEqualTo(Action.PUSH)

      scope.replaceTop(rootPresenter)
      assertThat(awaitItem().change.action).isEqualTo(Action.REPLACE)

      scope.replaceTop(rootPresenter)
      assertThat(awaitItem().change.action).isEqualTo(Action.REPLACE)

      assertThat(scope.recordedBackstackChanges.value.map { it.action })
        .containsExactly(Action.PUSH, Action.REPLACE, Action.REPLACE)
    }
  }

  private class TestPresenter(private val id: String) : MoleculePresenter<Unit, BaseModel> {
    @Composable
    override fun present(input: Unit): BaseModel {
      return TestModel(id)
    }
  }

  private data class TestModel(val id: String) : BaseModel
}
