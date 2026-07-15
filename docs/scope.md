# Scope

!!! note

    Importing the `Scopes` API is an opt-in feature through the Gradle DSL. The default value is `false`.
    ```groovy
    appPlatform {
      addPublicModuleDependencies true
    }
    ```

## Overview

Scopes define the boundary our software components operate in. A scope is a space with a well-defined lifecycle
that can be created and torn down. Scopes host other objects and can bind them to their lifecycle. Sub-scopes
or child scopes have the same or a shorter lifecycle as their parent scope.

A leak happens when one scope references another scope with a different lifecycle, e.g. a background thread,
which is started and finishes after a certain amount of time, references an Android `Activity` that is being
destroyed while the thread is still running. In this case the thread with the longer lifecycle leaks the
`Activity` with the shorter lifecycle. Another example is a singleton object, which lives as long as the
application process runs, keeping a strong reference to a user object, which should be released after the
user session expires.

Relying purely on platform specific scopes is problematic, because these scopes are out of our control.
When the platform decides to destroy one of its scopes, then we need to adjust and tear down our operations.
This doesn’t always align with our use cases, e.g. we might want to finish uploading data in the background
after the platform scope such as an `Activity` has been destroyed. Further, the platform scopes may not align
with how we'd represent logical scopes for our apps, e.g. they often lack a user scope. This forces us to
push objects and lifecycles into the application scope and this could cause data to leak across sessions and
trigger out of memory scenarios.

We need to be in charge of our own scopes. In simple terms this means having an object that can be created and
destroyed.

The App Platform provides the
[Scope](https://github.com/vRallev/app-platform/blob/main/scope/public/src/commonMain/kotlin/software/ralf/app/platform/scope/Scope.kt)
interface to implement this concept.

```kotlin title="Scope.kt"
interface Scope {

  val name: String
  val parent: Scope?

  fun buildChild(name: String, builder: (Builder.() -> Unit)? = null): Scope
  fun children(): Set<Scope>

  fun isDestroyed(): Boolean
  fun destroy()

  fun register(scoped: Scoped)
  fun <T : Any> getService(key: String): T?
}
```

## Creating a `Scope`

A `Scope` is created through the builder function. The
[Builder](https://github.com/vRallev/app-platform/blob/main/scope/public/src/commonMain/kotlin/software/ralf/app/platform/scope/Scope.kt#L57)
allows you to add services before the Scope is finalized:

```kotlin
val rootScope = Scope.buildRootScope {
  addService("key", service)
}
```

Child scopes are created using the parent:

```kotlin
rootScope.buildChild("user scope") {
  addService("child-service", childService)
}
```

??? example "Sample"

    The root scope is usually created when the application is launched. The sample application creates its
    root scope [here](https://github.com/vRallev/app-platform/blob/main/sample/app/src/commonMain/kotlin/software/ralf/app/platform/sample/DemoApplication.kt).
    This `Scope` is never destroyed and stays alive for the entire app lifetime.

    The sample application has a child scope for the logged in user. This `Scope` is created during
    [login](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/user/UserManagerImpl.kt#L47-L52)
    and [destroyed](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/user/UserManagerImpl.kt#L68)
    during logout.

    ```kotlin
    override fun login(userId: Long) {
      ...
      val userComponent = userComponentFactory.createUserComponent(user)

      val userScope =
        rootScopeProvider.rootScope.buildChild("user-$userId") {
          addKotlinInjectComponent(userComponent)
          addCoroutineScopeScoped(userComponent.userScopeCoroutineScopeScoped)
        }

      ...

      userScope.register(userComponent.userScopedInstances)
    }

    override fun logout() {
      val currentUserScope = user.value?.scope
      ...
      currentUserScope?.destroy()
    }
    ```

Tests usually leverage the test scope, which comes with better defaults for services such as the coroutine scope:

```kotlin
@Test
fun `my test`() = runTest {
  val scope = Scope.buildTestScope(this)
}

// Or
@Test
fun `my test`() = runTestWithScope { scope ->
  // `scope` is equivalent to calling `Scope.buildTestScope(this)`.
}
```

??? example "Sample"

    Classes implementing the `Scoped` interface usually make use of the `runTestWithScope` function in their tests.
    Notice in [this sample](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonTest/kotlin/software/ralf/app/platform/sample/user/SessionTimeoutTest.kt#L36-L48)
    how `SessionTimeout`, which implements the `Scoped` interface, is registered in the `Scope`.

    ```kotlin hl_lines="7"
    @Test
    fun `on timeout the user is logged out`() = runTestWithScope { scope ->
      val userManager = FakeUserManager()
      userManager.login(1L)

      val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
      scope.register(sessionTimeout)

      assertThat(userManager.user.value).isNotNull()

      advanceTimeBy(SessionTimeout.initialTimeout + 1.milliseconds)
      assertThat(userManager.user.value).isNull()
    }
    ```

## Services

A scope can host other objects like an object graph from dependency injection frameworks and a coroutine scope.
The latter is especially helpful, because the coroutine scope can be canceled when our logical scope is destroyed
and all pending operations are torn down. Connecting our scopes with the dependency injection components makes
our dependency injection setup more flexible, because we’re in charge of instantiating components and can provide
extra objects like a user ID to the object graph. When a scope is destroyed we release the dependency injection
component and the memory can be reclaimed by the runtime. DI components and subcomponents form a tree, therefore
subcomponents can inject all types that are provided by parent components. The strong recommendation is to align
the component tree with the scope hierarchy.

While a service can be obtained through the `getService()` function, a more frequent pattern is to rely on
extension functions for stronger types. Similarly, an extension function on the `Builder` allows us to add a service
to a `Scope`.

```kotlin
interface MyService

private const val MY_SERVICE_KEY = "myService"

fun Scope.Builder.addMyService(service: MyService) {
  addService(MY_SERVICE_KEY, service)
}

fun Scope.myService(): MyService {
  return checkNotNull(getService<MyService>(MY_SERVICE_KEY))
}
```

The App Platform comes with a coroutine scope service and an integration for
[Metro](https://zacsweers.github.io/metro) and
[kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil) as dependency injection
frameworks. Metro is the recommended default.

```kotlin
val rootScope = Scope.buildRootScope {
  addCoroutineScopeScoped(coroutineScope)
  addMetroDependencyGraph(metroDependencyGraph)
  addKotlinInjectComponent(kotlinInjectComponent)
}

// Obtain service.
rootScope.coroutineScope()
rootScope.metroDependencyGraph<DefGraph>()
rootScope.kotlinInjectComponent<AbcComponent>()
```

!!! warning

    `Scopes` through their service mechanism implement the service locator pattern. With the provided dependency
    injection framework usually it’s not needed to add custom services and it’s better to rely on dependency
    injection instead.

### `CoroutineScope`

!!! info

    By default, the IO dispatcher is used for all launched jobs for the provided `CoroutineScope`.

    In tests when using `Scope.buildTestScope()` or `runTestWithScope` the `backgroundScope` is from the `TestScope`
    is used by default and added to `Scope` instance.

It's strongly recommended to add a `CoroutineScope` to each `Scope`. App Platform provides a `CoroutineScope`
[by default for the `AppScope`](https://github.com/vRallev/app-platform/blob/main/kotlin-inject/impl/src/commonMain/kotlin/software/ralf/app/platform/scope/coroutine/AppScopeCoroutineScopeComponent.kt).
It is important to register this `CoroutineScope` in the created app `Scope` instance in order to cancel the
`CoroutineScope` in case the `AppScope` ever gets destroyed. The same applies to any child scope.

=== "Metro"

    ```kotlin
    @DependencyGraph(AppScope::class)
    interface AppGraph {
      /** The coroutine scope that runs as long as the app scope is alive. */
      @ForScope(AppScope::class) val appScopeCoroutineScopeScoped: CoroutineScopeScoped // (1)!
    }
    
    fun createAppScope(appGraph: AppGraph): Scope {
      return Scope.buildRootScope {
        addMetroDependencyGraph(appGraph)
        addCoroutineScopeScoped(appGraph.appScopeCoroutineScopeScoped)
      }
    }
    ```

    1.  `CoroutineScopeScoped` wraps a `CoroutineScope` in a `Scoped` instance. In `onExitScope()` of this instance the
        `CoroutineScope` will be canceled.

=== "kotlin-inject-anvil"

    ```kotlin
    @SingleIn(AppScope::class)
    @MergeComponent(AppScope::class)
    interface AppComponent {
      /** The coroutine scope that runs as long as the app scope is alive. */
      @ForScope(AppScope::class) val appScopeCoroutineScopeScoped: CoroutineScopeScoped // (1)!
    }
    
    fun createAppScope(appComponent: AppComponent): Scope {
      return Scope.buildRootScope {
        addKotlinInjectComponent(appComponent)
        addCoroutineScopeScoped(appComponent.appScopeCoroutineScopeScoped)
      }
    }
    ```

    1.  `CoroutineScopeScoped` wraps a `CoroutineScope` in a `Scoped` instance. In `onExitScope()` of this instance the
        `CoroutineScope` will be canceled.


The `CoroutineScope` can be injected in classes and used to launch async work. A common pattern is to use the
`onEnterScope()` function when implementing the `Scoped` interface to launch coroutine jobs:

```kotlin
override fun onEnterScope(scope: Scope) {
  // This job will be automatically canceled when the `scope` gets destroyed.
  scope.launch { // (1)!
    someFlow.collect {
      ...
    }
  }
}
```

1.  `scope.launch` is a convenience function for `scope.coroutineScope().launch`.

Since the `CoroutineScope` is part of the Metro or `kotlin-inject-anvil` object graph, the `CoroutineScope` can be
injected in the constructor as well:

```kotlin
@Inject
@SingleIn(AppScope::class)
class MyClass(@ForScope(AppScope::class) coroutineScope: CoroutineScope) {
  init {
    coroutineScope.launch {
      ...
    }
  }
}
```

Whenever a `CoroutineScope` is injected, a new child `CoroutineScope` with its own `Job` is created (the parent `Job`
points to the shared `CoroutineScope` `Job`). The prevents consumers from accidentally tearing down all running
coroutines when canceling an injected `CoroutineScope`.

```kotlin
override fun onEnterScope(scope: Scope) {
  val myCoroutineScope = scope.coroutineScope()

  myCoroutineScope.launch { ... }
  myCoroutineScope.launch { ... }

  // This is safe to do and only cancels the two launched jobs and `myCoroutineScope`. It doesn't cancel the
  // shared `CoroutineScope` hosted within the `scope` object.
  myCoroutineScope.cancel()
}
```

## `Scoped`

Service objects can tie themselves to the lifecycle of a scope by implementing the
[`Scoped`](https://github.com/vRallev/app-platform/blob/main/scope/public/src/commonMain/kotlin/software/ralf/app/platform/scope/Scoped.kt)
interface:

```kotlin
interface Scoped {
    fun onEnterScope(scope: Scope)
    fun onExitScope()
}
```

Usually, we rely on our dependency injection framework to instantiate all `Scoped` instances for a scope. By doing
so service objects will be automatically created when their corresponding scope is created and receive a callback
when their scope is destroyed. This helps with loose coupling between our service objects. Implementing the `Scoped`
interface is a detail, which doesn’t need to be exposed to the API layer:

```kotlin hl_lines="5 6 7"
interface LocationProvider {
  val location: StateFlow<Location>
}

class AndroidLocationProvider(
  private val locationManager: LocationManager
) : LocationProvider, Scoped {

  private val _location = MutableStateFlow<Location>()
  override val location get() = _location

  override fun onEnterScope(scope: Scope) {
    scope.launch {
      // Observe location updates through LocationManager

      val androidLocation = ...
      _location.value = androidLocation
    }
  }
}
```

!!! note

    Note in the example that the concrete implementation class implements the `Scoped` interface and
    not `LocationProvider`. Being lifecycle aware is an implementation detail.

How the `Scoped` object is instantiated depends on the dependency injection framework and which scope to use.
With Metro, or alternatively `kotlin-inject-anvil`, for the app scope it would be:

=== "Metro"

    ```kotlin
    @SingleIn(AppScope::class) // (1)!
    @ContributesScoped(AppScope::class) // (2)!
    class AndroidLocationProvider(
      ...
    ) : LocationProvider, Scoped {
      ...
    }
    ```

    1.  This annotation ensures that there is only ever a single instance of `AndroidLocationProvider` in the `AppScope`.
    2.  This annotation ensures that when somebody injects `LocationProvider`, then they get the singleton instance of `AndroidLocationProvider`.

    ??? note "`@ContributesScoped` will generate and contribute bindings"

        The `@ContributesScoped` annotation will generate a binding container with bindings for `LocationProvider`
        and `Scoped`. The generated container will be added automatically to the `AppScope`. No further manual step
        is needed.

        ```kotlin
        @ContributesTo(AppScope::class)
        @BindingContainer
        @Origin(AndroidLocationProvider::class)
        interface AndroidLocationProviderContribution {
          @Binds
          fun bindAndroidLocationProvider(instance: AndroidLocationProvider): LocationProvider

          @Binds
          @IntoSet
          @ForScope(AppScope::class)
          fun bindAndroidLocationProviderScoped(instance: AndroidLocationProvider): Scoped

          companion object {
            @Provides
            fun provideAndroidLocationProvider(
              locationManager: LocationManager
            ): AndroidLocationProvider = AndroidLocationProvider(locationManager)
          }
        }
        ```

        If the class has an `@Inject` constructor, the generated constructor provider is skipped
        and only the bindings are generated. If it has multiple constructors, annotate the
        constructor to use with `@Inject`.

=== "kotlin-inject-anvil"

    ```kotlin
    @Inject // (1)!
    @SingleIn(AppScope::class) // (2)!
    @ContributesBinding(AppScope::class) //(3)!
    class AndroidLocationProvider(
      ...
    ) : LocationProvider, Scoped {
      ...
    }
    ```

    1.  This annotation is required to support constructor injection.
    2.  This annotation ensures that there is only ever a single instance of `AndroidLocationProvider` in the `AppScope`.
    3.  This annotation ensures that when somebody injects `LocationProvider`, then they get the singleton instance of `AndroidLocationProvider`.

    ??? note "`@ContributesBinding` will generate and contribute bindings"

        The `@ContributesBinding` annotation will generate a component interface with bindings for `LocationProvider`
        and `Scoped`. The generated interface will be added automatically to the `AppScope`. No further manual step
        is needed.

        ```kotlin
        @Provides
        public fun provideAndroidLocationProvider(androidLocationProvider: AndroidLocationProvider): LocationProvider = androidLocationProvider

        @Provides
        @IntoSet
        @ForScope(AppScope::class)
        fun provideAndroidLocationProviderScoped(androidLocationProvider: AndroidLocationProvider): Scoped = androidLocationProvider
        ```


??? example "Sample"

    Another example in the sample app is [`SessionTimeout`](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/user/SessionTimeout.kt).
    This class is part of the `UserScope` and implements the `Scoped` interface. `onEnterScope()` will be called when
    the user logs in and `onExitScope()` when the user logs out.

    ```kotlin
    @SingleIn(UserScope::class)
    @ContributesScoped(UserScope::class) // Use @ContributesBinding with kotlin-inject-anvil.
    class SessionTimeout(...) : Scoped {

      override fun onEnterScope(scope: Scope) {
        // This job will be automatically canceled when the user logs out and the user scope is
        // destroyed.
        scope.launch {
          while (userManager.user.value != null) {
            ...
          }
        }

        scope.launch {
          ...
        }
      }
    }
    ```

### Registering `Scoped`

The dependency injection frameworks like Metro and `kotlin-inject-anvil` are only responsible for creating `Scoped`
instances, but don't automatically register them in the `Scope`. This has to be done whenever the `Scope` is created:

=== "Metro"

    ```kotlin hl_lines="4 16"
    @DependencyGraph(AppScope::class)
    interface AppGraph {
      /** All [Scoped] instances part of the app scope. */
      @ForScope(AppScope::class) val appScopedInstances: Set<Scoped>
    }
    
    fun createAppScope(appGraph: AppGraph): Scope {
      val rootScope =
        Scope.buildRootScope {
          addMetroDependencyGraph(appGraph)
    
          addCoroutineScopeScoped(appGraph.appScopeCoroutineScopeScoped)
        }
    
      rootScope.register(appGraph.appScopedInstances)
    
      return rootScope
    }
    ```
    
    By calling `appGraph.appScopedInstances` the DI framework instantiates all `Scoped` instances part of the
    `AppScope`. The `rootScope.register(...)` call will register all of the `Scoped` instances and invoke
    `onEnterScope(scope)`. When calling `rootScope.destroy()` later at some point, then `onExitScope()` will be
    called for all `Scoped` instances.

=== "kotlin-inject-anvil"

    ```kotlin hl_lines="5 16"
    @SingleIn(AppScope::class)
    @MergeComponent(AppScope::class)
    interface AppComponent {
      /** All [Scoped] instances part of the app scope. */
      @ForScope(AppScope::class) val appScopedInstances: Set<Scoped>
    }
    
    fun createAppScope(appComponent: AppComponent): Scope {
      val rootScope =
        Scope.buildRootScope {
          addKotlinInjectComponent(appComponent)
    
          addCoroutineScopeScoped(appComponent.appScopeCoroutineScopeScoped)
        }
    
      rootScope.register(appComponent.appScopedInstances)
    
      return rootScope
    }
    ```
    
    By calling `appComponent.appScopedInstances` the DI framework instantiates all `Scoped` instances part of the
    `AppScope`. The `rootScope.register(...)` call will register all of the `Scoped` instances and invoke
    `onEnterScope(scope)`. When calling `rootScope.destroy()` later at some point, then `onExitScope()` will be
    called for all `Scoped` instances.


??? example "Sample"

    The sample application implements this mechanism for the
    [`AppScope`](https://github.com/vRallev/app-platform/blob/main/sample/app/src/commonMain/kotlin/software/ralf/app/platform/sample/DemoApplication.kt#L31-L33)
    and the [`UserScope`](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/user/UserManagerImpl.kt#L58-L60).

### `onExit`

The convenience function `onExit` is handy when you want to create objects lazily within `onEnterScope()` and
not create a property in the class itself. This callback notifies you when the `Scope` is destroyed similar to
`onExitScope()`.

=== "Metro"

    ```kotlin
    @SingleIn(AppScope::class)
    @ContributesScoped(AppScope::class)
    class MyClass(private val application: Application) : Scoped {
    
      override fun onEnterScope(scope: Scope) {
        val receiver = object : BroadcastReceiver()
    
        application.registerReceiver(receiver, Intent())
    
        scope.onExit {
          // This function is invoked when the scope gets destroyed.
          application.unregisterReceiver(receiver)
        }
      }
    }
    ```

=== "kotlin-inject-anvil"

    ```kotlin
    @Inject
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class MyClass(private val application: Application) : Scoped {
    
      override fun onEnterScope(scope: Scope) {
        val receiver = object : BroadcastReceiver()
    
        application.registerReceiver(receiver, Intent())
    
        scope.onExit {
          // This function is invoked when the scope gets destroyed.
          application.unregisterReceiver(receiver)
        }
      }
    }
    ```


### Threading

Which thread is used for calling `onEnterScope()` and `onExitScope()` is an implementation detail of the scope
owner when calling `scope.register(Scoped)`. Usually, the app scope is created as soon as possible when the
application launches and therefore the main thread is used. Child scopes may use the main thread or a background
thread.

To safely launch long running work or blocking tasks it’s recommended to use the coroutine scope provided by the
`Scope`:

```kotlin
override fun onEnterScope(scope: Scope) {
  scope.launch { ... }
}
```

Clean up routines in `onExitScope()` must be blocking, otherwise these tasks live longer than the `Scope` and
therefore may cause a leak (thread and memory) and potential race conditions. It’s strongly recommended not to
launch any asynchronous work within `onExitScope()`. By the time `onExitScope()` is called, the coroutine
scope provided by the `Scope` has been canceled already.

## Hosting `Scopes`

Scopes need to be remembered and must be accessible in order to get access to their services. Where to host scopes
depends on what scopes are required and when they need to be created. Most apps have some form of an application
scope, which is a singleton scope for the entire lifetime of the application. A natural place to host this scope
for Android apps is within the `Application` class, for iOS apps within `App` struct or the main function
for desktop applications.

A user scope has a shorter lifecycle than the application scope, but usually lives longer than UI components.
It is commonly hosted by a service object managing the login state. This scope is destroyed after the user
session expires.

App Platform by default only provides the `AppScope`, which has to be manually created by each application as
highlighted above.

??? example "Sample"

    The sample application has a common class [DemoApplication](https://github.com/vRallev/app-platform/blob/main/sample/app/src/commonMain/kotlin/software/ralf/app/platform/sample/DemoApplication.kt)
    that is responsible for creating the app scope. The Android app instantiates `DemoApplication` in the
    [`Application` class](https://github.com/vRallev/app-platform/blob/main/sample/app/src/androidMain/kotlin/software/ralf/app/platform/sample/AndroidApplication.kt#L19).
    The iOS sample creates the `DemoApplication` in the [`UIApplicationDelegate`](https://github.com/vRallev/app-platform/blob/main/sample/iosApp/iosApp/iOSApp.swift#L6).
    On Desktop `DemoApplication` is created part of the [`main()` function](https://github.com/vRallev/app-platform/blob/main/sample/app/src/desktopMain/kotlin/software/ralf/app/platform/sample/Main.kt#L8).

### `RootScopeProvider`

[`RootScopeProvider`](https://github.com/vRallev/app-platform/blob/main/scope/public/src/commonMain/kotlin/software/ralf/app/platform/scope/RootScopeProvider.kt),
as the name suggests, gives access to the root `Scope` ("AppScope"). Usually, this interface is implemented by the application
object of the individual platform to get access to the root `Scope` from a platform context, e.g. on Android this is
handy in an `Activity`:

```kotlin
class MainActivity : Activity() {

  private val rootScopeProvider
    get() = application as RootScopeProvider

  ...
}
```

??? example "Sample"

    The sample application implements `RootScopeProvider` in the Android
    [`Application` class](https://github.com/vRallev/app-platform/blob/main/sample/app/src/androidMain/kotlin/software/ralf/app/platform/sample/AndroidApplication.kt#L19)
    and the iOS [`UIApplicationDelegate`](https://github.com/vRallev/app-platform/blob/main/sample/iosApp/iosApp/iOSApp.swift#L6).
    On Desktop there is no concept of a singleton application object by default, but in the sample app we created an
    equivalent with [`DesktopApp`](https://github.com/vRallev/app-platform/blob/main/sample/app/src/desktopMain/kotlin/software/ralf/app/platform/sample/DesktopApp.kt).
