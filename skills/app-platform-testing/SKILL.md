---
name: app-platform-testing
description: Test applications built with App Platform. Use for shared fakes, Kotlin Multiplatform unit tests, coroutine and Flow tests, presenter or scope tests, optional Desktop renderer tests, robot modules, and platform integration tests.
---

# App Platform Testing

Use the smallest test that proves the behavior. Keep most feature logic in fast shared tests, and use app integration tests for graph assembly, host lifecycle, platform rendering, and full user journeys.

Follow the consuming project's targets, assertion library, UI test runner, screenshot setup, and DI choice. Examples omit imports and use AssertK for value assertions; resolve unfamiliar APIs from existing code or matching public docs.

## Choose the test layer

| Concern | Default test | Usual location |
| --- | --- | --- |
| Shared logic, repositories, presenters, and model changes | Unit test | `commonTest` |
| App Platform scope lifecycle | Unit test with a test scope | `commonTest` |
| Model-to-UI rendering | Optional renderer or screenshot test | Desktop or another supported UI test source set |
| Full graph behavior without visual checks | Headless integration test | Test-only app assembly |
| Launch, rendering, platform lifecycle, and user journeys | Robot integration test | Platform app test source set |

Put platform behavior in its platform test source set. Keep app-shell tests focused on app-shell wiring. Name tests after product behavior or screens; include a platform name only when the behavior is platform-specific. Do not repeat the same presenter or repository behavior at every layer.

## Fakes

Use a real implementation when it is fast, deterministic, and easy to construct. Otherwise prefer a fake over a mocking framework. Search the repository before adding another fake.

Put a reusable fake in the API owner's `:testing` module, usually in `commonMain`. Keep a one-off fake beside its test. Ordinary production modules add `:testing` only to test configurations; a test-only `:testing` or robot assembly module may use it from its main source set.

Give fakes clear controls and observable results. Keep them deterministic, return valid domain data, and support the failures or delays that consumers handle. Avoid mock-style call scripts that copy implementation details. Reset shared fake state between tests when a test graph outlives one test.

```kotlin
class FakeLocationProvider(
  initialLocation: Location,
) : LocationProvider {
  override val location: StateFlow<Location>
    field = MutableStateFlow(initialLocation)

  fun setLocation(value: Location) {
    location.value = value
  }
}
```

## Unit tests

Construct the unit directly with real values and fakes. Drive its public inputs and assert its public outputs instead of private calls or collaborator call order.

Use `runTest` for coroutine code and its scheduler for delays. Use Turbine when a `Flow` changes over time. Do not use wall-clock sleeps.

```kotlin
@Test
fun routeChangesWithLocation() = runTest {
  val locations = FakeLocationProvider(start)
  val repository = RoutingRepository(locations)

  repository.route.test {
    assertThat(awaitItem()).isEqualTo(startRoute)

    locations.setLocation(destination)
    assertThat(awaitItem()).isEqualTo(destinationRoute)
  }
}
```

For `MoleculePresenter`, pass the current `TestScope` to the App Platform `test` helper. Drive changes through model callbacks, input flows, or fakes. For a counter model with `count` and `onIncrement`:

```kotlin
@Test
fun incrementUpdatesTheModel() = runTest {
  CounterPresenter().test(this) {
    val initial = awaitItem()
    assertThat(initial.count).isEqualTo(0)

    initial.onIncrement()
    assertThat(awaitItem().count).isEqualTo(1)
  }
}
```

For `Scoped` services, prefer `runTestWithScope` or `runTestWithScoped`. Use `Scope.buildTestScope(this)` when an existing `runTest` owns the scheduler. These helpers install a test coroutine owner and clean up the App Platform scope; do not attach a second coroutine owner.

## Optional renderer tests

Dedicated renderer tests are optional. Keep the app's screenshot strategy when it already covers the same visual states. When a Desktop target exists, `runComposeUiTest` is usually the quickest way to render immutable sample models and check meaningful branches or callbacks:

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun showsProgress() = runComposeUiTest {
  setContent {
    LoginRenderer().renderCompose(
      LoginPresenter.Model(loginInProgress = true) {},
    )
  }

  onNodeWithTag("loginProgress").assertIsDisplayed()
}
```

Use `desktopTest` or a shared source set only when every receiving target has compatible UI test support. Android renderer tests that need Android runtime behavior require instrumentation on a device or emulator.

## Robots

Robots expose product actions and observations while hiding tags, labels, test framework calls, fake internals, and platform input APIs. They can interact with UI, control fake scenarios, inspect analytics, or wrap system actions such as back navigation.

Put reusable feature robots in a robot module, usually `:impl-robots`. Put app-level journeys, test graph assembly, and shared fixtures in a test-only app assembly when several tests need them. Keep robot modules off production runtime classpaths.

Annotate discoverable robots with `@ContributesRobot` using the consuming graph's scope key. Keep contributed classes public when generated graphs in other modules use them. Use `ComposeRobot` with `composeRobot<T>()`; use plain `Robot` or `AndroidViewRobot` with `robot<T>()`.

```kotlin
@ContributesRobot(AppScope::class)
class SignInRobot(
  private val scenario: FakeSignInScenario,
) : ComposeRobot() {
  fun failNextSignIn() {
    scenario.failNext()
  }

  fun signIn() {
    compose.onNodeWithTag("signIn").performClick()
  }

  fun seeError() {
    compose.onNodeWithTag("signInError").assertIsDisplayed()
  }
}
```

Each `robot<T>()` or `composeRobot<T>()` call creates a fresh instance and calls `close()` afterward. Keep shared scenario state in an injected fake or scoped state owner, and do not make robots singletons. Robot lookup starts at the root scope and searches children; the same robot type in multiple sibling scopes is ambiguous.

Prefer stable semantics tags and keep selectors inside robots. Move repeated action sequences into named product journeys. Share a journey in `commonMain` when the same behavior is intentionally tested on several hosts, and call it from every intended host suite. Otherwise share the robot vocabulary and keep the scenario platform-specific.

## Robot integration tests

Reuse or extend the app's existing UI test rule or fixture before adding setup. It should build a fresh, production-backed test graph, replace external systems with fakes, start the real root presenter and template path, and render through the real factory. If the behavior can be proved from the template or model stream, use a headless fixture and skip rendering.

Keep platform test classes thin:

```kotlin
@Test
fun failedSignInShowsAnError() = runRobotTest {
  composeRobot<SignInRobot> {
    failNextSignIn()
    signIn()
  }

  waitUntilCatching("sign-in error shown") {
    composeRobot<SignInRobot> { seeError() }
  }
}
```

`runRobotTest` in this example is an app-owned fixture. It should launch the host, make its `RootScopeProvider` available to robot lookup, render the root template when needed, and always tear everything down. Desktop and other hosts without automatic application lookup use the matching `RobotInternals` API for their App Platform version; clear it afterward. Android normally obtains the root scope from the test application. Destroy the app or root scope after every test.

Use UI test synchronization and App Platform wait helpers instead of sleeps. `waitUntilCatching` retries its whole block and blocks the calling thread, so call it off the main thread and put only repeatable observations inside it. Perform clicks and other actions once, outside retry blocks.

Hide platform-only actions behind robot facades when several hosts share a journey. Keep launch rules, device setup, and native host behavior in their platform source sets.

## Build setup

The App Platform plugin recognizes robot module names and adds the base Robot API. Modules with Compose robots also need the project's Compose UI option:

```kotlin
appPlatform {
  enableComposeUi(true)
}
```

Keep `enableModuleStructure(true)` when the project uses App Platform's `:testing` and robot module rules. Keep the project's existing DI integration enabled so `@ContributesRobot` registrations are generated. `enableComposeUi(true)` supplies Compose Robot support, but the app still chooses its UI test runner and screenshot dependencies. `enableMoleculePresenters(true)` supplies the presenter test helper to test source sets, and the public plugin supplies scope test helpers.

Ordinary production modules add `:testing` only to test configurations and robot modules only to UI or integration-test configurations. Test-only `:testing` and robot modules may use `:testing` from main source sets; robot modules may also depend on other robot modules. Compile the final test graph after changing robot contributions or test bindings; compiling the feature alone does not verify graph assembly.

## Run checks

Run the narrowest configured task first, then the platform suites affected by the change.

- KMP libraries often use `testAndroidHostTest`, `desktopTest`, `iosSimulatorArm64Test`, or `allTests`. Inspect the module's targets before choosing.
- `testDebugUnitTest` is usually an Android app-shell task, not the default for a KMP library.
- Use the Desktop app's `desktopTest` for rendered Desktop journeys.
- Use `connectedDebugAndroidTest` or the project's managed-device task for Android activities, Views, back handling, or Compose/View integration.
- Run `checkModuleStructureDependencies` after adding or changing `:testing` or robot dependencies.

Public docs: [testing](https://vrallev.github.io/app-platform/testing/), [module structure](https://vrallev.github.io/app-platform/module-structure/), [presenters](https://vrallev.github.io/app-platform/presenter/), [renderers](https://vrallev.github.io/app-platform/renderer/), and [scopes](https://vrallev.github.io/app-platform/scope/).
