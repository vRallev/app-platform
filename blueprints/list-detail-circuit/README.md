# List Detail Circuit Blueprint

This local experiment implements the same Middle-earth application as
[`list-detail`](../list-detail/) using [Circuit](https://slackhq.github.io/circuit/) and Metro.
It keeps App Platform's Gradle module structure and dependency checks, with no App Platform
runtime or test libraries. It has its own Gradle build and does not add CI integration.

Android, iOS, Desktop, and Wasm share the character data, portraits, theme, and Compose UI. The
adaptive behavior matches the original: phone navigation, a 384 dp list pane in tablet landscape,
selection across layout changes, and shared portrait/name/age transitions on phones.

- [Compare the unidirectional dataflow implementations](docs/udf-comparison.md).
- [Test coverage and local validation](docs/testing.md).

The package and application ID are `software.ralf.circuit.listdetail`, so both blueprints can be
installed together. Dependency versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml),
including Circuit `0.38.0`. Build scripts use the Groovy DSL.

## Complexity comparison

Based on reading these two implementations, App Platform has more setup concepts; Circuit has
more custom glue in this particular feature. The underlying unidirectional dataflow complexity
is almost identical.

| Area | Judgment |
| --- | --- |
| State derivation | Tie. Both use observable state, composable presenters, effects, and emitted models. |
| Events | App Platform's callbacks are concise. Circuit's sealed events make the event vocabulary explicit, with more declarations and dispatch code. |
| App setup | Circuit is simpler: a graph and `CircuitContent`, versus scopes, template production, and renderer lookup. |
| Adaptive composition | App Platform is more straightforward here. Parents pass inputs, compose children, and override callbacks. |
| Headless tests | Circuit's fixtures and typed assertions are simpler, but supporting the complete navigation flow requires more production glue. |

The tablet implementation illustrates the composition difference.
[App Platform's parent](../list-detail/list-detail/impl/src/commonMain/kotlin/software/ralf/app/platform/listdetail/TabletListDetailPresenter.kt)
changes a selection callback and calls the detail presenter.
[Circuit's parent](list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/TabletListDetailPresenter.kt)
constructs screens and presenters and supplies a navigator that translates navigation requests
into selection changes. The App Platform version is easier to follow for this behavior.

Circuit is easier to follow at the application boundary. Its
[`AppContent`](app-framework/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/AppContent.kt)
is direct. App Platform requires understanding `Scope`, `MoleculeScope`, `TemplateProvider`,
templates, and renderer registration. That is a substantial initial learning cost, even when
individual classes are small.

The biggest qualification is this experiment's custom Circuit navigation implementation. To
preserve the headless coverage, it manually composes navigation entries and handles transition
identity, departing state, and saved UI state in
[`PhoneListDetailUi`](list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/PhoneListDetailUi.kt).
App Platform already packages equivalent responsibilities behind its presenter-backstack APIs.
This comparison therefore exposes a cost of the chosen Circuit integration, not an unavoidable
cost of every Circuit app.

There is also a lifecycle difference behind the smaller setup: App Platform retains the running
presenter composition across Android recreation; this Circuit version restores saved state and
restarts presenter work. Matching retained work would add complexity.

For composing presentation logic and testing the whole flow headlessly, this comparison favors
App Platform. For understanding and wiring up an ordinary Compose screen, it favors Circuit.
These judgments apply to the implementations in these blueprints, not every application built
with either framework.

## Run

Run these commands from this directory. Use Java 21 for Gradle; the wrapper and Foojay resolver
use the same toolchain setup as the original blueprint.

```bash
# Desktop
./gradlew :app:desktop:run

# Desktop with Compose Hot Reload
./gradlew :app:desktop:hotRunDesktop --auto

# Android, with a connected device or emulator
./gradlew :app:android:installDebug

# Web
./gradlew :app:web:wasmJsBrowserDevelopmentRun
```

Press `Command+S` on macOS or `Ctrl+S` elsewhere to switch the Desktop window between the phone
and tablet presets.

For iOS, open `app/ios/iosApp.xcodeproj` and run the `iosApp` scheme on a simulator. Its build phase
embeds the `ListDetailCircuitApp` Kotlin framework. Build just that framework with:

```bash
./gradlew :app-framework:impl:linkDebugFrameworkIosSimulatorArm64
```

## Test

```bash
# Shared presenter/sizing tests and both Desktop integration modes
./gradlew desktopTest

# Headless integration only: production Metro graph, repository, presenters, and navigation
./gradlew :app-framework:impl-ui-test-robots:desktopTest

# Rendered Desktop integration only
./gradlew :app:desktop:desktopTest

# Android UI, including Activity recreation and system Back
./gradlew :app:android:connectedDebugAndroidTest

# Or use the configured Pixel 3 / API 30 managed emulator
./gradlew :app:android:emulatorCheck

# Module boundaries
./gradlew checkModuleStructureDependencies
```

## Change the app

Start with `:list-detail:impl` for Circuit presenters and UIs, `:list-detail:public` for the root
screen and repository contract, and `:list-detail:testing` for the fake repository.
`:app-framework:impl` assembles Metro contributions into a `Circuit` and hosts the root screen.
`:templates` holds window sizing and the outer theme/insets scaffold; `:theme:public` owns theme
access. Platform app modules contain only their launchers and platform integration tests.

Keep new feature implementations behind their `:public` module. Add UI helpers to an `:impl-robots`
module and app-level test fixtures to `:app-framework:impl-ui-test-robots`. These are ordinary Compose
test helpers; their module names do not imply an App Platform runtime dependency.
