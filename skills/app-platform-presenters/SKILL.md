---
name: app-platform-presenters
description: Build and test App Platform MoleculePresenters. Use when changing models, state, parent and child presenters, template selection or delegation, tests, or Gradle and host setup.
---

# App Platform Molecule Presenters

`MoleculePresenter` uses the Compose runtime to turn inputs and injected data into a `BaseModel`. The UI or a parent presenter reads the model and sends events through its callbacks. Compose UI is optional.

Before editing, identify the inputs, model, events, how long state must last, and who starts and stops the root. Follow the project's module, DI, and build choices. Check its App Platform version before using `ExperimentalAppPlatform` APIs. Examples omit imports; resolve unfamiliar APIs from existing code or matching public docs.

## Models

- Implement `MoleculePresenter<InputT, ModelT>` with `ModelT : BaseModel`. Include all state and callbacks the consumer needs.
- Expose immutable model snapshots, preferably data classes or sealed types. Use `val` properties and immutable values, and return a new model when state changes. Keep equality and public types stable to satisfy `BaseModel`.
- Prefer presenter interfaces with implementations supplied through DI. Put shared contracts and models in `:public`, implementations in `:impl`, and assemble them in the app. Keep implementations used by generated graphs across modules public.
- Nest `Model` under its presenter. Put `present()` first, then helpers, the companion object, and nested types, with `Model` last.
- When consumers need only some fields, define a partial `Model : BaseModel` interface in `:public`. An immutable model in `:impl` implements it and adds fields used only by the implementation. Use a concrete public model when consumers need all fields.

  ```kotlin
  // :public
  interface SearchPresenter : MoleculePresenter<String, SearchPresenter.Model> {
    interface Model : BaseModel {
      val query: String
    }
  }

  // :impl
  class SearchPresenterImpl : SearchPresenter {
    @Composable
    override fun present(input: String): Model {
      return Model(query = input, canSearch = input.isNotBlank())
    }

    data class Model(
      override val query: String,
      val canSearch: Boolean,
    ) : SearchPresenter.Model
  }
  ```

  Parents using `SearchPresenter` can read `query`; its renderer in `:impl` can also read `canSearch`.

- When consumers only pass a model along, hide its type with a generic interface:

  ```kotlin
  interface CatalogPresenter<ModelT : BaseModel> : MoleculePresenter<Unit, ModelT>
  ```

  Consumers can use `CatalogPresenter<*>`. Parents that return different child models can use `BaseModel`.

## Inputs

Use constructor or assisted-factory arguments for fixed values, such as an item ID. Use `InputT` for changing values, such as a filter, and always read the latest input. Use `Unit` when there is no input.

Keep callbacks on the model, not in inputs. Children report results through their models; parents decide what those results mean.

## State and effects

Keep mutable state, caches, scopes, and effects inside `present()`. Presenter fields hold dependencies and fixed arguments. Presenters must not be singletons.

Use `remember` for local state and Compose runtime APIs such as `collectAsState` or `produceState` for changing data:

```kotlin
interface CounterPresenter : MoleculePresenter<Unit, CounterPresenter.Model> {
  data class Model(
    val count: Int,
    val onIncrement: () -> Unit,
  ) : BaseModel
}

class CounterPresenterImpl : CounterPresenter {
  @Composable
  override fun present(input: Unit): CounterPresenter.Model {
    var count by remember { mutableIntStateOf(0) }
    return CounterPresenter.Model(count = count, onIncrement = { count++ })
  }
}
```

Use `LaunchedEffect` to start work, `DisposableEffect` to clean up listeners, and `rememberCoroutineScope` for work from callbacks. Choose keys that restart work when needed. Don't start side effects directly in `present()`, which can run many times.

## Parent and child presenters

Call children with `present(input)`. Parents can select or combine their models to drive navigation. Create optional children through a factory inside the active branch and remember the instance. Call `present()` each time; do not remember its model. Children used on every call can be injected directly.

To reset state for a new item, key both child creation and its `present()` call. Creating a new instance alone does not reset the composition:

```kotlin
return key(input.itemId) {
  val child = remember { itemPresenterFactory.create(input.itemId) }
  child.present(Unit)
}
```

Use `androidx.compose.runtime.key`. For lists, key children by stable item IDs. To keep a child's lifetime, pass changing input and key only the state or effects that need to reset.

Use `presentDetached()` only for costly child presenters. It can briefly pair new parent input with an old child model and does not inherit custom composition locals. Call `present()` directly when updates must agree or locals must be shared.

### Backstack navigation

For push and pop navigation, use experimental `presenterBackstack(initialPresenter) { models -> ... }`. Wrap its models in an app-specific `PresenterBackstackModel` with `onBack = { pop() }`. The helper provides `LocalBackstackScope`; children read `LocalBackstackScope.requireNotNull()` and call `push()`, `pop()`, or `replaceTop()` from model callbacks. The stack holds presenter instances, keeps every entry in composition, and ignores a pop at the root.

Use `PresenterBackstackRenderer` for Navigation 3 UI. Set `appPlatform { enableMoleculePresenterBackstack(true) }` to add the module and enable Molecule presenters and Compose UI. See the [backstack guide](https://vrallev.github.io/app-platform/presenter/#presenter-backstack) for rendering and tests with a fake scope.

## Template selection

Use a `Template` for app-level layout and shell choices. Use a normal parent model for local component layout. A template is the app-specific root model, and its variants name semantic slots instead of holding an untyped list:

```kotlin
sealed interface AppTemplate : Template {
  data class FullScreen(val content: BaseModel) : AppTemplate

  data class ListDetail(
    val list: BaseModel,
    val detail: BaseModel,
  ) : AppTemplate
}
```

Wrap the root presenter in a template presenter. `toTemplate` follows `ModelDelegate` chains. It uses an `AppTemplate` returned by a child, or applies the default to the last delegated model:

```kotlin
class AppTemplatePresenter(
  private val rootPresenter: MoleculePresenter<Unit, *>,
) : MoleculePresenter<Unit, AppTemplate> {
  @Composable
  override fun present(input: Unit): AppTemplate {
    return rootPresenter.present(Unit).toTemplate<AppTemplate> {
      AppTemplate.FullScreen(it)
    }
  }
}
```

Keep app-wide composition locals or presenter services around `rootPresenter.present(Unit)` at this boundary.

An app-owned presenter can choose another layout without needing its own renderer:

```kotlin
data class Model(
  val list: BaseModel,
  val detail: BaseModel,
) : BaseModel, ModelDelegate {
  override fun delegate(): BaseModel = AppTemplate.ListDetail(list, detail)
}
```

Delegation may pass through navigation models, but it must end. Keep reusable feature libraries independent of app template types; let an app-owned presenter choose their slots. Put a shared template contract in `:public` when app-owned presenters need it, and put the root wrapper in `:impl` or app assembly. Follow the root lifecycle guidance below when hosting the wrapper.

Use the `app-platform-renderers` skill for template layout, child rendering, registration, factories, and platform hosts.

## Platform code and UI

Keep Android, Compose UI, `ViewModel`, navigation controllers, and resource IDs out of shared presenter APIs. Put platform work behind injected interfaces.

Add a renderer only when a model needs direct visual output. Renderers handle localized text, layout, animations, and model callbacks. Presenters handle app state, navigation, and template selection. Register visible models through the app's renderer setup and check that the real app graph can resolve them.

Follow the app's existing effect design. If the root creates a dispatcher, remember it there and share it with children. Handle platform effects in the host. Helpers such as `withCompositionLocal` can provide a value while returning a model.

### Add a screen

1. Define the presenter interface, inputs, immutable model, and callbacks.
2. Implement `present()` and wire its dependencies through DI.
3. Add and register a renderer if the model has visible output.
4. Connect it to a parent presenter or wrap it in the app's template.
5. Test presenter behavior with fakes; add UI tests for layout and event wiring when needed.

## Start and stop presenters

In a Compose host, remember the root and call `present()` directly if the host's composition should own its state. Otherwise, let a host own the scope and its cleanup:

```kotlin
class PresenterHost<ModelT : BaseModel>(
  moleculeScopeFactory: MoleculeScopeFactory,
  rootPresenter: MoleculePresenter<Unit, ModelT>,
) {
  private val moleculeScope = moleculeScopeFactory.createMoleculeScope()
  val models = moleculeScope.launchMoleculePresenter(rootPresenter, Unit).model

  fun close() {
    moleculeScope.cancel()
  }
}
```

The factory applies platform defaults. Launching returns `Presenter<ModelT>`, with a `StateFlow` at `.model`. Pass a `StateFlow` input when the host needs to change it.

Call the host's `close()` from its owner's cleanup, such as `ViewModel.onCleared()` or window cleanup. Stopping collection does not stop the presenters. Canceling a `MoleculeScope` also cancels the wrapped coroutine scope, so use a scope the host owns.

## Keep state after a child leaves

When a child stops being called, its ordinary remembered state and effects end. Keeping the presenter instance does not preserve its composition.

- Use `withLocalRetainedValuesStore()` and `retain` to keep values in memory. Keep one managed store per child outside the active branch. Stores outlive inactive children and end with their owner. They do not keep effects running or survive root or process recreation.

  This parent has two destinations and injected child factories:

  ```kotlin
  @OptIn(ExperimentalAppPlatform::class)
  @Composable
  override fun present(input: Destination): BaseModel {
    val firstStore = key(Destination.First) { retainManagedRetainedValuesStore() }
    val secondStore = key(Destination.Second) { retainManagedRetainedValuesStore() }

    return when (input) {
      Destination.First -> withLocalRetainedValuesStore(firstStore) {
        val child = remember { createFirstPresenter() }
        child.present(Unit)
      }
      Destination.Second -> withLocalRetainedValuesStore(secondStore) {
        val child = remember { createSecondPresenter() }
        child.present(Unit)
      }
    }
  }
  ```

  Inside each child's `present()`, using the counter model above:

  ```kotlin
  var count by retain { mutableIntStateOf(0) }
  return CounterPresenter.Model(count = count, onIncrement = { count++ })
  ```

- Use `ReturningSaveableStateHolder` and `rememberSaveable` for saveable values. Keep the holder in the parent. Use a different stable, saveable key for each child and remove saved state when a child is gone for good. Restoring after root or process recreation also needs a host that saves and restores the parent state registry.

  ```kotlin
  @OptIn(ExperimentalAppPlatform::class)
  @Composable
  override fun present(input: Destination): BaseModel {
    val holder = rememberReturningSaveableStateHolder()

    return when (input) {
      Destination.First -> holder.SaveableStateProvider(key = "first") {
        val child = remember { createFirstPresenter() }
        child.present(Unit)
      }
      Destination.Second -> holder.SaveableStateProvider(key = "second") {
        val child = remember { createSecondPresenter() }
        child.present(Unit)
      }
    }
  }
  ```

  Inside each child's `present()`:

  ```kotlin
  var count by rememberSaveable { mutableIntStateOf(0) }
  return CounterPresenter.Model(count = count, onIncrement = { count++ })
  ```

For shared state or state that outlives the parent, inject an owner that lives long enough. Use persistent storage for data that must survive closing and reopening the app; a `StateFlow` alone does not save data.

## Build setup

Configure each module that defines, calls, or starts presenters, using the project's existing plugin version and conventions. A dependency's setup does not configure its consumers:

```kotlin
plugins {
  id("software.ralf.app.platform")
}

appPlatform {
  enableMoleculePresenters(true)
}
```

This adds the Compose compiler, runtime, Molecule, presenter API, and test helpers. Don't repeat those dependencies. Enable Compose UI separately when needed.

Bind presenter interfaces to implementations through the app's existing DI setup. For Metro, use `enableMetro(true)`.

In modules that build the app, `addImplModuleDependencies(true)` supplies defaults such as `MoleculeScopeFactory`. Keep `:impl` dependencies out of feature API modules and check that the app's DI graph builds.

The presenter option does not set Android's `isReturnDefaultValues`. Configure it in shared test setup if needed for Android stubs. Use Robolectric only for tests of real Android behavior.

## Tests

Prefer `commonTest`, fakes, and fast, deterministic dependencies. Call `runTest` directly and pass its scope to `presenter.test`:

```kotlin
class CounterPresenterTest {
  @Test
  fun incrementUpdatesTheModel() = runTest {
    CounterPresenterImpl().test(this) {
      val initial = awaitItem()
      assertEquals(0, initial.count)
      initial.onIncrement()
      assertEquals(1, awaitItem().count)
    }
  }
}
```

The helper uses the test's background scope. For inputs, pass `presenter.test(this, input = value)` or an input `StateFlow`.

- Use Turbine's `awaitItem` and `expectNoEvents`. Drive model callbacks, input flows, and fakes; test behavior instead of private methods.
- Test parents with fake children. Check child inputs, chosen models, state changes, and effects.
- Test the default template wrapper, each `ModelDelegate` override, and state changes that select another template variant. These can be headless presenter tests.
- Check state across recomposition, cleanup when children leave, and restored state when they return. Test the real host's save/restore path before claiming support for recreation.
- Use virtual time instead of sleeps. Model emissions can skip intermediate states.

Run focused tests. Compile affected targets and check host and DI setup when dependencies or host code change. Add UI tests only for behavior presenter tests cannot cover. Follow the project's formatting and API checks, and update docs when behavior changes.

Public docs: [presenters](https://vrallev.github.io/app-platform/presenter/), [setup](https://vrallev.github.io/app-platform/setup/), [modules](https://vrallev.github.io/app-platform/module-structure/), [DI](https://vrallev.github.io/app-platform/di/), [renderers](https://vrallev.github.io/app-platform/renderer/), [templates](https://vrallev.github.io/app-platform/template/), and [testing](https://vrallev.github.io/app-platform/testing/).
