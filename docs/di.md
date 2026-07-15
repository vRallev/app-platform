# DI Framework

!!! note

    App Platform provides support for [Metro](https://zacsweers.github.io/metro) and
    [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil) as dependency injection
    frameworks. Metro is the recommended default, while `kotlin-inject-anvil` remains available as
    the alternative and for existing codebases. Both frameworks are compile-time injection
    frameworks and ready for Kotlin Multiplatform. They verify correctness of the object graph at
    build time and avoid crashes at runtime.

    Enabling dependency injection is an opt-in feature through the Gradle DSL. The default value is `false`.
    ```groovy
    appPlatform {
      enableMetro true
      enableKotlinInject true
    }
    ```

!!! tip

    Start with the [Metro documentation](https://zacsweers.github.io/metro). Reach for the
    [kotlin-inject-anvil documentation](https://github.com/amzn/kotlin-inject-anvil) when you are
    maintaining the alternative path or migrating older code. App Platform makes heavy use of
    `@ContributesBinding` and `@ContributesTo` annotations to decompose and assemble components /
    object graphs.

## Metro

!!! note

    Metro is an opt-in feature through the Gradle DSL. The default value is `false`.
    ```groovy
    appPlatform {
      enableMetro true
    }
    ```

### Dependency graph

Dependency graphs are added as a service to the `Scope` class and can be obtained using the `metroDependencyGraph()`
extension function:

```kotlin
scope.metroDependencyGraph<AppGraph>()
```

In modularized projects, final graphs are defined in the `:app` modules, because the object graph has to
know about all features of the app. It is strongly recommended to create an object graph in each platform specific
folder to provide platform specific types.

=== "Android"

    ```kotlin title="androidMain"
    @DependencyGraph(AppScope::class)
    interface AndroidAppGraph {
      @DependencyGraph.Factory
      fun interface Factory {
        fun create(
          @Provides application: Application,
          @Provides rootScopeProvider: RootScopeProvider,
        ): AndroidAppGraph
      }
    }
    ```

=== "iOS"

    ```kotlin title="iosMain"
    @DependencyGraph(AppScope::class)
    interface IosAppGraph {
      @DependencyGraph.Factory
      fun interface Factory {
        fun create(
          @Provides uiApplication: UIApplication,
          @Provides rootScopeProvider: RootScopeProvider,
        ): IosAppGraph
      }
    }
    ```

=== "Desktop"

    ```kotlin title="desktopMain"
    @DependencyGraph(AppScope::class)
    interface DesktopAppGraph {
      @DependencyGraph.Factory
      fun interface Factory {
        fun create(@Provides rootScopeProvider: RootScopeProvider): DesktopAppGraph
      }
    }
    ```

=== "WasmJs"

    ```kotlin title="wasmJsMain"
    @DependencyGraph(AppScope::class)
    interface WasmJsAppGraph {
      @DependencyGraph.Factory
      fun interface Factory {
        fun create(@Provides rootScopeProvider: RootScopeProvider): WasmJsAppGraph
      }
    }
    ```

### Platform implementations

Metro makes it simple to provide platform specific implementations for abstract APIs without needing
to use `expect / actual` declarations or any specific wiring. Since the final object graphs live in the platform
specific source folders, all contributions for a platform are automatically picked up. Platform specific
implementations can use and inject types from the platform.

```kotlin title="commonMain"
interface LocationProvider
```

=== "Android"

    ```kotlin title="androidMain"
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class AndroidLocationProvider(
      val application: Application,
    ) : LocationProvider
    ```

=== "iOS"

    ```kotlin title="iosMain"
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class IosLocationProvider(
      val uiApplication: UIApplication,
    ) : LocationProvider
    ```

=== "Desktop"

    ```kotlin title="desktopMain"
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class DesktopLocationProvider(
      ...
    ) : LocationProvider
    ```

=== "WasmJs"

    ```kotlin title="wasmJsMain"
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class WasmLocationProvider(
      ...
    ) : LocationProvider
    ```

Other common code within `commonMain` can safely inject and use `LocationProvider`.

### Injecting dependencies

It's recommended to rely on constructor injection as much as possible, because it removes boilerplate and makes
testing easier. But it some cases it's required to get a dependency from an object graph where constructor injection
is not possible, e.g. in a static context or types created by the platform. In this case a contributed object graph
interface with access to the `Scope` help:

```kotlin title="androidMain"
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

  private val graph = (application as RootScopeProvider).rootScope.metroDependencyGraph<Graph>()
  private val templateProvider = graph.templateProviderFactory.createTemplateProvider()

  @ContributesTo(AppScope::class)
  interface Graph {
    val templateProviderFactory: TemplateProvider.Factory
  }
}
```

This sample shows an Android `ViewModel` that doesn't use constructor injection. Instead, the `Scope` is retrieved
from the `Application` class and the Metro object graph is found through the `metroDependencyGraph()` function.

??? example "Sample"

    The `ViewModel` example comes from the [sample app](https://github.com/vRallev/app-platform/blob/main/sample/app/src/androidMain/kotlin/software/ralf/app/platform/sample/MainActivityViewModel.kt).
    `ViewModels` can use constructor injection, but this requires more setup. This approach of using a graph
    interface was simpler and faster.

    Another example where this approach is handy is in [`NavigationPresenterImpl`](https://github.com/vRallev/app-platform/blob/main/sample/navigation/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/navigation/NavigationPresenterImpl.kt).
    This class waits for the user scope to be available and then optionally retrieves the `Presenter` that is part
    of the user graph. Constructor injection cannot be used, because `NavigationPresenterImpl` is part of the app
    scope and cannot inject dependencies from the user scope, which is a child scope of app scope. This would violate
    dependency inversion rules.

    ```kotlin hl_lines="17"
    @ContributesTo(UserScope::class)
    interface UserGraph {
      val userPresenter: UserPagePresenter
    }

    @Composable
    override fun present(input: Unit): BaseModel {
      val scope = getUserScope()
      if (scope == null) {
        // If no user is logged in, then show the logged in screen.
        val presenter = remember { loginPresenter() }
        return presenter.present(Unit)
      }

      // A user is logged in. Use the user graph to get an instance of UserPagePresenter, which is only
      // part of the user scope.
      val userPresenter = remember(scope) { scope.metroDependencyGraph<UserGraph>().userPresenter }
      return userPresenter.present(Unit)
    }
    ```

### Default bindings

App Platform provides a few defaults that can be injected, including a `CoroutineScope` and `CoroutineDispatchers`.

```kotlin
@Inject
class SampleClass(
  @ForScope(AppScope::class) appScope: CoroutineScope,

  @IoCoroutineDispatcher ioDispatcher: CoroutineDispatcher,
  @DefaultCoroutineDispatcher defaultDispatcher: CoroutineDispatcher,
  @MainCoroutineDispatcher mainDispatcher: CoroutineDispatcher,
)
```

!!! info "CoroutineScope"

    The `CoroutineScope` uses the IO dispatcher by default. The qualifier `@ForScope(AppScope::class)` is needed to
    allow other scopes to have their own `CoroutineScope`. For example, the sample app provides a `CoroutineScope`
    [for the user scope](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/user/UserGraph.kt),
    which gets canceled when the user scope gets destroyed. The `CoroutineScope` for the user scope uses the qualifier
    `@ForScope(UserScope::class)

    ```kotlin
    /**
     * Provides the [CoroutineScopeScoped] for the user scope. This is a single instance for the user
     * scope.
     */
    @Provides
    @SingleIn(UserScope::class)
    @ForScope(UserScope::class)
    fun provideUserScopeCoroutineScopeScoped(
      @IoCoroutineDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScopeScoped {
      return CoroutineScopeScoped(dispatcher + SupervisorJob() + CoroutineName("UserScope"))
    }

    /**
     * Provides the [CoroutineScope] for the user scope. A new child scope is created every time an
     * instance is injected so that the parent cannot be canceled accidentally.
     */
    @Provides
    @ForScope(UserScope::class)
    fun provideUserCoroutineScope(
      @ForScope(UserScope::class) userScopeCoroutineScopeScoped: CoroutineScopeScoped
    ): CoroutineScope {
      return userScopeCoroutineScopeScoped.createChild()
    }
    ```

!!! info "CoroutineDispatcher"

    It's recommended to inject `CoroutineDispatcher` through the constructor instead of using `Dispatcher.*`. This
    allows to easily swap them within unit tests to remove concurrency and improve stability.

### `@ContributesScoped`

!!! warning

    Metro uses `@ContributesScoped` for `Scoped` integrations. `kotlin-inject-anvil` achieves a
    similar result by repurposing `@ContributesBinding` with a custom code generator.

The [`Scoped`](scope.md#scoped) interface is used to notify implementations when a `Scope` gets created and destroyed.

```kotlin
class AndroidLocationProvider : LocationProvider, Scoped {

  override fun onEnterScope(scope: Scope) {
    ...
  }
    
  override fun onExitScope() {
    ...
  }
}
```
The implementation class `AndroidLocationProvider` needs to be bound to the super type `LocationProvider` and use
multi-bindings for the `Scoped` interface. This is a lot of boilerplate to write that be auto-generated using
`@ContributesScoped` instead. When using `@ContributesScoped`, all bindings are generated and `@ContributesBinding`
doesn't need to be added. A typical implementation looks like this:

```kotlin hl_lines="2"
@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class AndroidLocationProvider : LocationProvider, Scoped
```

See the documentation for [`Scoped`](scope.md#scoped) for more details.

### Missing integrations

Metro already supports almost all App Platform specific custom extensions that previously existed
for `kotlin-inject-anvil`, including `@ContributesRenderer` and `@ContributesRobot`. The remaining
gap is support for `@ContributesRealImpl` and `@ContributesMockImpl`, which still needs a Metro
equivalent.

### Migrating to Metro from kotlin-inject-anvil

Metro and `kotlin-inject-anvil` are conceptually very similar. Since Metro is the recommended
default, migrating existing `kotlin-inject-anvil` code is usually mostly mechanical. Errors will be
reported at compile time and not runtime.

Steps could like this. [PR/173](https://github.com/vRallev/app-platform/pull/173) highlights this migration for the
`:sample` application.

* It's strongly recommended to use the latest Kotlin and Metro version. Metro is a compiler plugin and tied to the compiler to a certain degree.
* Enable Metro in the Gradle DSL:
```groovy
appPlatform {
    enableMetro true
}
```
* Change kotlin-inject specific imports to Metro:
```
me.tatarka.inject.annotations.IntoSet -> dev.zacsweers.metro.IntoSet
me.tatarka.inject.annotations.Provides -> dev.zacsweers.metro.Provides
software.amazon.lastmile.kotlin.inject.anvil.AppScope -> dev.zacsweers.metro.AppScope
software.amazon.lastmile.kotlin.inject.anvil.ContributesTo -> dev.zacsweers.metro.ContributesTo
software.amazon.lastmile.kotlin.inject.anvil.ForScope -> dev.zacsweers.metro.ForScope
software.amazon.lastmile.kotlin.inject.anvil.SingleIn -> dev.zacsweers.metro.SingleIn
```
* Update the final kotlin-inject components to Metro. The Metro docs explain the API very well. E.g. this component had to adopt a factory:
```kotlin
// Old:
@Component
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class DesktopAppComponent(@get:Provides val rootScopeProvider: RootScopeProvider) :
  DesktopAppComponentMerged

// New:
@DependencyGraph(AppScope::class)
interface DesktopAppComponent {
  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides rootScopeProvider: RootScopeProvider): DesktopAppComponent
  }
}
```
* Change usages of `addKotlinInjectComponent()` to `addMetroDependencyGraph()` and usages of `kotlinInjectComponent()` to `metroDependencyGraph()`.

## kotlin-inject-anvil

!!! note

    This section documents the supported alternative path. Prefer the Metro section above for new
    App Platform code.

    `kotlin-inject-anvil` is an opt-in feature through the Gradle DSL. The default value is `false`.
    ```groovy
    appPlatform {
      enableKotlinInject true
    }
    ```

### Component

Components are added as a service to the `Scope` class and can be obtained using the `kotlinInjectComponent()` extension
function:

```kotlin
scope.kotlinInjectComponent<AppComponent>()
```

In modularized projects, final components are defined in the `:app` modules, because the object graph has to
know about all features of the app. It is strongly recommended to create a component in each platform specific
folder to provide platform specific types.

=== "Android"

    ```kotlin title="androidMain"
    @SingleIn(AppScope::class)
    @MergeComponent(AppScope::class)
    abstract class AndroidAppComponent(
      @get:Provides val application: Application,
      @get:Provides val rootScopeProvider: RootScopeProvider,
    )
    ```

=== "iOS"

    ```kotlin title="iosMain"
    @SingleIn(AppScope::class)
    @MergeComponent(AppScope::class)
    abstract class IosAppComponent(
      @get:Provides val uiApplication: UIApplication,
      @get:Provides val rootScopeProvider: RootScopeProvider,
    )
    ```

=== "Desktop"

    ```kotlin title="desktopMain"
    @SingleIn(AppScope::class)
    @MergeComponent(AppScope::class)
    abstract class DesktopAppComponent(
      @get:Provides val rootScopeProvider: RootScopeProvider
    )
    ```

=== "WasmJs"

    ```kotlin title="wasmJsMain"
    @MergeComponent(AppScope::class)
    @SingleIn(AppScope::class)
    abstract class WasmJsAppComponent(
      @get:Provides val rootScopeProvider: RootScopeProvider
    )
    ```


### Platform implementations

`kotlin-inject-anvil` makes it simple to provide platform specific implementations for abstract APIs without needing
to use `expect / actual` declarations or any specific wiring. Since the final components live in the platform specific
source folders, all contributions for a platform are automatically picked up. Platform specific implementations can
use and inject types from the platform.

```kotlin title="commonMain"
interface LocationProvider
```

=== "Android"

    ```kotlin title="androidMain"
    @Inject
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class AndroidLocationProvider(
      val application: Application,
    ) : LocationProvider
    ```

=== "iOS"

    ```kotlin title="iosMain"
    @Inject
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class IosLocationProvider(
      val uiApplication: UIApplication,
    ) : LocationProvider
    ```

=== "Desktop"

    ```kotlin title="desktopMain"
    @Inject
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class DesktopLocationProvider(
      ...
    ) : LocationProvider
    ```

=== "WasmJs"

    ```kotlin title="wasmJsMain"
    @Inject
    @SingleIn(AppScope::class)
    @ContributesBinding(AppScope::class)
    class WasmLocationProvider(
      ...
    ) : LocationProvider
    ```

Other common code within `commonMain` can safely inject and use `LocationProvider`.

### Injecting dependencies

It's recommended to rely on constructor injection as much as possible, because it removes boilerplate and makes
testing easier. But it some cases it's required to get a dependency from a component where constructor injection
is not possible, e.g. in a static context or types created by the platform. In this case a contributed component
interface with access to the `Scope` help:

```kotlin title="androidMain"
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

  private val component = (application as RootScopeProvider).rootScope.kotlinInjectComponent<Component>()
  private val templateProvider = component.templateProviderFactory.createTemplateProvider()

  @ContributesTo(AppScope::class)
  interface Component {
    val templateProviderFactory: TemplateProvider.Factory
  }
}
```

This sample shows an Android `ViewModel` that doesn't use constructor injection. Instead, the `Scope` is retrieved
from the `Application` class and the `kotlin-inject-anvil` component is found through the `kotlinInjectComponent()` function.

??? example "Sample"

    The `ViewModel` example comes from the [sample app](https://github.com/vRallev/app-platform/blob/main/sample/app/src/androidMain/kotlin/software/ralf/app/platform/sample/MainActivityViewModel.kt).
    `ViewModels` can use constructor injection, but this requires more setup. This approach of using a component
    interface was simpler and faster.

    Another example where this approach is handy is in [`NavigationPresenterImpl`](https://github.com/vRallev/app-platform/blob/main/sample/navigation/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/navigation/NavigationPresenterImpl.kt).
    This class waits for the user scope to be available and then optionally retrieves the `Presenter` that is part
    of the user component. Constructor injection cannot be used, because `NavigationPresenterImpl` is part of the app
    scope and cannot inject dependencies from the user scope, which is a child scope of app scope. This would violate
    dependency inversion rules.

    ```kotlin hl_lines="17"
    @ContributesTo(UserScope::class)
    interface UserComponent {
      val userPresenter: UserPagePresenter
    }

    @Composable
    override fun present(input: Unit): BaseModel {
      val scope = getUserScope()
      if (scope == null) {
        // If no user is logged in, then show the logged in screen.
        val presenter = remember { loginPresenter() }
        return presenter.present(Unit)
      }

      // A user is logged in. Use the user component to get an instance of UserPagePresenter, which is only
      // part of the user scope.
      val userPresenter = remember(scope) { scope.kotlinInjectComponent<UserComponent>().userPresenter }
      return userPresenter.present(Unit)
    }
    ```

### Default bindings

App Platform provides a few defaults that can be injected, including a `CoroutineScope` and `CoroutineDispatchers`.

```kotlin
@Inject
class SampleClass(
  @ForScope(AppScope::class) appScope: CoroutineScope,

  @IoCoroutineDispatcher ioDispatcher: CoroutineDispatcher,
  @DefaultCoroutineDispatcher defaultDispatcher: CoroutineDispatcher,
  @MainCoroutineDispatcher mainDispatcher: CoroutineDispatcher,
)
```

!!! info "CoroutineScope"

    The `CoroutineScope` uses the IO dispatcher by default. The qualifier `@ForScope(AppScope::class)` is needed to
    allow other scopes to have their own `CoroutineScope`. For example, the sample app provides a `CoroutineScope`
    [for the user scope](https://github.com/vRallev/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/ralf/app/platform/sample/user/UserGraph.kt),
    which gets canceled when the user scope gets destroyed. The `CoroutineScope` for the user scope uses the qualifier
    `@ForScope(UserScope::class)

    ```kotlin
    /**
     * Provides the [CoroutineScopeScoped] for the user scope. This is a single instance for the user
     * scope.
     */
    @Provides
    @SingleIn(UserScope::class)
    @ForScope(UserScope::class)
    fun provideUserScopeCoroutineScopeScoped(
      @IoCoroutineDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScopeScoped {
      return CoroutineScopeScoped(dispatcher + SupervisorJob() + CoroutineName("UserScope"))
    }

    /**
     * Provides the [CoroutineScope] for the user scope. A new child scope is created every time an
     * instance is injected so that the parent cannot be canceled accidentally.
     */
    @Provides
    @ForScope(UserScope::class)
    fun provideUserCoroutineScope(
      @ForScope(UserScope::class) userScopeCoroutineScopeScoped: CoroutineScopeScoped
    ): CoroutineScope {
      return userScopeCoroutineScopeScoped.createChild()
    }
    ```

!!! info "CoroutineDispatcher"

    It's recommended to inject `CoroutineDispatcher` through the constructor instead of using `Dispatcher.*`. This
    allows to easily swap them within unit tests to remove concurrency and improve stability.


## Metro vs `kotlin-inject-anvil`

Metro supports all features of `kotlin-inject-anvil` and `kotlin-inject`, produces more efficient code, provides 
better error messages and compiles much faster. Metro is the recommended default for new projects,
while `kotlin-inject-anvil` remains the supported alternative when you need compatibility with
existing code. We strongly recommend using Metro for new projects and migrating existing projects
soon.
