@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackScope.BackstackChange.Action
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.test

class PresenterBackstackScopeTest {
  @Test
  fun `presenterBackstack makes the scope available to content`() = runTest {
    val initialPresenter = FixedPresenter(InitialModel)

    createScopeSnapshotPresenter(initialPresenter).test(this) {
      val model = awaitItem()

      assertThat(model.scope).isSameInstanceAs(model.localScope)
      assertThat(model.backstack).isEqualTo(listOf(initialPresenter))
      assertThat(model.modelBackstack).isEqualTo(listOf(InitialModel))
      assertThat(model.action).isEqualTo(Action.PUSH)
    }
  }

  @Test
  fun `presenterBackstack pushes child presenters`() = runTest {
    val childPresenter = FixedPresenter(ChildModel)
    val initialPresenter = PushPresenter(childPresenter)

    createPresenterBackstackModelPresenter(initialPresenter).test(this) {
      val model = awaitItem()
      val initialModel = model.backstack.single() as PushModel

      initialModel.onPush()

      val updatedModel = awaitItem()
      assertThat(updatedModel.backstack.size).isEqualTo(2)
      assertThat(updatedModel.backstack.last()).isEqualTo(ChildModel)
    }
  }

  @Test
  fun `presenterBackstack pops child presenters`() = runTest {
    val childPresenter = FixedPresenter(ChildModel)
    val initialPresenter = PushPresenter(childPresenter)

    createPresenterBackstackModelPresenter(initialPresenter).test(this) {
      val model = awaitItem()
      val initialModel = model.backstack.single() as PushModel

      initialModel.onPush()
      val updatedModel = awaitItem()
      updatedModel.onBack()

      val poppedModel = awaitItem()
      assertThat(poppedModel.backstack.size).isEqualTo(1)
      assertThat(poppedModel.backstack.single()::class).isEqualTo(PushModel::class)
    }
  }

  @Test
  fun `presenterBackstack clears popped presenter saveable state`() = runTest {
    val childPresenter = SaveableStatefulPresenter()
    val initialPresenter = PushPresenter(childPresenter)

    createPresenterBackstackModelPresenter(initialPresenter).test(this) {
      val model = awaitItem()
      val initialModel = model.backstack.single() as PushModel

      initialModel.onPush()
      var childStack = awaitItem()
      val childModel = childStack.backstack.last() as SaveableStateModel

      childModel.onValueChanged("Changed")
      childStack = awaitItem()
      assertThat((childStack.backstack.last() as SaveableStateModel).value).isEqualTo("Changed")

      childStack.onBack()
      val poppedModel = awaitItem()
      (poppedModel.backstack.single() as PushModel).onPush()

      val repushedModel = awaitItem()
      assertThat((repushedModel.backstack.last() as SaveableStateModel).value).isEqualTo("Initial")
    }
  }

  @Test
  fun `presenterBackstack isolates state when the same presenter instance is pushed twice`() =
    runTest {
      val childPresenter = SaveableStatefulPresenter()
      val initialPresenter = PushPresenter(childPresenter)

      createPresenterBackstackModelPresenter(initialPresenter).test(this) {
        val model = awaitItem()
        val initialModel = model.backstack.single() as PushModel

        initialModel.onPush()
        val firstChildStack = awaitItem()
        (firstChildStack.backstack.last() as SaveableStateModel).onValueChanged("First")
        val changedFirstChildStack = awaitItem()
        (changedFirstChildStack.backstack.first() as PushModel).onPush()

        val secondChildStack = awaitItem()

        assertThat((secondChildStack.backstack[1] as SaveableStateModel).value).isEqualTo("First")
        assertThat((secondChildStack.backstack[2] as SaveableStateModel).value).isEqualTo("Initial")
      }
    }

  @Test
  fun `presenterBackstack ignores root pop`() = runTest {
    createPresenterBackstackModelPresenter(FixedPresenter(InitialModel)).test(this) {
      val model = awaitItem()

      model.onBack()

      expectNoEvents()
    }
  }

  private fun createScopeSnapshotPresenter(
    initialPresenter: ComposePresenter<Unit, out BaseModel>
  ): ComposePresenter<Unit, ScopeSnapshotModel> {
    return object : ComposePresenter<Unit, ScopeSnapshotModel> {
      @Composable
      override fun present(input: Unit): ScopeSnapshotModel {
        return presenterBackstack(initialPresenter) { modelBackstack ->
          ScopeSnapshotModel(
            scope = this,
            localScope = LocalBackstackScope.requireNotNull(),
            backstack = lastBackstackChange.value.backstack,
            modelBackstack = modelBackstack,
            action = lastBackstackChange.value.action,
          )
        }
      }
    }
  }

  private fun createPresenterBackstackModelPresenter(
    initialPresenter: ComposePresenter<Unit, out BaseModel>
  ): ComposePresenter<Unit, PresenterBackstackModel> {
    return object : ComposePresenter<Unit, PresenterBackstackModel> {
      @Composable
      override fun present(input: Unit): PresenterBackstackModel {
        return presenterBackstack(initialPresenter) { modelBackstack ->
          TestPresenterBackstackModel(backstack = modelBackstack, onBack = { pop() })
        }
      }
    }
  }

  private class FixedPresenter(private val model: BaseModel) : ComposePresenter<Unit, BaseModel> {
    @Composable override fun present(input: Unit): BaseModel = model
  }

  private class PushPresenter(private val childPresenter: ComposePresenter<Unit, out BaseModel>) :
    ComposePresenter<Unit, BaseModel> {
    @Composable
    override fun present(input: Unit): BaseModel {
      val backstackScope = LocalBackstackScope.requireNotNull()
      return PushModel(onPush = { backstackScope.push(childPresenter) })
    }
  }

  private class SaveableStatefulPresenter : ComposePresenter<Unit, BaseModel> {
    @Composable
    override fun present(input: Unit): BaseModel {
      var value by rememberSaveable { mutableStateOf("Initial") }
      return SaveableStateModel(value = value, onValueChanged = { value = it })
    }
  }

  private data class ScopeSnapshotModel(
    val scope: PresenterBackstackScope,
    val localScope: PresenterBackstackScope,
    val backstack: List<ComposePresenter<Unit, out BaseModel>>,
    val modelBackstack: List<BaseModel>,
    val action: Action,
  ) : BaseModel

  private data class PushModel(val onPush: () -> Unit) : BaseModel

  private data class SaveableStateModel(val value: String, val onValueChanged: (String) -> Unit) :
    BaseModel

  private data class TestPresenterBackstackModel(
    override val backstack: List<BaseModel>,
    override val onBack: () -> Unit,
  ) : PresenterBackstackModel

  private data object InitialModel : BaseModel

  private data object ChildModel : BaseModel
}
