# Testing

## Local verification: 28 August 2026

All 21 distinct scenarios passed: 11 presenter, 3 sizing, 2 headless Desktop, 3 rendered Desktop,
and 2 Android UI tests. The 14 shared presenter/sizing tests also passed on Android host, iOS
simulator Arm64, and Wasm in Chrome. Android UI ran on a Pixel 4 emulator with API 33.

Android APK, Desktop distribution, iOS simulator framework and Swift app, and the production Wasm
bundle built successfully. Formatting, Detekt, and module checks passed. The web app was also
checked in phone and tablet layouts, including selection across resizing. The iOS app was built,
but its UI and native gestures were not exercised on a simulator.

## Coverage

The original blueprint's 16 test scenarios are preserved. The Circuit version also checks
Android recreation/system Back, returning from tablet to a fresh phone stack, invalid/removed
selection fallback, a fresh feature composition, and list scroll state across navigation.

| Layer | Test source | What it exercises |
| --- | --- | --- |
| Presenter unit tests | `list-detail/impl/src/commonTest/.../ListDetailPresenterTest.kt` | Fake repository, real Circuit presenters and phone navigator, layout selection, back, resize, live detail data, empty state, and composition lifetime |
| Screen sizing | `templates/public/src/commonTest/.../ScreenSizeTest.kt` | Landscape threshold, square windows, and portrait windows |
| Desktop headless integration | `app-framework/impl-ui-test-robots/src/desktopTest/.../ListDetailDesktopHeadlessTest.kt` | Production Metro graph, real repository and root presenter, phone stack and tablet selection; no Compose UI scene |
| Desktop UI integration | `app/desktop/src/desktopTest/.../ListDetailDesktopUiTest.kt` | Production `AppContent` in phone/tablet Compose scenes, UI interactions and scroll restoration |
| Android UI integration | `app/android/src/androidTest/.../ListDetailAndroidUiTest.kt` | Production Activity and graph on a device, toolbar Back, Activity recreation, and system Back |

The headless fixture resolves `ListDetailScreen` from `graph.circuit`, calls Circuit's `Presenter.test`,
and sends the same state events that the UIs send. It does not replace production navigation with
a fake or duplicate the app logic in a test harness. Presenter unit tests supply a fake repository
and `CircuitSaver.NoOp` because they do not restore Android state; the Android integration test
exercises the production serializers and saveable stack.

The feature robots are ordinary classes that accept a Compose semantics provider. Tests construct
them directly. The original tags and selected-row semantics remain intact; there is no App Platform
robot lookup, global test scope, or test-only application graph. Platform suites remain separate,
and there are no iOS UI integration tests, matching the original test boundary.

## Commands

Run from the blueprint directory:

```bash
# All Desktop tests: shared units, rendered integration, headless integration
./gradlew desktopTest

# Individual integration modes
./gradlew :app-framework:impl-ui-test-robots:desktopTest
./gradlew :app:desktop:desktopTest
./gradlew :app:android:connectedDebugAndroidTest

# Managed Android alternative
./gradlew :app:android:emulatorCheck

# Shared unit tests on other targets
./gradlew testAndroidHostTest
./gradlew iosSimulatorArm64Test
./gradlew wasmJsTest

# Build conventions and static checks
./gradlew -p buildSrc release
./gradlew checkModuleStructureDependencies detekt

# Platform artifacts
./gradlew :app:android:assembleDebug
./gradlew :app:desktop:createDistributable
./gradlew :app-framework:impl:linkDebugFrameworkIosSimulatorArm64
./gradlew :app:web:wasmJsBrowserDistribution
```

Desktop tests use the production `480 × 840 dp` and `1100 × 760 dp` presets. Android uses the
connected device's real window size; use a phone in portrait for these scenarios. The managed
device is a Pixel 3 with an API 30 `aosp-atd` image. A normal `AndroidJUnitRunner` launches the
production application, and tests do not change device-wide animation settings.

Browser tests require a Chromium executable. If Chrome is not on the default path, set `CHROME_BIN`
to the executable before running `wasmJsTest`. The production web bundle is written to
`app/web/build/dist/wasmJs/productionExecutable/`.

Wasm uses the committed `kotlin-js-store/wasm/yarn.lock`. When intentionally changing Kotlin or npm
dependencies, run `kotlinWasmUpgradeYarnLock` and review the resulting lockfile in this blueprint.
The original blueprint's lockfile is independent.

The copied Android launcher assets retain the original appearance. Android lint reports
`IconLauncherShape`, `MonochromeLauncherIcon`, and `IconDuplicates` on those assets; this experiment
does not suppress those findings or redesign the icons.

Detekt retains the behavioral and style rules from the original blueprint. Required KDoc is scoped
to `:public` modules; the implementation walkthrough lives in [the UDF comparison](udf-comparison.md).
