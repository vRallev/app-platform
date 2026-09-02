# Presenter

!!! note

    While App Platform has a generic `Presenter` interface to remove coupling, we strongly recommend using
    `MoleculePresenter` for implementations. `MoleculePresenters` are an opt-in feature through the Gradle DSL.
    The default value is `false`.

    ```groovy
    appPlatform {
      enableMoleculePresenters true
    }
    ```

## Unidirectional dataflow

App Platform implements the unidirectional dataflow pattern to decouple business logic from UI rendering. Not only
does this allow for better testing of business logic and provides clear boundaries, but individual apps can also
share more code and change the look and feel when needed.

## `MoleculePresenter`

In the unidirectional dataflow pattern events and state only travel into one direction through a single stream.
State is produced by `Presenters` and can be observed through a reactive stream:

```kotlin
interface Presenter<ModelT : BaseModel> {
  val model: StateFlow<ModelT>
}
```

`Presenters` can be implemented in many ways as long as they can be converted to this interface. App Platform
provides and recommends the implementation using [Molecule](https://github.com/cashapp/molecule) since it provides
many advantages. Molecule is a library that turns a `@Composable` function into a `StateFlow`. It leverages the
core of [Compose](https://developer.android.com/compose) without bringing in Compose UI as dependency. The primary
use case of Compose is handling, creating and modifying tree-like data structures, which is a natural fit for
UI frameworks. Molecule reuses Compose to handle state management and state transitions to implement business
logic in the form of `@Composable` functions with all the benefits that Compose provides.

The [MoleculePresenter](https://github.com/vRallev/app-platform/blob/main/presenter-molecule/public/src/commonMain/kotlin/software/ralf/app/platform/presenter/molecule/MoleculePresenter.kt)
interface looks like this:

```kotlin
fun interface MoleculePresenter<InputT : Any, ModelT : BaseModel> {
  @Composable
  fun present(input: InputT): ModelT
}
```

[`Models`](https://github.com/vRallev/app-platform/blob/main/presenter/public/src/commonMain/kotlin/software/ralf/app/platform/presenter/BaseModel.kt)
represent the state of a `Presenter`. Usually, they’re implemented as immutable, inner data classes of the `Presenter`.
Using sealed hierarchies is a good practice to allow to differentiate between different states:

```kotlin
interface LoginPresenter : MoleculePresenter<Model> {
  sealed interface Model : BaseModel {
    data object LoggedOut : Model

    data class LoggedIn(
      val user: User,
    ) : Model
  }
}
```

Notice that it’s recommended even for `Presenters` to follow the dependency inversion principle. `LoginPresenter` is
an interface and there can be multiple implementations.

??? example "Sample"

    The sample application follows the same principle of dependency inversion. E.g. the API of the
    [`LoginPresenter`](https://github.com/vRallev/app-platform/blob/main/sample/login/public/src/commonMain/kotlin/software/ralf/app/platform/sample/login/LoginPresenter.kt)
    is part of the `:public` module, while the implementation [`LoginPresenterImpl`](https://github.com/vRallev/app-platform/blob/main/sample/login/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/login/LoginPresenterImpl.kt)
    lives in the `:impl` module. This abstraction is used in tests, where [`FakeLoginPresenter`](https://github.com/vRallev/app-platform/blob/main/sample/navigation/impl/src/commonTest/kotlin/software/ralf/app/platform/sample/navigation/NavigationPresenterImplTest.kt#L45-L49)
    simplifies the test setup of classes relying on `LoginPresenter`.

Observers of the state of a `Presenter`, such as the UI layer, communicate back to the `Presenter` through events.
Events are sent through a lambda in the `Model`, which the `Presenter` must provide:

```kotlin hl_lines="16"
interface LoginPresenter : MoleculePresenter<Unit, Model> {

  sealed interface Event {
    data object Logout : Event

    data class ChangeName(
      val newName: String,
    ) : Event
  }

  sealed interface Model : BaseModel {
    data object LoggedOut : Model

    data class LoggedIn(
      val user: User,
      val onEvent: (Event) -> Unit,
    ) : Model
  }
}
```

A concrete implementation of `LoginPresenter` could look like this:

```kotlin
@ContributesBinding(AppScope::class)
class LoginPresenterImpl : LoginPresenter {
  @Composable
  fun present(input: Unit): Model {
    ..
    return if (user != null) {
      LoggedIn(user = user) { event ->
        when (event) {
          is Logout -> ..
          is ChangeName -> ..
        }
      }
    } else {
      LoggedOut
    }
  }
}
```

!!! note

    `MoleculePresenters` are never singletons. They are automatically bound to an API using
    `@ContributesBinding`, but they don't use the `@SingleIn` annotation. Metro can instantiate a
    contributed presenter with a single constructor without `@Inject`; `kotlin-inject-anvil` users
    should still use `@Inject` for constructor injection. `MoleculePresenters` manage their state in
    the `@Composable` function with the Compose runtime. Therefore, it's strongly discouraged to have
    any class properties.

## Model driven navigation

`Presenters` are composable, meaning that one presenter could combine N other presenters into a single stream of
model objects. With that concept in mind we can decompose large presenters into multiple smaller ones. Not only
do they become easier to change, maintain and test, but we can also share and reuse presenters between multiple
screens if needed. Presenters form a tree with nested presenters. They’re unaware of their parent and communicate
upwards only through their `Model`.

```kotlin hl_lines="14 17"
class OnboardingPresenterImpl(
  // Make presenters lazy to only instantiate them when they're actually needed.
  private val lazyLoginPresenter: () -> LoginPresenter,
  private val lazyRegistrationPresenter: () -> RegistrationPresenter,
) : OnboardingPresenter {

  @Composable
  fun present(input: Unit): BaseModel {
    ...
    return if (mustRegister) {
      // Remember the presenter to avoid creating a new one during each
      // composition (in other words when computing a new model).
      val registrationPresenter = remember { lazyRegistrationPresenter() }
      registrationPresenter.present(Unit)
    } else {
      val loginPresenter = remember { lazyLoginPresenter() }
      loginPresenter.present(Unit)
    }
  }
}
```

Notice how the parent presenter calls the `@Composable` `present()` function from the child presenters like
a regular function to compute their model and return it.

??? example "Sample"

    [`NavigationPresenterImpl`](https://github.com/vRallev/app-platform/blob/main/sample/navigation/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/navigation/NavigationPresenterImpl.kt)
    is another example that highlights this principle.

    [`UserPagePresenterImpl`](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/user/UserPagePresenterImpl.kt)
    goes a step further. Its `BaseModel` is composed of two sub-models. The `listModel` is even an input for the
    detail-presenter.

    ```kotlin
    val listModel = userPageListPresenter.present(UserPageListPresenter.Input(user))
    return Model(
      listModel = listModel,
      detailModel =
        userPageDetailPresenter.present(
          UserPageDetailPresenter.Input(user, selectedAttribute = listModel.selectedIndex)
        ),
    )
    ```

This concept allows us to implement model-driven navigation. By driving the entire UI layer through `Presenters` and
emitted `Models` navigation becomes a first class API and testable. Imagine having a root presenter implementing a
back stack that forwards the model of the top most presenter. When the user navigates to a new screen, then the
root presenter would add a new presenter to the stack and provide its model object.

<div class="d2-diagram d2-diagram--standard">
  <img class="d2-diagram__light" src="../images/presenter/model-driven-navigation-light.svg" alt="Model-driven navigation presenter hierarchy">
  <img class="d2-diagram__dark" src="../images/presenter/model-driven-navigation-dark.svg" alt="Model-driven navigation presenter hierarchy">
</div>

In the example above, the root presenter would forward the model of the onboarding, delivery or settings presenter
to the UI layer. The onboarding presenter as shown in the code example can either call the login or registration
presenter based on a condition. With Molecule calling a child presenter is as easy as invoking a function.

## Parent child communication

While the pattern isn’t used frequently, parent presenters can provide input to their child presenters. The
returned model from the child presenter can be used further to change the control flow.

```kotlin
interface ChildPresenter : MoleculePresenter<Input, Model> {
  data class Input(
    val argument: String,
  )
}

class ParentPresenterImpl(
  private val lazyChildPresenter: () -> ChildPresenter
) : ParentPresenter {

  @Composable
  fun present(input: Unit) {
    val childPresenter = remember { lazyChildPresenter() }
    val childModel = childPresenter.present(Input(argument = "abc"))

    return if (childModel...) ...
  }
}
```

This mechanism is favored less, because it only allows for direct parent to child presenter interactions and
becomes hard to manage for deeply nested hierarchies. More often a service object is injected instead, which
is used by the multiple presenters:

```kotlin hl_lines="8 12 20 25 28"
interface AccountManager {
  val currentAccount: StateFlow<Account>

  fun mustRegister(): Boolean
}

class LoginPresenterImpl(
  private val accountManager: AccountManager
): LoginPresenter {
  @Composable
  fun present(input: Unit): Model {
    val account by accountManager.currentAccount.collectAsState()
    ...
  }
}

class OnboardingPresenterImpl(
  private val lazyLoginPresenter: () -> LoginPresenter,
  private val lazyRegistrationPresenter: () -> RegistrationPresenter,
  private val accountManager: AccountManager,
) : OnboardingPresenter {

  @Composable
  fun present(input: Unit): BaseModel {
    val account by accountManager.currentAccount.collectAsState()
    ...

    return if (accountManager.mustRegister()) {
      val registrationPresenter = remember { lazyRegistrationPresenter() }
      registrationPresenter.present(Unit)
    } else {
      val loginPresenter = remember { lazyLoginPresenter() }
      loginPresenter.present(Unit)
    }
  }
}
```

This example shows how `AccountManager` holds state and is injected into multiple presenters instead of relying
on presenter inputs.

## Launching

`MoleculePresenters` can inject other presenters and call their `present()` function inline. If you are already in a
composable UI context, then you can simply call the presenter to compute the model:

```kotlin
fun mainViewController(): UIViewController = ComposeUIViewController {
  val presenter = remember { LoginPresenter() }
  val model = presenter.present(Unit)
  ...
}
```

In this example the `LoginPresenter` model is computed from an iOS Compose Multiplatform function.

In other scenarios a composable context may not be available and it's necessary to turn the `@Composable` functions
into a `StateFlow` for consumption.

[`MoleculeScope`](https://github.com/vRallev/app-platform/blob/main/presenter-molecule/public/src/commonMain/kotlin/software/ralf/app/platform/presenter/molecule/MoleculeScope.kt)
helps to turn a `MoleculePresenter` into a `Presenter`, which then exposes a `StateFlow`:

```kotlin
val stateFlow = moleculeScope
  .launchMoleculePresenter(
    presenter = myPresenter,
    input = Unit,
  )
  .model
```

!!! warning

    `MoleculeScope` wraps a `CoroutineScope`. The presenter keeps running, recomposing and producing new models
    until the `MoleculeScope` is canceled. If the `MoleculeScope` is never canceled, then presenters leak and will
    cause issues later.

    Use [`MoleculeScopeFactory`](https://github.com/vRallev/app-platform/blob/main/presenter-molecule/public/src/commonMain/kotlin/software/ralf/app/platform/presenter/molecule/MoleculeScopeFactory.kt)
    to create a new `MoleculeScope` instance and call `cancel()` when you don't need it anymore.

    On Android an implementation using `ViewModels` may look like this:

    ```kotlin
    class MainActivityViewModel(
      moleculeScopeFactory: MoleculeScopeFactory,
      myPresenter: MyPresenter,
    ) : ViewModel() {

      private val moleculeScope = moleculeScopeFactory.createMoleculeScope()

      // Expose the models for consumption.
      val models = moleculeScope
        .launchMoleculePresenter(
          presenter = myPresenter,
          input = Unit
        )
        .models

      override fun onCleared() {
        moleculeScope.cancel()
      }
    }
    ```

!!! info

    By default `MoleculeScope` uses the main thread for running presenters and
    [`RecompositionMode.ContextClock`](https://github.com/cashapp/molecule/blob/trunk/molecule-runtime/src/commonMain/kotlin/app/cash/molecule/RecompositionMode.kt),
    meaning a new model is produced only once per UI frame and further changes are conflated.

    This behavior can be changed by creating a custom `MoleculeScope`, e.g. tests make use of this:

    ```kotlin
    fun TestScope.moleculeScope(
      coroutineContext: CoroutineContext = EmptyCoroutineContext
    ): MoleculeScope {
      val scope = backgroundScope + CoroutineName("TestMoleculeScope") + coroutineContext

      return MoleculeScope(scope, RecompositionMode.Immediate)
    }
    ```

### Detached presenters

Calling a child presenter's `present()` function directly makes it part of the same Compose hierarchy as its parent.
This is the right default because the call is simple and composition locals naturally flow through the presenter tree.
However, every parent recomposition also invokes every inline child presenter. If a busy parent produces a new value
frequently, then expensive child presenters below it are called just as frequently even when their own input has not
changed.

Use `presentDetached()` when a child presenter should run in its own Molecule hierarchy:

```kotlin
class ParentPresenter(
  private val busyPresenter: BusyPresenter,
  private val expensivePresenter: ExpensivePresenter,
) : MoleculePresenter<Unit, ParentPresenter.Model> {

  @Composable
  override fun present(input: Unit): Model {
    val busyModel = busyPresenter.present(Unit)
    val expensiveModel = expensivePresenter.presentDetached(Unit)

    return Model(busyModel, expensiveModel)
  }

  data class Model(
    val busyModel: BusyPresenter.Model,
    val expensiveModel: ExpensivePresenter.Model,
  ) : BaseModel
}
```

In this example, recompositions caused by `BusyPresenter` do not invoke `ExpensivePresenter.present()` again. The
detached presenter still recomposes when its own `input` changes or when state read by the detached hierarchy changes.

`presentDetached()` uses the current presenter's `RecompositionMode` by default and cancels the detached hierarchy when
the call leaves composition. Prefer direct `present()` calls for cheap child presenters or when the child depends on
composition locals provided by the parent. Only App Platform composition locals explicitly preserved by
`presentDetached()` are available in the detached hierarchy.

The detached hierarchy catches up to input changes asynchronously. If the parent input changes, the parent presenter may
emit one model containing the new parent input and the previous detached child model before the detached presenter
recomposes with the new input. Prefer a direct `present()` call when parent and child model state must update atomically
in the same emission. This is not a concern when the detached presenter always receives the same input, such as `Unit`;
in that case, child presenter updates are driven by the detached hierarchy's own state changes.

## Testing

A [`test()`](https://github.com/vRallev/app-platform/blob/main/presenter-molecule/testing/src/commonMain/kotlin/software/ralf/app/platform/presenter/molecule/TestPresenter.kt)
utility function is provided to make testing `MoleculePresenters` easy using the [Turbine](https://github.com/cashapp/turbine/)
library:

```kotlin
class LoginPresenterImplTest {

  @Test
  fun `after 1 second the user is logged in after pressing the login button`() = runTest {
    val userManager = FakeUserManager()

    LoginPresenterImpl(userManager).test(this) {
      val firstModel = awaitItem()
      ...
    }
  }
}
```

The `test()` function uses the `TestScope.backgroundScope` to run the presenter.

??? example "Sample"

    The sample application implements multiple tests for its presenters, e.g.
    [`LoginPresenterImplTest`](https://github.com/vRallev/app-platform/blob/main/sample/login/impl/src/commonTest/kotlin/software/ralf/app/platform/sample/login/LoginPresenterImplTest.kt),
    [`NavigationPresenterImplTest`](https://github.com/vRallev/app-platform/blob/main/sample/navigation/impl/src/commonTest/kotlin/software/ralf/app/platform/sample/navigation/NavigationPresenterImplTest.kt)
    and [`UserPagePresenterImplTest`](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonTest/kotlin/software/ralf/app/platform/sample/user/UserPagePresenterImplTest.kt).

## Back gestures

`Presenters` support back gestures with a similar API in terms of syntax and semantic to Compose Multiplatform. Any
`Presenter` can call these functions:

```kotlin
@Composable
fun present(input: Unit): Model {
  BackHandlerPresenter {
    // Handle a back press.  
  }

  PredictiveBackHandlerPresenter { progress: Flow<BackEventCompat> ->
    // code for gesture back started
    try {
      progress.collect { backevent ->
        // code for progress
      }
      // code for completion
    } catch (e: CancellationException) {
      // code for cancellation
    }
  }  
}
```

!!! warning

    Notice `Presenter` suffix in these function names. These functions should not be confused with `BackHandler {}` and
    `PredictiveBackHandler {}` coming from Compose Multiplatform or Compose UI Android, which would fail at runtime 
    when called from a `Presenter`.

Calling these functions requires `BackGestureDispatcherPresenter` to be setup as composition local. This is usually
done from the root presenter in your hierarchy. An instance of `BackGestureDispatcherPresenter` is provided by App
Platform in the application scope and can be injected:

```kotlin hl_lines="3 7 8 9"
@Inject
class RootPresenter(
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    return returningCompositionLocalProvider(
      LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
    ) {
      // Call other child presenters.
    }
  }
}
```

The last step is to forward back gestures from the UI layer to `Presenters` to invoke the callbacks in the
`Presenters`. Here again it's recommended to do this from within the root `Renderer`:

```kotlin hl_lines="4 8"
@ContributesRenderer
class RootPresenterRenderer(
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()

    // Call other child renderers.
  }
}
```

A similar built-in integration is provided for Android Views. There it's recommended to call this function from each
Android `Activity`:

```kotlin hl_lines="6"
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    backGestureDispatcherPresenter.forwardBackPressEventsToPresenters(this)
    // ...
  }
}
```

Unit tests verifying the behavior of a `Presenter` using the back handler APIs need to provide the composition local
as well. This can be achieved by wrapping the `Presenter` with `withBackGestureDispatcher()`:

```kotlin
class MyPresenterTest {

  @Test
  fun `test back handler`() = runTest {
    val presenter = MyPresenter()

    presenter.withBackGestureDispatcher().test(this) {
      // Verify the produced models from the presenter.
    }
  }
}
```

??? example "Sample"

    The `BackHandlerPresenter {}` call has been integrated in the sample application with this recommended setup. All
    necessary changes are part of this [commit](https://github.com/vRallev/app-platform/pull/84/commits/a807a5673973eae26940cd1130dad836cb3dbd43).

    The same setup has been integrated in the recipes app part of this [commit](https://github.com/vRallev/app-platform/pull/82/commits/fce1b3fbc0b2683ec6a93a499694f914bac34b18)
    as well. 

## Compose runtime

One of the major benefits of using Compose through Molecule is how the framework turns reactive streams such as
`Flow` and `StateFlow` into imperative code, which then becomes easier to understand, write and maintain.
Composable functions have a lifecycle, they enter a composition (the presenter starts to be used) and leave
a composition (the presenter is no longer used). Properties can be made reactive and trigger creating a
new `Model` whenever they change.

### Lifecycle

This example contains two child presenters:

```kotlin
class OnboardingPresenterImpl(
  private val lazyLoginPresenter: () -> LoginPresenter,
  private val lazyRegistrationPresenter: () -> RegistrationPresenter,
) : OnboardingPresenter {

  @Composable
  fun present(input: Unit): BaseModel {
    ...
    return if (mustRegister) {
      val registrationPresenter = remember { lazyRegistrationPresenter() }
      registrationPresenter.present(Unit)
    } else {
      val loginPresenter = remember { lazyLoginPresenter() }
      loginPresenter.present(Unit)
    }
  }
}
```

On the first composition, when `OnboardingPresenterImpl.present()` is called for the first time, the lifecycle of
`OnboardingPresenterImpl` starts. Let’s assume `mustRegister` is true, then `RegistrationPresenter` gets called
and its lifecycle starts as well. In the example when `mustRegister` switches to false, then `RegistrationPresenter`
leaves the composition and its lifecycle ends. `LoginPresenter` enters the composition and its lifecycle starts.
If the parent presenter of `OnboardingPresenterImpl` stops calling this presenter, then `OnboardingPresenterImpl`
and `LoginPresenter` would leave composition and both of their lifecycles end.

### State

[Google’s guide](https://developer.android.com/develop/ui/compose/state) for state management is a good starting
point. APIs most often used are [`remember()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#remember(kotlin.Function0)),
[`mutableStateOf()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#mutableStateOf(kotlin.Any,androidx.compose.runtime.SnapshotMutationPolicy)),
[`collectAsState()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#(kotlinx.coroutines.flow.StateFlow).collectAsState(kotlin.coroutines.CoroutineContext))
and [`produceState()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#produceState(kotlin.Any,kotlin.coroutines.SuspendFunction1)).

```kotlin
@Composable
fun present(input: Unit): Model {
  var toggled: Boolean by remember { mutableStateOf(false) }

  return Model(
    text = if (toggled) "toggled" else "not toggled",
  ) {
    when (it) {
      is ToggleClicked -> toggled = !toggled
    }
  }
}
```

In this example, whenever the Presenter receives the `ToggleClicked` event, then the state `toggled` changes.
This triggers a recomposition in the Compose runtime and will call `present()` again to compute a new `Model`.

`Flows` can easily be observed using `collectAsState()`:

```kotlin hl_lines="10"
interface AccountManager {
  val currentAccount: StateFlow<Account>
}

class LoginPresenterImpl(
  private val accountManager: AccountManager
): LoginPresenter {
  @Composable
  fun present(input: Unit): Model {
    val account: Account by accountManager.currentAccount.collectAsState()
    ...
  }
}
```

Whenever the `currentAccount` Flow emits a new `Account`, then the Compose runtime will trigger a recomposition
and a new `Model` will be computed.

### Side effects

It’s recommended to read [Google’s guide](https://developer.android.com/jetpack/compose/side-effects). Since
composable functions come with a lifecycle, async operations can safely be launched and get automatically torn
down when the `Presenter` leaves the composition. Commonly used APIs are
[`LaunchedEffect()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#LaunchedEffect(kotlin.Any,kotlin.coroutines.SuspendFunction1)),
[`DisposableEffect()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#DisposableEffect(kotlin.Any,kotlin.Function1))
and [`rememberCoroutineScope()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#rememberCoroutineScope(kotlin.Function0)).

```kotlin
@Composable
fun present(input: Unit): Model {
  LaunchedEffect(key) {
    // This is within a CoroutineScope and suspending functions can
    // be called:
    flowOf(1, 2, 3).collect { ... }
  }
}
```

If the `key` changes between compositions, then a new coroutine is launched and the previous one canceled. For more
details see [here](https://developer.android.com/jetpack/compose/side-effects#launchedeffect).

This is an example for how one would use `rememberCoroutineScope()`:

```kotlin
@Composable
fun present(input: Unit): Model {
  val coroutineScope = rememberCoroutineScope()

  return Model() {
    when (it) {
      is OnClick -> coroutineScope.launch { ... }
    }
  }
}
```

When the `Presenter` leaves composition, then all jobs launched by this coroutine scope get canceled. For more
details see [here](https://developer.android.com/jetpack/compose/side-effects#remembercoroutinescope).

## Recipes

There are common scenarios you may encounter when using `Presenters`.

!!! info

    Most recipes below are not part of the stable App Platform API and we look for feedback. Some pieces, such as
    `ReturningSaveableStateHolder`, are exposed as experimental APIs while their shape is validated.

    The [Recipes app](index.md#web-recipe-app) and [Sample app](index.md#web-clickable) can be tested in the browser.

### Save `Presenter` state

`Presenters` can make full use of the Compose runtime, e.g. using `remember { }` and `mutableStateOf()`. But when a
`Presenter` leaves the composition and no longer is part of the hierarchy, then it loses its state and would be called
with the initial state the next time.

```kotlin
@Composable
fun present(input: Unit): Model {
  val showLogin = ...

  val model = if (showLogin) {
    loginPresenter.present(Unit)
  } else {
    registerPresenter.present(Unit)
  }

  return model
}
```

Take this function for example. Every time `showLogin` is toggled then either `loginPresenter` or `registerPresenter`
is called with their initial state. These presenters only remember their state, if `showLogin` doesn't change.

The Compose runtime provides `rememberSaveable { }` and `SaveableStateHolder` as a solution to save and restore small
pieces of UI state. App Platform provides the experimental
[`ReturningSaveableStateHolder`](https://github.com/vRallev/app-platform/blob/main/presenter-molecule/public/src/commonMain/kotlin/software/ralf/app/platform/presenter/molecule/saveable/ReturningSaveableStateHolder.kt)
API for `@Composable` functions that return a value. This matters for `MoleculePresenter` functions, because a presenter
doesn't render UI directly; it returns a model.

`Presenters` wrapped with `ReturningSaveableStateHolder` can use `rememberSaveable { }` to restore state even after they
weren't part of the hierarchy anymore:

```kotlin
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.molecule.saveable.rememberReturningSaveableStateHolder

@OptIn(ExperimentalAppPlatform::class)
@Composable
fun present(input: Unit): Model {
  val showLogin = ...

  val saveableStateHolder = rememberReturningSaveableStateHolder()

  val presenter = if (showLogin) loginPresenter else registerPresenter
  val presenterKey = if (showLogin) "login" else "register"

  return saveableStateHolder.SaveableStateProvider(key = presenterKey) {
    presenter.present(Unit)
  }
}
```

State wrapped in `rememberSaveable { }` in `LoginPresenter` and `RegisterPresenter` will be preserved no
matter how often `showLogin` is toggled.

The key passed to `SaveableStateProvider` identifies the state bucket for that subtree. Use a stable key with stable
`equals()` and `hashCode()` behavior, and don't compose the same key in two active providers at the same time. If the
holder is running under a parent `LocalSaveableStateRegistry`, the key and saved values must also be saveable by that
registry. On Android that generally means they need to fit into saved instance state, or be converted by a custom
`Saver`.

There are two persistence layers. While the holder is alive, inactive presenter state is kept in the holder. The holder
itself is remembered with `rememberSaveable`, so a parent saveable registry can save that inactive-state map. Without a
parent saveable registry, `ReturningSaveableStateHolder` still preserves state while switching between subtrees in the
current composition, but it doesn't provide process-death persistence by itself.

### `Presenter` backstack

With `Presenters` it's easy to implement model driven navigation. Which `Presenter` is shown on screen is part of the
business logic.

```kotlin
@Composable
fun present(input: Unit): Model {
  val showLogin = ...

  val model = if (showLogin) {
    loginPresenter.present(Unit)
  } else {
    registerPresenter.present(Unit)
  }

  return model
}
```

This pattern can be generalized:

```kotlin
interface NavigationManager {
  val currentPresenter: StateFlow<MoleculePresenter<Unit, BaseModel>>

  fun navigateTo(presenter: MoleculePresenter<Unit, BaseModel>)
}

@Inject
class NavigationPresenter(val navigationManager: NavigationManager) : MoleculePresenter<Unit, BaseModel> {

  @Compose
  fun present(input: Unit): BaseModel {
    val presenter by navigationManager.currentPresenter.collectAsState()
    return presenter.present(Unit)
  }
}
```

This solution always shows the `Presenter` for which `navigateTo()` was called last. This function can be called from
anywhere in the app.

Another solution is a backstack of `Presenters`, where `Presenters` can be pushed to the stack and the top
most `Presenter` can be popped from the stack. App Platform provides this as an experimental Navigation 3 backed API.
The easiest way to import it is the Gradle plugin option:

```groovy
appPlatform {
  enableMoleculePresenterBackstack true
}
```

This option adds the presenter backstack module and also enables Molecule presenters and Compose UI. The API keeps the
backstack in presenter code, while the renderer integration delegates rendering, back gestures, retained entries, and
transitions to Navigation 3.

The Recipes app wraps the shared API in a small app-specific presenter:

```kotlin
class CrossSlideBackstackPresenter(
  private val initialPresenter: MoleculePresenter<Unit, out BaseModel>
) : MoleculePresenter<Unit, CrossSlideBackstackPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    return presenterBackstack(initialPresenter) { backstack ->
      Model(backstack = backstack, onBack = { pop() })
    }
  }

  data class Model(
    override val backstack: List<BaseModel>,
    override val onBack: () -> Unit,
  ) : PresenterBackstackModel
}
```

`presenterBackstack()` always keeps the initial presenter as the root entry. It composes every presenter in the stack,
returns the resulting model list to the `content` lambda, and provides
[`PresenterBackstackScope`](https://github.com/vRallev/app-platform/blob/main/presenter-backstack-nav3/public/src/commonMain/kotlin/software/ralf/app/platform/presenter/backstack/nav3/PresenterBackstackScope.kt)
to the presenter subtree. The scope exposes `push()`, `pop()`, and `replaceTop()`.

The model's `onBack` callback is the only back hook the presenter needs for renderer-driven back navigation.
`PresenterBackstackRenderer` passes it to Navigation 3's `NavDisplay`, so back gestures and `NavDisplay` back events
call back into presenter code and pop the presenter-owned stack.

Child presenters get access to the nearest scope through `LocalBackstackScope`:

```kotlin
@Composable
override fun present(input: Unit): Model {
  val backstack = LocalBackstackScope.requireNotNull()
  ...

  return Model {
    when (it) {
      Event.AddPresenterToBackstack -> backstack.push(BackstackChildPresenter())
    }
  }
}
```

For unit tests of presenters that read `LocalBackstackScope`, use the testing module and wrap the presenter with a
fake scope. `withPresenterBackstackScope()` only provides the composition local. It does not add the presenter under
test to the fake backstack automatically.

```kotlin
val backstackScope = FakePresenterBackstackScope()

presenter
  .withPresenterBackstackScope(backstackScope)
  .test(this) {
    val model = awaitItem()

    model.onContinue()

    assertThat(backstackScope.lastBackstackChange.value.action)
      .isEqualTo(Action.PUSH)
  }
```

If the presenter under test calls `pop()`, use the fake's default placeholder root and push the presenter before
invoking the callback. When the presenter under test is the root entry, `pop()` is ignored, matching production
behavior.

```kotlin
val backstackScope = FakePresenterBackstackScope()
backstackScope.push(presenter)

presenter
  .withPresenterBackstackScope(backstackScope)
  .test(this) {
    val model = awaitItem()

    model.onBack()

    assertThat(backstackScope.recordedBackstackChanges.value.map { it.action })
      .containsExactly(Action.PUSH, Action.PUSH, Action.POP)
  }
```

Consumers define their own `PresenterBackstackModel` and renderer. The base `PresenterBackstackRenderer` creates stable
Navigation 3 keys for model entries, keeps popped models available while exit transitions finish, and forwards
`PresenterBackstackModel.onBack` to `NavDisplay`. The renderer only needs to render each model:

```kotlin
@ContributesRenderer
class CrossSlideBackstackRenderer(
  private val rendererFactory: RendererFactory,
) : PresenterBackstackRenderer<CrossSlideBackstackPresenter.Model>() {
  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    rendererFactory.getComposeRenderer(model).renderCompose(model)
  }
}
```

The same renderer can customize Navigation 3 behavior by overriding `PresenterNavDisplay()`. When overriding this
function, pass all received parameters to `NavDisplay`; they connect the presenter-owned stack to Navigation 3 and keep
retained entries renderable during transitions:

```kotlin
@Composable
override fun PresenterNavDisplay(
  backstack: List<Int>,
  onBack: () -> Unit,
  entryProvider: (Int) -> NavEntry<Int>,
  modifier: Modifier,
) {
  NavDisplay(
    backStack = backstack,
    onBack = onBack,
    entryProvider = entryProvider,
    modifier = modifier,
    transitionSpec = {
      crossSlideTransition(AnimatedContentTransitionScope.SlideDirection.Left)
    },
    popTransitionSpec = {
      crossSlideTransition(AnimatedContentTransitionScope.SlideDirection.Right)
    },
    predictivePopTransitionSpec = {
      crossSlideTransition(AnimatedContentTransitionScope.SlideDirection.Right)
    },
  )
}
```

### `CompositionLocal`

Both the `BackHandlerPresenter { }` integration for back button presses and the backstack recipe for navigation leverage
[Compose's `CompositionLocal` feature](https://developer.android.com/develop/ui/compose/compositionlocal#creating).
This is a powerful mechanism to provide state from a parent presenter to nested child presenters even deep down in
the stack without relying on the `Input` parameter of presenters or providing
dependencies through the constructor. Another benefit is that `CompositionLocals` are embedded in the presenter tree
and multiple instances can be provided for different parts of the tree or even be overridden, e.g. a parent presenter
may use a backstack, but then a child presenter may provide its own backstack for its child presenters.

A common implementation may look like this:

```kotlin
class YourType

public val LocalYourType: ProvidableCompositionLocal<YourType?> = compositionLocalOf { null }

class ParentPresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val yourType = remember { YourType() }

    return returningCompositionLocalProvider(
      LocalYourType provides yourType
    ) {
      // ... call child presenters
    }
  }
}

class ChildPresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val yourType = checkNotNull(LocalYourType.current)
    ...
  }
}
```

While `CompositionLocals` are powerful, their biggest downsides are unit tests. In a unit test for `ChildPresenter`
a value for `LocalYourType.current` must be provided, otherwise the call will throw an exception.

### Presenter-backed text fields

Text inputs have renderer-specific editing state, such as cursor position and selection, but presenters
often still need to own the actual text value. `PresenterTextFieldState` stores the text value in the presenter 
layer without depending on Compose Foundation's `TextFieldState`.

```kotlin
@OptIn(ExperimentalAppPlatform::class)
class SearchPresenter : MoleculePresenter<Unit, SearchPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    val query = remember { PresenterTextFieldState() }

    return Model(
      query = query,
      clearQuery = query::clearText,
    )
  }

  data class Model(
    val query: PresenterTextFieldState,
    val clearQuery: () -> Unit,
  ) : BaseModel
}
```

Compose renderers can bridge the presenter-owned text value to Compose Foundation's `TextFieldState`
with the experimental `rememberPresenterBackedTextFieldState()` helper:

```kotlin
@OptIn(ExperimentalAppPlatform::class)
@ContributesRenderer
class SearchRenderer : ComposeRenderer<SearchPresenter.Model>() {
  @Composable
  override fun Compose(model: SearchPresenter.Model, modifier: Modifier) {
    val queryState = rememberPresenterBackedTextFieldState(model.query)

    BasicTextField(
      state = queryState,
      modifier = modifier,
    )
  }
}
```

Edits made by the user are copied back to the `PresenterTextFieldState`, and presenter changes such
as `replaceText()` or `clearText()` are copied into the Compose text field. When presenter text is
applied to the renderer state, the cursor is placed at the end of the new value.

### App Bar

The Recipes app implements an app bar for all its screens and allows child presenters to change the content.

There are multiple ways to implement the app bar and decompose the different screen elements. One way is using
[Templates](template.md), where one slot in the template is reserved for the app bar model. A specific `Presenter`
could be responsible for providing this model:

```kotlin
sealed interface SampleAppTemplate : Template {

  data class FullScreenTemplate(
    val appBarModel: AppBarModel
    val content: BaseModel,
  ) : SampleAppTemplate
}

class SampleAppTemplatePresenter(
  private val appBarPresenter: AppBarPresenter,
  private val rootPresenter: MoleculePresenter<Unit, BaseModel>,
) : MoleculePresenter<Unit, SampleAppTemplate> {
  @Composable
  fun present(input: Unit): SampleAppTemplate {
    val contentModel = rootPresenter.present(Unit)

    return contentModel.toTemplate { model ->
      val appBarModel = appBarPresenter.present(Unit)
      FullScreenTemplate(appBarModel, contentModel)
    }
  }
}
```

The `SampleAppTemplateRenderer` has access to `appBarModel` from the `FullScreenTemplate` and can use the model
to configure the app bar UI.

The Recipe app has chosen a different implementation, where any `BaseModel` class from a `Presenter` can implement the
specific [`AppBarConfigModel`](https://github.com/vRallev/app-platform/blob/main/recipes/common/impl/src/commonMain/kotlin/software/ralf/app/platform/recipes/appbar/AppBarConfigModel.kt)
interface, which provides the configuration for the app bar. Implementing this interface is optional:

```kotlin
class MenuPresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    ...
  }

  data class Model(
    private val menuItems: List<AppBarConfig.MenuItem>,
  ) : BaseModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig {
      return AppBarConfig(title = "Menu items", menuItems = menuItems)
    }
  }
}
```

If a `BaseModel` implementing `AppBarConfigModel` bubbles all the way up to the
[`RootPresenter`](https://github.com/vRallev/app-platform/blob/main/recipes/common/impl/src/commonMain/kotlin/software/ralf/app/platform/recipes/template/RootPresenter.kt),
then the `BaseModel` from the child `Presenter` will provide the config for the `Template` or otherwise the
`RootPresenter` will provide a default:

```kotlin
return contentModel.toTemplate { model ->
  val appBarConfig =
    if (model is AppBarConfigModel) {
      model.appBarConfig()
    } else {
      AppBarConfig.DEFAULT
    }

  RecipesAppTemplate.FullScreenTemplate(model, appBarConfig)
}
```

For the Recipes app backstack, `CrossSlideBackstackPresenter.Model` implements `AppBarConfigModel`. It delegates the
active child model's app bar configuration and adds the back-arrow action when the presenter stack has more than one
entry.

### Navigation 3

The [Navigation 3 library](https://developer.android.com/guide/navigation/navigation-3) can be used with App Platform.
For idiomatic navigation App Platform [recommends](presenter.md#model-driven-navigation) handling navigation events in 
`Presenters`. `Presenters` are composable, build a tree and can delegate which `Presenter` is shown on screen to child 
`Presenters`. This is how App Platform implements a unidirectional dataflow. The downside of Navigation 3 is that 
it pushes navigation logic into the Compose UI layer, which is against App Platform's philosophy of handling navigation 
in the business logic. With the right integration strategy, this downside can be mitigated.

The presenter backstack API is the recommended integration strategy when a Compose renderer should use Navigation 3
but presenter code should still own navigation state. The Recipes app's
[`Navigation3HomePresenter`](https://github.com/vRallev/app-platform/blob/main/recipes/common/impl/src/commonMain/kotlin/software/ralf/app/platform/recipes/nav3/Navigation3HomePresenter.kt)
hosts a nested presenter stack:

```kotlin
@Composable
override fun present(input: Unit): Model {
  return presenterBackstack(Navigation3ChildPresenter(index = 0)) { backstack ->
    Model(backstack = backstack, onBack = { pop() })
  }
}

data class Model(
  override val backstack: List<BaseModel>,
  override val onBack: () -> Unit,
) : PresenterBackstackModel
```

Child presenters use the scope provided by `presenterBackstack()` to mutate that stack:

```kotlin
@Composable
override fun present(input: Unit): Model {
  val backstack = LocalBackstackScope.requireNotNull()

  return Model {
    when (it) {
      Event.AddPresenter -> backstack.push(Navigation3ChildPresenter(index = index + 1))
    }
  }
}
```

The
[`Renderer`](https://github.com/vRallev/app-platform/blob/main/recipes/common/impl/src/commonMain/kotlin/software/ralf/app/platform/recipes/nav3/Navigation3HomeRenderer.kt)
extends `PresenterBackstackRenderer`. The base renderer wraps the model stack in `NavDisplay`, gives each entry a
stable Navigation 3 key, invokes the individual renderer for each model, and forwards back gestures to the presenter
model's `onBack` callback:

```kotlin
class Navigation3HomeRenderer(
  private val rendererFactory: RendererFactory,
) : PresenterBackstackRenderer<Navigation3HomePresenter.Model>() {
  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    rendererFactory.getComposeRenderer(model).renderCompose(model)
  }
}
```

With this integration, handling of the backstack remains in `Presenters` and is testable, while Navigation 3 handles
the renderer-level navigation container and back gesture integration.

??? info "Alternative integration"

    If a unidirectional dataflow isn't required, an alternative integration is making each `NavEntry` a unique 
    `Presenter` root and compute the `Model` directly using the `Presenter`. For the reasons mentioned we don't 
    recommend this setup.

    ```kotlin
    data object List
    data object Detail

    @ContributesRenderer
    class Navigation3Renderer(
      private val listPresenter: ListPresenter,
      private val detailPresenter: DetailPresenter,
      private val rendererFactory: RendererFactory,
    ) : ComposeRenderer<Model>() {
      @Composable
      override fun Compose(model: Model, modifier: Modifier) {
        val backstack = remember { mutableStateListOf<Any>(List) }

        NavDisplay(
          backStack = backstack,
          onBack = { backstack.removeAt(backstack.size - 1) },
          entryProvider =
            entryProvider {
              entry<List> {
                val model = listPresenter.present(Unit)
                rendererFactory.getComposeRenderer(model).renderCompose(model)
              }
              entry<Detail> {
                val model = detailPresenter.present(Unit)
                rendererFactory.getComposeRenderer(model).renderCompose(model)
              }
            },
        )
      }
    }
    ```

### SwiftUI

#### `Presenters` and SwiftUI `Views`

In iOS it's possible to connect `Presenters` to SwiftUI `Views` so `Presenter` logic can be shared while keeping UI
native. The Recipes app demonstrates a [set of Swift APIs](https://github.com/vRallev/app-platform/tree/main/recipes/app/ios/recipesIosApp/PresenterViews)
that demonstrate how to launch a `Presenter` and render SwiftUI `Views` in the iOS flavor. Note that App Platform 
does not provide an API equivalent of SwiftUI `Renderers`. As such, we need to decide how to observe the flow of models 
from a given `Presenter` and create `Views` from them.

To obtain an observable stream of models, `Presenter` can be extended to provide an `AsyncThrowingStream` from the 
model `StateFlow`. It's also possible to implement a convenient extension of `Flow` so we can convert any `Flow` to an
`AsyncThrowingStream`.

```swift
extension Presenter {
    func viewModels<Model>(ofType type: Model.Type) -> AsyncThrowingStream<Model, Error> {
        model
            .values()
            .compactMap { $0 as? Model }
            .asAsyncThrowingStream()
    }
}

extension Kotlinx_coroutines_coreFlow {
    /// The Flows send Any, so we lose type information and need to cast at runtime instead of getting a type-safe compile time check.
    func values() -> AsyncThrowingStream<Any?, Error> {
        let collector = Kotlinx_coroutines_coreFlowCollectorImpl<Any?>()
        collect(collector: collector, completionHandler: collector.onComplete(_:))
        return collector.values
    }
}
```

Given a `Model` there are multiple ways to implement association with some SwiftUI `View`. The Recipes app chooses to
create a [`protocol`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/protocols/) for
view creation and extend `BaseModel` to create views under the requirement of its conformance:

```swift
protocol PresenterViewModel {
    associatedtype Renderer : View
    @ViewBuilder @MainActor func makeViewRenderer() -> Self.Renderer
}

extension BaseModel {
    @MainActor func getViewRenderer() -> AnyView {
        guard let viewModel = self as? (any PresenterViewModel) else {
            assertionFailure("ViewModel \(self) does not conform to `PresenterViewModel`")

            // This is an implementation detail. If crashing is preferred even in production builds, `fatalError(..)`
            // can be used instead
            return AnyView(Text("Error, some ViewModel was not implemented!"))
        }

        return AnyView(viewModel.makeViewRenderer())
    }
}
```

??? info "Alternate implementation"

    We can also create a `View` registry:
    
    ```swift
    public class PresenterViewRegistry {
        @MainActor private var registry: [ObjectIdentifier: (Any) -> AnyView] = [:]
    
        public init(registry: [ObjectIdentifier : (Any) -> AnyView] = [:]) {
            self.registry = registry
        }
    
        public static var shared: PresenterViewRegistry = PresenterViewRegistry()
    }
    
    @MainActor public extension PresenterViewRegistry {
        func registerViewForModelType<Model, Content: View>(_ type: Model.Type, makeView: @escaping (Model) -> Content) {
            let typeID = ObjectIdentifier(Model.self)
            registry[typeID] = { model in
                AnyView(makeView(model as! Model))
            }
        }
    
        func makeViewForModel<Model>(_ model: Model) -> some View {
            let type = type(of: model as Any)
            let typeID = ObjectIdentifier(type)
            if let makeView = registry[typeID] {
                return makeView(model)
            }
            fatalError("Could not find view builder for \(type). Add it to the registry.")
        }
    }
    ```
    
    The registry can be stored in an `Environment` property wrapper. This is similar to how `@ContributesRenderer` works 
    under the hood, though without an equivalent App Platform API the heavy lifting on registration and registry 
    lifecycle management falls to consumers. Due to these reasons we generally recommend to use the protocol setup.

#### Navigation with `Presenters` and SwiftUI

SwiftUI provides [navigation containers](https://developer.apple.com/documentation/swiftui/navigation) to enable
movement between different part of an app's view hierarchy. Similar to `Navigation 3`, SwiftUI's navigation containers 
push navigation logic to the UI layer, which is against App Platform's philosophy of handling navigation in business
logic. However, to support navigation with SwiftUI `Views` and `Presenters`, it is recommended to integrate with 
SwiftUI's navigation offerings. This SwiftUI keeps the determination of some completed back gesture an implementation
detail, and we want ensure that all back events are handled appropriately and the user experience feels truly native.

!!! note

    We provide a recipe for integration with `NavigationStack` for single column navigation based on back gesture. For 
    other kinds of navigation with `NavigationSplitView` or `NavigationLink` it is possible to integrate following our
    [model driven navigation](https://vrallev.github.io/app-platform/presenter/#model-driven-navigation) pattern. However,
    we don't provide an explicit recipe for it. If you're missing some use cases here, please let us know.

The Recipes app demonstrates how SwiftUI navigation APIs can be used while following App Platform's philosophy of 
unidirectional data flow. As navigation is a part of business logic, the recipe [implements navigation with 
a backstack of `Presenters`](https://github.com/vRallev/app-platform/blob/main/recipes/common/impl/src/commonMain/kotlin/software/ralf/app/platform/recipes/swiftui/SwiftUiHomePresenter.kt).
The root `Presenter` responsible for the `Presenter` backstack computes the `Model` backstack:

```kotlin
@Composable
  override fun present(input: Unit): Model {
    val backstack = remember {
      mutableStateListOf<MoleculePresenter<Unit, out BaseModel>>().apply {
        // There must be always one element.
        add(SwiftUiChildPresenter(index = 0, backstack = this))
      }
    }

    return Model(modelBackstack = backstack.map { it.present(Unit) }) {
      when (it) {
        is Event.BackstackModificationEvent -> {
          val updatedBackstack = it.indicesBackstack.map { index -> backstack[index] }

          backstack.clear()
          backstack.addAll(updatedBackstack)
        }
      }
    }
  }
```
The `Presenter` forwards the `Models` and event callbacks to a SwiftUI `View`, which
integrates these models with a [`NavigationStack`](https://github.com/vRallev/app-platform/blob/main/recipes/app/ios/recipesIosApp/SwiftUI/SwiftUiHomePresenterView.swift).
Note that to integrate we create a [`Binding`](https://developer.apple.com/documentation/swiftui/binding) that is passed
in to the `NavigationStack`. The `Binding's` value type must conform to `Hashable` and by default `BaseModel` does not
conform. To resolve this in the recipe we simply represent each `Model` by the index of its position in the `Model`
backstack as we do not require more complex identifiers.

```swift
extension SwiftUiHomePresenter.Model {
    func pathBinding() -> Binding<[Int]> {
        .init {
            // drop the first value of the backstack from the path because that should be the root view
            Array(self.modelBackstack.indices.dropFirst())
        } set: { modifiedIndices in

            // the resulting backstack indices the presenter should compute on is the first index (0) that was
            // dropped as well as the remaining indices post modification
            let indicesBackstack = [0] + modifiedIndices.map { $0.toKotlinInt() }

            self.onEvent(
                SwiftUiHomePresenterEventBackstackModificationEvent (
                    indicesBackstack: indicesBackstack
                )
            )
        }
    }
}

private struct NavigationStackView: View {
    var backstack: [BaseModel]
    var model: SwiftUiHomePresenter.Model

    init(model: SwiftUiHomePresenter.Model) {
        self.backstack = model.modelBackstack
        self.model = model
    }

    var body: some View {
        NavigationStack(path: model.pathBinding()) {
            backstack[0].getViewRenderer()
                .navigationDestination(for: Int.self) { index in
                    backstack[index].getViewRenderer()
                }
        }
    }
}
```
