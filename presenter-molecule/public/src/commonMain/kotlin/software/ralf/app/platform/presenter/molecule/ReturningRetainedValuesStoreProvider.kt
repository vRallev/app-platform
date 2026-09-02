package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel

/**
 * Composes [content] in the retained-values store associated with [key] and returns its model.
 *
 * State created with `remember`, `collectAsState`, or `produceState` normally lives only while its
 * call remains in the presenter composition. Use `retain` inside [content] for arbitrary in-memory
 * state that should return after a child presenter temporarily leaves the hierarchy. The parent
 * selecting the active child owns a `RetainedValuesStoreRegistry`, retained with
 * `retainRetainedValuesStoreRegistry`, and composes each child with a stable logical key:
 * ```
 * @OptIn(ExperimentalAppPlatform::class)
 * class AuthPresenter(
 *   private val createLoginPresenter: () -> LoginPresenter,
 *   private val createRegistrationPresenter: () -> RegistrationPresenter,
 * ) : MoleculePresenter<Destination, BaseModel> {
 *   @Composable
 *   override fun present(input: Destination): BaseModel {
 *     val retainedState = retainRetainedValuesStoreRegistry()
 *     return when (input) {
 *       Destination.Login ->
 *         retainedState.returningRetainedValuesStoreProvider("login") {
 *           val presenter = remember { createLoginPresenter() }
 *           presenter.present(Unit)
 *         }
 *       Destination.Registration ->
 *         retainedState.returningRetainedValuesStoreProvider("registration") {
 *           val presenter = remember { createRegistrationPresenter() }
 *           presenter.present(Unit)
 *         }
 *     }
 *   }
 * }
 *
 * class LoginPresenter : MoleculePresenter<Unit, LoginPresenter.Model> {
 *   @Composable
 *   override fun present(input: Unit): Model {
 *     var email by retain { mutableStateOf("") }
 *     return Model(email = email, onEmailChanged = { email = it })
 *   }
 *
 *   data class Model(
 *     val email: String,
 *     val onEmailChanged: (String) -> Unit,
 *   ) : BaseModel
 * }
 * ```
 *
 * Use a key with stable equality and hash-code behavior that identifies the logical child. Do not
 * compose the same key in two active providers. Call [RetainedValuesStoreRegistry.clearChild] when
 * an inactive child's state is permanently obsolete, or [RetainedValuesStoreRegistry.clearChildren]
 * when pruning several keys.
 *
 * Retained values stay in memory, may hold values that are not saveable, and do not survive
 * recreation of the root presenter or process death. Put state that must survive those events in an
 * injected state owner or repository. For state shared across siblings or deeply nested presenters,
 * prefer an injected owner over threading it through every level.
 */
@ExperimentalAppPlatform
@Composable
public fun <ModelT : BaseModel> RetainedValuesStoreRegistry.returningRetainedValuesStoreProvider(
  key: Any?,
  content: @Composable () -> ModelT,
): ModelT {
  var model: ModelT? = null
  LocalRetainedValuesStoreProvider(key) { model = content() }
  return checkNotNull(model)
}
