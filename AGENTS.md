# AGENTS.md

## Purpose

This repository contains App Platform, a Kotlin Multiplatform application framework plus example applications and a starter blueprint. App Platform was originally developed at Amazon and is now maintained independently. The core concepts are documented in [`docs/`](docs/) and implemented across reusable library modules plus a few app entrypoints.

Start here before changing code:

- `README.md`
- `docs/index.md`
- `docs/setup.md`
- `docs/module-structure.md`
- `docs/di.md`
- `docs/presenter.md`
- `docs/renderer.md`
- `docs/template.md`
- `docs/testing.md`
- `settings.gradle`
- `buildSrc/src/main/kotlin/software/ralf/app/platform/gradle/buildsrc/`

`mkdocs.yml` is the docs site manifest. The Pages workflow builds Wasm artifacts for `:sample:app` and `:recipes:app` and copies them into `docs/web/` before publishing.

## Repo Shape

Important top-level areas:

- `gradle-plugin/`: the published `software.ralf.app.platform` Gradle plugin.
- `buildSrc/`: repo-local convention plugins used by this repository’s own modules. This is where platform targets, emulator config, desktop packaging, and Wasm defaults are defined.
- `docs/`: framework documentation. Treat this as the authoritative product docs.
- `sample/`: the main sample app. This is the best place to study end-to-end usage of scopes, DI, presenters, renderers, templates, fakes, and robots.
- `recipes/`: a second example app plus reusable “recipe” patterns, including the separate `recipesIosApp` SwiftUI/Xcode wrapper.
- `blueprints/starter/`: a standalone starter app template with its own Gradle wrapper, version catalog, and README.

Core framework module families:

- `scope`, `di-common`
- `presenter`, `presenter-molecule`
- `renderer`, `renderer-android-view`, `renderer-compose-multiplatform`
- `robot`, `robot-compose-multiplatform`, `robot-internal`
- `kotlin-inject`, `kotlin-inject-extensions`
- `metro`, `metro-extensions`
- `ksp-common`

Compiler plugin work currently lives in:

- `metro-extensions/contribute/impl-compiler-plugin/`: JVM-only Kotlin compiler plugin module for Metro-backed App Platform DI extensions such as `@ContributesRobot`. `src/main/` contains FIR generation and diagnostics. `src/test/resources/box`, `diagnostics`, and `dump` contain compiler test data. `src/test/java/.../runners/` contains generated JUnit test runners and must be regenerated with `generateTests` after adding or renaming test data files.

## Architecture Rules

The most important repo rule is the module structure documented in `docs/module-structure.md`.

- `:public` modules expose reusable APIs and shared code.
- `:impl` modules contain concrete implementations.
- `:testing` modules hold shared fakes and test helpers.
- `:*-robots` modules hold shared UI robots.
- `:app` modules are the only modules allowed to depend on `:impl` modules.

Do not introduce a dependency from a non-`:app` module to an `:impl` module. The build enforces this via `checkModuleStructureDependencies`.

The framework’s architectural flow is:

1. `Scope` and DI assemble objects for a lifecycle boundary.
2. `MoleculePresenter` implementations produce models.
3. App-specific `Template` presenters wrap the root model tree.
4. `RendererFactory` resolves platform renderers for those models.
5. Thin platform entrypoints bootstrap the root scope and start rendering.

Representative entrypoints:

- Android: `sample/app/src/androidMain/.../AndroidApplication.kt`, `MainActivity.kt`
- iOS: `sample/app/src/iosMain/.../MainViewController.kt`, `sample/iosApp/`
- Desktop: `sample/app/src/desktopMain/.../Main.kt`, `DesktopApp.kt`
- Wasm: `sample/app/src/wasmJsMain/.../Main.kt`

## Toolchain

Local development should match CI as closely as possible. These versions live in `gradle/libs.versions.toml`.

Expected warning: Gradle prints a warning that configuration-on-demand is not supported for Wasm targets. This is noisy but currently normal in this repo.

For Metro compiler-plugin work, prefer source over decompiled artifacts:

- Reference implementation: `https://github.com/square/metro-extensions`
- Metro source: use a local checkout if you have one, otherwise upstream Metro on GitHub
- Avoid relying on `.gradle/caches` or decompiled JARs when the source is available

## Run The Apps

There are three app-style entrypoints to care about:

- `:sample:app`: main sample app inside the root build.
- `:recipes:app`: recipe/demo app inside the root build.
- `blueprints/starter`: standalone starter app; run commands from inside that directory or use its own `./gradlew`.

### Android

Install the debug APK onto a connected device or emulator:

```bash
./gradlew :sample:app:installDebug
./gradlew :recipes:app:installDebug
```

For the standalone starter:

```bash
cd blueprints/starter
./gradlew :app:installDebug
```

`buildSrc/.../BaseAndroidPlugin.kt` configures managed emulator tests with a local device named `emulator` using a Pixel 3 / API 30 `aosp-atd` image.

### iOS

Sample app:

```bash
open sample/iosApp/iosApp.xcodeproj
```

Recipe app:

```bash
open recipes/recipesIosApp/recipesIosApp.xcodeproj
```

The Xcode projects include a shell build phase that calls Gradle:

- `:sample:app:embedAndSignAppleFrameworkForXcode`
- `:recipes:app:embedAndSignAppleFrameworkForXcode`

If you only want to build the Kotlin framework without opening Xcode:

```bash
./gradlew :sample:app:linkDebugFrameworkIosSimulatorArm64
./gradlew :recipes:app:linkDebugFrameworkIosSimulatorArm64
```

CI builds the sample iOS wrapper with `xcodebuild -project sample/iosApp/iosApp.xcodeproj -scheme iosApp ... -destination id=<simulator-id>`. Use `xcrun simctl list devices` to pick a simulator if you need a pure CLI invocation.

### Desktop

Run the desktop Compose app:

```bash
./gradlew :sample:app:run
./gradlew :recipes:app:run
```

Starter blueprint:

```bash
cd blueprints/starter
./gradlew :app:run
```

Desktop packaging tasks such as `packageDmg`, `packageDeb`, and `packageMsi` are available on app modules.

### Wasm

Development server:

```bash
./gradlew :sample:app:wasmJsBrowserDevelopmentRun
./gradlew :recipes:app:wasmJsBrowserDevelopmentRun
```

Production bundle:

```bash
./gradlew :sample:app:wasmJsBrowserDistribution
./gradlew :recipes:app:wasmJsBrowserDistribution
```

Starter blueprint:

```bash
cd blueprints/starter
./gradlew :app:wasmJsBrowserDevelopmentRun
```

After a production Wasm build, serve the generated files from:

- `sample/app/build/dist/wasmJs/productionExecutable/`
- `recipes/app/build/dist/wasmJs/productionExecutable/`

The starter README suggests `npx http-server` from the production output directory.

## Run The Tests

### Repo-wide CI-style checks

These are the main root-level quality gates used by GitHub Actions:

```bash
./gradlew testDebugUnitTest
./gradlew iosSimulatorArm64Test -Pkotlin.incremental.native=true
./gradlew desktopTest
./gradlew linuxX64Test
./gradlew wasmJsTest
./gradlew apiCheck
./ktfmt.sh --dry-run --set-exit-if-changed
./gradlew detekt
./gradlew lint
./gradlew checkModuleStructureDependencies
```

### Sample app tests by platform

Android instrumented UI tests:

```bash
./gradlew :sample:app:emulatorCheck
```

Or against a manually started device:

```bash
./gradlew :sample:app:connectedDebugAndroidTest
```

Desktop UI tests:

```bash
./gradlew :sample:app:desktopTest
```

Android unit tests:

```bash
./gradlew :sample:app:testDebugUnitTest
```

iOS simulator tests:

```bash
./gradlew :sample:app:iosSimulatorArm64Test -Pkotlin.incremental.native=true
```

All sample app target tests:

```bash
./gradlew :sample:app:allTests
```

### Metro compiler-plugin module

Run these from the repo root:

```bash
./gradlew :metro-extensions:contribute:impl-compiler-plugin:test
./gradlew :metro-extensions:contribute:impl-compiler-plugin:test --tests 'software.ralf.app.platform.metro.compiler.runners.BoxTestGenerated$Metro.testTinyGraph'
./gradlew :metro-extensions:contribute:impl-compiler-plugin:test -PupdateTestData
./gradlew :metro-extensions:contribute:impl-compiler-plugin:generateTests
```

Use this workflow for compiler tests:

- Add new test data under `src/test/resources/box`, `diagnostics`, or `dump`
- Run `:metro-extensions:contribute:impl-compiler-plugin:generateTests` after adding or renaming test data files
- Run `:metro-extensions:contribute:impl-compiler-plugin:test`
- Use `-PupdateTestData` when intentionally updating FIR or IR golden files

Test data conventions for this module:

- `box/`: compile-and-run tests. Each file exposes `fun box(): String` and should return `"OK"`.
- `diagnostics/`: compiler error tests with inline diagnostic markers plus `.fir.diag.txt` golden files.
- `dump/`: compiler dump tests with `.fir.txt` goldens, plus `.fir.kt.txt` files for IR text dumps.

`apiCheck` and `apiDump` are disabled for this module, so do not use them as validation commands here.

### Where tests live

- Android UI tests: `sample/app/src/androidInstrumentedTest/`
- Desktop UI tests: `sample/app/src/desktopTest/`
- Shared unit tests: `sample/*/src/commonTest/`
- Shared fakes: `sample/user/testing/`
- Shared robots: `sample/login/impl-robots/`, `sample/user/impl-robots/`
- Compiler plugin test data: `metro-extensions/contribute/impl-compiler-plugin/src/test/resources/`
- Generated compiler test runners: `metro-extensions/contribute/impl-compiler-plugin/src/test/java/software/ralf/app/platform/metro/compiler/runners/`

## Current Test Reality

As of this checkout:

- `:sample:app:desktopTest` runs successfully.
- `:sample:app:testDebugUnitTest` succeeds but currently has `NO-SOURCE`.
- `:sample:app:iosSimulatorArm64Test -Pkotlin.incremental.native=true` succeeds but is currently skipped because `sample/app` has no iOS test sources.
- Android UI coverage for the sample app is in `androidInstrumentedTest` and is exercised through `emulatorCheck`/`connectedDebugAndroidTest`.

## Wasm Lockfile Caveat

Wasm tasks are currently strict about the committed Yarn lockfile under `kotlin-js-store/wasm/yarn.lock`.

If a Wasm task fails with:

```text
Execution failed for task ':kotlinWasmStoreYarnLock'.
Lock file was changed. Run the `kotlinWasmUpgradeYarnLock` task to actualize lock file
```

then the generated `build/wasm/yarn.lock` does not match the committed lock. In this checkout, both `:sample:app:wasmJsTest` and `:sample:app:wasmJsBrowserDistribution` hit that failure.

Treat `kotlinWasmUpgradeYarnLock` as an intentional dependency update step, not a routine run command. If you change Wasm/npm dependencies on purpose, update and review `kotlin-js-store/wasm/yarn.lock` in the same change.

## Change Log Workflow

When a change affects consumers of this project, add a short, single-line entry describing the implication under the appropriate `Unreleased` section in `CHANGELOG.md`. Follow the existing writing style and omit changes with no consumer-visible impact, such as test-only updates.

## Docs Workflow

To work on docs locally:

```bash
cp CHANGELOG.md docs/changelog.md
pip install mkdocs-material "mkdocs-material[imaging]"
mkdocs serve
```

Use D2 for new or updated documentation diagrams. Keep editable `.d2` sources under `docs/diagrams/`, commit matching light and dark SVG variants under `docs/images/`, and use or extend a checked-in rendering script so the exact layout and style remain reproducible.

When changing framework behavior, update both:

- the relevant `docs/*.md` page
- the sample and/or starter code that demonstrates that behavior

If a change affects how consumers start a new project, also update `blueprints/starter/README.md`.
