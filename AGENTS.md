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

`mkdocs.yml` is the docs site manifest. The Pages workflow builds Wasm artifacts for `:sample:app:web` and `:recipes:app:web` and copies them into `docs/web/` before publishing.

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
- `presenter`, `presenter-compose`
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
- `:app` modules and the application-assembly `:app-framework:impl` modules are allowed to depend on `:impl` modules.

Do not introduce a dependency on an `:impl` module outside these application assembly points. The build enforces this via `checkModuleStructureDependencies`, with a scoped exception for each application framework assembly module.

The framework’s architectural flow is:

1. `Scope` and DI assemble objects for a lifecycle boundary.
2. `ComposePresenter` implementations produce models.
3. App-specific `Template` presenters wrap the root model tree.
4. `RendererFactory` resolves platform renderers for those models.
5. Thin platform entrypoints bootstrap the root scope and start rendering.

Representative entrypoints:

- Android: `sample/app/android/src/main/.../AndroidApplication.kt`, `sample/app-framework/impl/src/androidMain/.../MainActivity.kt`
- iOS: `sample/app-framework/impl/src/iosMain/.../MainViewController.kt`, `sample/app/ios/`
- Desktop: `sample/app/desktop/src/desktopMain/.../Main.kt`, `sample/app-framework/impl/src/desktopMain/.../DesktopApp.kt`
- Wasm: `sample/app/web/src/wasmJsMain/.../Main.kt`, `sample/app-framework/impl/src/wasmJsMain/.../WasmJsApp.kt`

## Toolchain

Local development should match CI as closely as possible. These versions live in `gradle/libs.versions.toml`.

Expected warning: Gradle prints a warning that configuration-on-demand is not supported for Wasm targets. This is noisy but currently normal in this repo.

For Metro compiler-plugin work, prefer source over decompiled artifacts:

- Reference implementation: `https://github.com/square/metro-extensions`
- Metro source: use a local checkout if you have one, otherwise upstream Metro on GitHub
- Avoid relying on `.gradle/caches` or decompiled JARs when the source is available

## Run The Apps

The root build contains two shared multiplatform application frameworks with separate platform applications:

- `:sample:app-framework:impl` with `:sample:app:android`, `:sample:app:desktop`, and `:sample:app:web`.
- `:recipes:app-framework:impl` with `:recipes:app:android`, `:recipes:app:desktop`, and `:recipes:app:web`.
- `blueprints/starter`: standalone starter app; run commands from inside that directory or use its own `./gradlew`.

### Android

Install the debug APK onto a connected device or emulator:

```bash
./gradlew :sample:app:android:installDebug
./gradlew :recipes:app:android:installDebug
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
open sample/app/ios/iosApp.xcodeproj
```

Recipe app:

```bash
open recipes/app/ios/recipesIosApp.xcodeproj
```

The Xcode projects include a shell build phase that calls Gradle:

- `:sample:app-framework:impl:embedAndSignAppleFrameworkForXcode`
- `:recipes:app-framework:impl:embedAndSignAppleFrameworkForXcode`

If you only want to build the Kotlin framework without opening Xcode:

```bash
./gradlew :sample:app-framework:impl:linkDebugFrameworkIosSimulatorArm64
./gradlew :recipes:app-framework:impl:linkDebugFrameworkIosSimulatorArm64
```

CI builds the sample iOS wrapper with `xcodebuild -project sample/app/ios/iosApp.xcodeproj -scheme iosApp ... -destination id=<simulator-id>`. Use `xcrun simctl list devices` to pick a simulator if you need a pure CLI invocation.

### Desktop

Run the desktop Compose app:

```bash
./gradlew :sample:app:desktop:run
./gradlew :recipes:app:desktop:run
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
./gradlew :sample:app:web:wasmJsBrowserDevelopmentRun
./gradlew :recipes:app:web:wasmJsBrowserDevelopmentRun
```

Production bundle:

```bash
./gradlew :sample:app:web:wasmJsBrowserDistribution
./gradlew :recipes:app:web:wasmJsBrowserDistribution
```

Starter blueprint:

```bash
cd blueprints/starter
./gradlew :app:wasmJsBrowserDevelopmentRun
```

After a production Wasm build, serve the generated files from:

- `sample/app/web/build/dist/wasmJs/productionExecutable/`
- `recipes/app/web/build/dist/wasmJs/productionExecutable/`

The starter README suggests `npx http-server` from the production output directory.

## Run The Tests

### Repo-wide CI-style checks

These are the main root-level quality gates used by GitHub Actions:

```bash
./gradlew testAndroidHostTest testDebugUnitTest
./gradlew iosSimulatorArm64Test -Pkotlin.incremental.native=true
./gradlew desktopTest
./gradlew linuxX64Test
./gradlew wasmJsTest
./gradlew apiCheck
./ktfmt.sh --dry-run --set-exit-if-changed
./gradlew detekt
./gradlew lint lintAndroidMain
./gradlew checkModuleStructureDependencies
```

### Sample app tests by platform

Android instrumented UI tests:

```bash
./gradlew :sample:app:android:emulatorCheck
```

Or against a manually started device:

```bash
./gradlew :sample:app:android:connectedDebugAndroidTest
```

Desktop UI tests:

```bash
./gradlew :sample:app:desktop:desktopTest
```

Android host and launcher unit tests:

```bash
./gradlew :sample:app-framework:impl:testAndroidHostTest :sample:app:android:testDebugUnitTest
```

iOS simulator tests:

```bash
./gradlew :sample:app-framework:impl:iosSimulatorArm64Test -Pkotlin.incremental.native=true
```

All shared KMP app target tests:

```bash
./gradlew :sample:app-framework:impl:allTests
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

- Android UI tests: `sample/app/android/src/androidTest/`
- Desktop UI tests: `sample/app/desktop/src/desktopTest/`
- Shared unit tests: feature `sample/*/src/commonTest/` directories and `sample/app-framework/impl/src/commonTest/`
- Shared fakes: `sample/user/testing/`
- Shared robots: `sample/login/impl-robots/`, `sample/user/impl-robots/`
- Compiler plugin test data: `metro-extensions/contribute/impl-compiler-plugin/src/test/resources/`
- Generated compiler test runners: `metro-extensions/contribute/impl-compiler-plugin/src/test/java/software/ralf/app/platform/metro/compiler/runners/`

## Current Test Reality

As of this checkout:

- `:sample:app:desktop:desktopTest` runs successfully.
- `:sample:app-framework:impl:testAndroidHostTest` and `:sample:app:android:testDebugUnitTest` succeed but currently have `NO-SOURCE`.
- `:sample:app-framework:impl:iosSimulatorArm64Test -Pkotlin.incremental.native=true` succeeds but is currently skipped because the sample app framework has no iOS test sources.
- Android UI coverage for the sample app is in `sample/app/android/src/androidTest` and is exercised through `:sample:app:android:emulatorCheck` or `:sample:app:android:connectedDebugAndroidTest`.

## Wasm Lockfile Caveat

Wasm tasks are currently strict about the committed Yarn lockfile under `kotlin-js-store/wasm/yarn.lock`.

If a Wasm task fails with:

```text
Execution failed for task ':kotlinWasmStoreYarnLock'.
Lock file was changed. Run the `kotlinWasmUpgradeYarnLock` task to actualize lock file
```

then the generated `build/wasm/yarn.lock` does not match the committed lock. In this checkout, both `:sample:app:web:wasmJsTest` and `:sample:app:web:wasmJsBrowserDistribution` hit that failure.

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
