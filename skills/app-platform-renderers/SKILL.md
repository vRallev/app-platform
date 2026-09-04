---
name: app-platform-renderers
description: Build and test App Platform renderers and template layouts. Use for Compose or Android View rendering, model bindings, child renderers, template layout or rendering, UI state, and renderer factory setup.
---

# App Platform Renderers and Template Layouts

A `Renderer` turns a presenter's `BaseModel` into UI and sends user actions back through model callbacks. Add a renderer when a model needs direct visual output. A presenter that only selects child models may use their renderers directly.

Follow the consuming app's UI toolkit, design system, source sets, and DI setup. App Platform supplies Compose Multiplatform and Android View renderers; preserve any app-specific native integration. Check the project's version before using experimental APIs. Examples omit imports; resolve unfamiliar APIs from existing code or matching public docs.

## Keep responsibilities clear

- Renderers own layout, localized text, animations, focus, and other UI details. Presenters own feature decisions, validation, and navigation state.
- Read models without mutating them and forward events through their callbacks. Keep repositories and business logic in presenters.
- Keep shared presenter APIs free of UI and platform types. When using App Platform's module layout, put renderers in `:impl`, with shared Compose UI in `commonMain` and platform code in its target source set.
- Use the app's theme and components. Store UI labels in its localization resources; for Compose Multiplatform, these usually live under `src/commonMain/composeResources/values/strings.xml`. User-entered text and data from services can come from models. Test tags can be code constants.

## Compose renderers

Extend `ComposeRenderer<ModelT>` and override `Compose(model, modifier)`. Apply the supplied modifier to the root composable so parent layout, accessibility, input, and test behavior reach the UI.

For a `ProfilePresenter.Model` with `displayName: String` and `onSelected: () -> Unit`:

```kotlin
@ContributesRenderer
class ProfileRenderer : ComposeRenderer<ProfilePresenter.Model>() {
  @Composable
  override fun Compose(model: ProfilePresenter.Model, modifier: Modifier) {
    BasicText(
      text = model.displayName,
      modifier = modifier.clickable(role = Role.Button, onClick = model.onSelected),
    )
  }
}
```

Call `renderCompose(model, modifier)` from a composable context. The base `render(model)` entry point is unsupported for a direct `ComposeRenderer`.

Keep UI state inside `Compose()` with `remember` or the appropriate state helper. Factories cache renderer instances, so fields should hold dependencies rather than per-screen Compose state. Read the current model on each call. For long-lived effects, use keys or `rememberUpdatedState` to keep callbacks current. Clean up UI listeners with `DisposableEffect`.

## Registration and factories

Annotate renderers with `@ContributesRenderer` and keep their classes public for generated code across modules. The renderer's model type determines its binding. With Metro, a single constructor gets a generated provider; `@Inject` is only needed to select among multiple constructors for this lookup.

Create and keep a factory once per UI host, such as an activity, view controller, or window:

| UI in the host | Factory |
| --- | --- |
| Compose Multiplatform | `ComposeRendererFactory` |
| Android Views | `AndroidRendererFactory` |
| Compose and Android Views together | `ComposeAndroidRendererFactory` |

The Android factories hold an activity and parent view; keep them within that host's lifetime. `getRenderer()` and `getComposeRenderer()` reuse cached instances. Use `createRenderer()`, `createComposeRenderer()`, or a distinct `rendererId` when separate instances are needed.

For missing-renderer errors, check the actual model type, contribution generation, the app's implementation dependencies, and the chosen factory. Confirm that the real host graph can resolve the renderer and its dependencies.

## Child renderers

Inject `RendererFactory` to render child models without depending on concrete child renderers. For a `PagePresenter.Model` with `content: BaseModel`:

```kotlin
@ContributesRenderer
class PageRenderer(
  private val rendererFactory: RendererFactory,
) : ComposeRenderer<PagePresenter.Model>() {
  @Composable
  override fun Compose(model: PagePresenter.Model, modifier: Modifier) {
    Box(modifier = modifier) {
      rendererFactory.getComposeRenderer(model.content).renderCompose(model.content)
    }
  }
}
```

Always pass the current child model. In repeated Compose content, key children by stable item identity so remembered UI state follows the right item.

## Template rendering

Templates are app-specific root models selected by the presenter tree. Use the `app-platform-presenters` skill for the template contract, root wrapper, `toTemplate`, and `ModelDelegate`. This section assumes variants such as `AppTemplate.FullScreen(content)` and `AppTemplate.ListDetail(list, detail)`.

The template renderer lays out the semantic slots and resolves every current child model through `RendererFactory`. Follow the app's existing structure for root chrome, insets, and transitions:

```kotlin
@ContributesRenderer
class AppTemplateRenderer(
  private val rendererFactory: RendererFactory,
) : ComposeRenderer<AppTemplate>() {
  @Composable
  override fun Compose(model: AppTemplate, modifier: Modifier) {
    Box(modifier = modifier) {
      when (model) {
        is AppTemplate.FullScreen -> Render(model.content)
        is AppTemplate.ListDetail -> Row {
          Render(model.list, Modifier.weight(1f))
          Render(model.detail, Modifier.weight(2f))
        }
      }
    }
  }

  @Composable
  private fun Render(model: BaseModel, modifier: Modifier = Modifier) {
    rendererFactory.getComposeRenderer(model).renderCompose(model, modifier)
  }
}
```

`@ContributesRenderer` includes sealed subtypes by default, so one renderer can handle the whole template hierarchy. Add a renderer branch when adding a variant. Use distinct renderer IDs only when live slots need separate cached renderer instances.

Templates use the normal rendering pipeline. Keep one renderer factory for the host lifetime and render each current template through it:

```kotlin
val template by templates.collectAsState()
rendererFactory.getComposeRenderer(template).renderCompose(template)
```

With App Platform's module structure, put template renderers in `:impl` and platform-specific renderers in their target source sets. Keep renderer factory construction and implementation selection at app assembly.

For Android Views, use `AndroidTemplateRenderer` and its `Container` for slots backed by real `ViewGroup`s. Reset inactive containers when the template changes so stale views are removed.

## Backstacks

For the experimental Navigation 3 integration, extend `PresenterBackstackRenderer<ModelT>` and implement `ComposeBackstackEntry()` using the same factory pattern. The base renderer forwards `model.onBack` and keeps popped models available for exit transitions. If overriding `PresenterNavDisplay()`, forward its `backstack`, `onBack`, `entryProvider`, and `modifier` to `NavDisplay`. Keep stack changes in presenter code.

## Text input

Keep cursor, selection, and IME composition in the UI while the presenter owns the text used by feature logic. Preserve the app's text synchronization contract.

When integrating an existing `PresenterTextFieldState`, use App Platform's experimental `rememberPresenterBackedTextFieldState()` from `software.ralf.app.platform.renderer.text`. It returns Compose Foundation's `TextFieldState`:

```kotlin
@OptIn(ExperimentalAppPlatform::class)
@Composable
fun SearchField(presenterState: PresenterTextFieldState, modifier: Modifier) {
  val fieldState = rememberPresenterBackedTextFieldState(presenterState)
  BasicTextField(state = fieldState, modifier = modifier)
}
```

Read `fieldState.text` for immediate UI feedback such as clear-button visibility. The helper copies edits in both directions; presenter text replacements place the cursor at the end.

## Android Views

Use `ViewRenderer<ModelT>` or an existing subtype such as `ViewBindingRenderer`. Create views in `inflate()`, bind the latest model and callbacks in `renderModel()`, and clean up registrations in `onDetach()`. View references belong to that renderer's view lifetime.

Pass the actual parent `ViewGroup` when asking the factory for child renderers. The Android factory uses the parent in its cache key so different containers receive separate renderer instances. Use `ComposeAndroidRendererFactory` for View/Compose interop.

## Build setup

Configure each module that declares or hosts Compose UI through the app's conventions or public plugin:

```kotlin
plugins {
  id("software.ralf.app.platform")
}

appPlatform {
  enableComposeUi(true)
}
```

This supplies Compose compiler/runtime, Foundation, and the Compose renderer API. Check existing conventions before adding dependencies. Add the app's chosen component library and UI test dependencies as needed; enabling Compose UI does not choose a design system.

For Android View renderers, `addPublicModuleDependencies(true)` supplies the renderer APIs alongside the app's Android plugin setup.

For Metro-generated contributions, use `enableMetro(true)`. Add implementation dependencies at app assembly points, using `addImplModuleDependencies(true)` when relying on App Platform defaults. Enable `enableComposePresenterBackstack(true)` for the Navigation 3 backstack module; it also enables Compose presenters and Compose UI.

## Tests

Dedicated renderer tests are optional. Follow the app's existing test strategy, including screenshot tests for visual output. When extra behavior coverage is useful, use sample models and fake dependencies. Keep feature decisions in presenter tests.

Prefer Desktop for shared Compose interaction tests when the project already has that target. Put them in `desktopTest` and use the app's runner, theme, and resource setup. For example:

```kotlin
@OptIn(ExperimentalTestApi::class)
class ProfileRendererTest {
  @Test
  fun clickSelectsProfile() = runComposeUiTest {
    var selected = false
    val renderer = ProfileRenderer()
    val model = ProfilePresenter.Model(displayName = "Ada", onSelected = { selected = true })

    setContent {
      renderer.renderCompose(model, Modifier.testTag("profile"))
    }

    onNodeWithTag("profile").assertTextEquals("Ada").performClick()
    runOnIdle { assertTrue(selected) }
  }
}
```

Resolve `runComposeUiTest` from the project's UI test API; current App Platform examples use `androidx.compose.ui.test.v2`. Android interaction tests need their own setup, typically instrumentation on a device or emulator. Move tests into a shared source set only when all receiving targets already have compatible UI test support.

Optional renderer or screenshot tests can cover each template variant, slot layout, app chrome, and transitions. When registration changes, a host integration test with the real factory can verify template and child renderer discovery. Use Desktop tests for shared Compose and window sizes, and Android instrumentation for activities, back handling, Views, or Compose/View interop.

When writing interaction tests, useful checks include model and callback updates, text selection and presenter replacements, back events, and exit transitions. Use UI test synchronization instead of sleeps, and run the app's existing checks.

Public docs: [renderers](https://vrallev.github.io/app-platform/renderer/), [templates](https://vrallev.github.io/app-platform/template/), [text fields](https://vrallev.github.io/app-platform/presenter/#presenter-backed-text-fields), [backstack](https://vrallev.github.io/app-platform/presenter/#presenter-backstack), [setup](https://vrallev.github.io/app-platform/setup/), and [testing](https://vrallev.github.io/app-platform/testing/).
