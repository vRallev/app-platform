---
name: app-platform-scope
description: Manage App Platform scopes, coroutine lifetimes, and Metro graph integration. Use when creating root or child scopes, registering Scoped services, attaching CoroutineScopeScoped owners, connecting Metro graphs to scopes, or testing teardown.
---

# App Platform Scopes

An App Platform `Scope` is an app-owned logical lifetime. It owns child scopes, lifecycle callbacks, and framework services, and can own coroutine cancellation. It works with or without dependency injection.

Follow the consuming project's App Platform version, module layout, and DI setup. Examples omit imports; resolve unfamiliar APIs from existing code or matching public docs.

## App Platform `Scope`

Before creating a scope, identify what owns it, what starts and ends it, and which shorter lifetimes are its children. One owner should create the scope, destroy it, and release its reference.

Create the root with `Scope.buildRootScope()` and shorter lifetimes with `buildChild()`:

```kotlin
val appScope = Scope.buildRootScope("app")
val sessionScope = appScope.buildChild("session-$sessionId")

sessionScope.destroy()
appScope.destroy()
```

A child cannot outlive its parent. Destroying a parent destroys all children first. Do not keep a child scope, its graph, or its objects in a longer-lived owner. Most operations on a destroyed scope fail; use `isDestroyed()` to guard stale references and update related state in a deliberate order.

Expose the app scope through `RootScopeProvider` when platform entry points or longer-lived coordinators need it. Keep scope owners small. Use the scope service map for App Platform integrations with typed helpers; use DI for application dependencies.

### Lifecycle callbacks

Implement `Scoped` on concrete services that need start or cleanup callbacks. Keep it out of their public interfaces:

```kotlin
class EventObserver(
  private val events: Events,
  private val onEvent: (Event) -> Unit,
) : Scoped {
  private var registration: Registration? = null

  override fun onEnterScope(scope: Scope) {
    registration = events.observe(onEvent)
  }

  override fun onExitScope() {
    registration?.close()
    registration = null
  }
}

scope.register(EventObserver(events) { /* Handle the event. */ })
```

`register()` calls `onEnterScope()` immediately. `destroy()` later calls `onExitScope()`. Use `scope.onExit { ... }` for a small resource created locally.

Callbacks have no thread guarantee. Keep `onExitScope()` synchronous and blocking. Do not depend on callback order between ordinary `Scoped` objects. A crash or process death does not call `onExitScope()`, so save durable state earlier.

## Coroutine scopes

Attach one `CoroutineScopeScoped` to every App Platform scope that runs coroutine work. Coroutine services are local: a child App Platform scope does not inherit its parent's coroutine owner.

```kotlin
val coroutineOwner = CoroutineScopeScoped(
  ioDispatcher + SupervisorJob() + CoroutineName("SessionScope")
)

val sessionScope = appScope.buildChild("session-$sessionId") {
  addCoroutineScopeScoped(coroutineOwner)
}

sessionScope.launch {
  repository.sync()
}

val uploadScope = sessionScope.coroutineScope(CoroutineName("Upload"))
```

`CoroutineScopeScoped` needs a `CoroutineName` and an owning job. `addCoroutineScopeScoped()` stores and registers it. Destroying the App Platform scope cancels it.

`scope.launch()` and `scope.coroutineScope()` create child coroutine scopes.

Teardown runs in this order:

1. Child App Platform scopes are destroyed.
2. Registered `CoroutineScopeScoped` owners are canceled.
3. Other `Scoped` objects receive `onExitScope()`.

Start long-running work with `scope.launch` in `onEnterScope()`. By `onExitScope()`, the coroutine owner has already been canceled; use that callback only for synchronous cleanup.

## Metro object graphs and App Platform integration

A Metro scope key and an App Platform `Scope` are separate. Metro controls object reuse inside a graph. App Platform controls the logical lifetime and teardown. The scope owner must align them.

Enable Metro through the consuming project's App Platform plugin:

```kotlin
appPlatform {
  enableMetro(true)
}
```

Build final graphs at the app assembly boundary, often in platform source sets when they need platform values. Match one graph lifetime to one App Platform scope lifetime.

### Attach and register a graph

The graph must expose its App Platform lifecycle set and coroutine owner. The names can follow the project:

```kotlin
@ContributesTo(AppScope::class)
interface AppGraph {
  @ForScope(AppScope::class)
  @Multibinds(allowEmpty = true)
  val scopedInstances: Set<Scoped>

  @ForScope(AppScope::class)
  val coroutineOwner: CoroutineScopeScoped
}
```

Attach the graph and coroutine owner while building the App Platform scope. Publish the scope before registering lifecycle objects, because `register()` invokes callbacks immediately:

```kotlin
class AppScopeOwner : RootScopeProvider {
  private var currentScope: Scope? = null

  override val rootScope: Scope
    get() = checkNotNull(currentScope)

  fun start(graph: AppGraph) {
    check(currentScope == null)

    val scope = Scope.buildRootScope("app") {
      addMetroDependencyGraph(graph)
      addCoroutineScopeScoped(graph.coroutineOwner)
    }
    currentScope = scope
    scope.register(graph.scopedInstances)
  }

  fun close() {
    val scope = currentScope ?: return
    scope.destroy()
    currentScope = null
  }
}
```

Metro creates `Scoped` objects but does not register them with App Platform. Repeat this sequence for custom child graphs: create the graph, build and publish its child `Scope`, attach its graph and coroutine owner, then register its `Set<Scoped>`.

The scope builder has one Metro graph service slot. If it receives more than one graph while building, it keeps the last, so the final graph should expose every narrow graph surface needed for that lifetime.

`scope.metroDependencyGraph<T>()` checks the current scope and then its parents. It never checks children. Prefer constructor injection within the graph; use graph lookup at platform-created entry points and scope boundaries.

## App Platform Metro features

At app assembly points, `addImplModuleDependencies(true)` adds App Platform's default Metro bindings:

- IO, default, and main coroutine dispatchers.
- The `AppScope` `CoroutineScopeScoped` and fresh child `CoroutineScope` values.
- `@PresenterCoroutineScope`, based on the app scope and main dispatcher.

The root owner must still install the graph's coroutine owner with `addCoroutineScopeScoped()`. Custom child graphs need their own qualified owner and injectable children:

```kotlin
@Provides
@SingleIn(SessionScope::class)
@ForScope(SessionScope::class)
fun provideCoroutineOwner(
  @IoCoroutineDispatcher dispatcher: CoroutineDispatcher,
): CoroutineScopeScoped {
  return CoroutineScopeScoped(
    dispatcher + SupervisorJob() + CoroutineName("SessionScope")
  )
}

@Provides
@ForScope(SessionScope::class)
fun provideCoroutineScope(
  @ForScope(SessionScope::class) owner: CoroutineScopeScoped,
): CoroutineScope = owner.createChild()
```

Use App Platform's `@ContributesScoped` for a Metro-created lifecycle service. Pair it with `@SingleIn` for the same lifetime so injection and lifecycle registration use the same object:

```kotlin
interface SessionObserver

@SingleIn(SessionScope::class)
@ContributesScoped(SessionScope::class)
class DefaultSessionObserver(
  private val repository: SessionRepository,
) : SessionObserver, Scoped
```

Do not also add `@ContributesBinding`. `@ContributesScoped` adds the qualified `Scoped` set entry. If the class has one direct supertype besides `Scoped`, it binds that type too. A contributed class may have at most one such supertype.

App Platform's Metro compiler support also handles `@ContributesRenderer` and `@ContributesRobot`. Their factories own instance lifetimes, so do not make renderers or robots Metro singletons. Use their dedicated guidance for registration and factories. For generated contributions, one constructor needs no `@Inject`; when there are several, mark the selected constructor. Keep contributed classes public when graphs in other modules use them.

Use Metro's documentation for graph factories, bindings, and aggregation. This skill covers only the App Platform integration.

## Tests

Prefer `runTestWithScope`. It adds a test-scheduler-backed coroutine owner and always destroys the scope:

```kotlin
@Test
fun startsWork() = runTestWithScope { scope ->
  val service = SessionService()
  scope.register(service)

  runCurrent()
  // Assert behavior started by onEnterScope().
}
```

Use `runTestWithScoped(service)` for one prebuilt lifecycle service. Use `Scope.buildTestScope(this)` inside an existing `runTest`. Drive coroutine work with the test scheduler instead of sleeps.

Test scopes already have a coroutine owner; do not attach a second one. For graph integration, exercise the production scope owner with a test-dispatcher-backed graph so graph attachment, registration, and teardown run together.

Compile the final app graph and affected targets after changing contributed lifecycle services. A feature-module compile alone does not verify graph assembly.

Public docs: [scopes](https://vrallev.github.io/app-platform/scope/), [DI](https://vrallev.github.io/app-platform/di/), [renderers](https://vrallev.github.io/app-platform/renderer/), [setup](https://vrallev.github.io/app-platform/setup/), [module structure](https://vrallev.github.io/app-platform/module-structure/), and [testing](https://vrallev.github.io/app-platform/testing/). Use the [Metro documentation](https://zacsweers.github.io/metro/latest/) for Metro APIs.
