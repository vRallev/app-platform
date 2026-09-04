# Change Log

## [Unreleased]

### Added

### Changed

- **Breaking change:** Rename the Molecule-specific presenter API to Compose-focused names, including `MoleculePresenter` to `ComposePresenter`, its scope APIs, Gradle DSL options, and `:presenter-molecule:*` artifacts to `:presenter-compose:*`.

### Deprecated

### Removed

### Fixed

### Security

### Other Notes & Contributions


## [0.1.4] - 2026-09-03

### Added

- Add project-neutral coding agent skills for App Platform module structure, scopes, presenters, renderers, templates, and testing.
- Add experimental `withLocalRetainedValuesStore()` for retaining child `MoleculePresenter` state with caller-owned managed stores while it temporarily leaves the composition.
- Expose `String.moduleTypeFromProjectPath()` for parsing module types without a `Project` instance.

### Changed

- Mark `BaseModel` as stable so Compose treats `MoleculePresenter` models consistently through the shared interface.
- Allow constructing `MoleculePresenter` instances with SAM syntax.
- Upgrade KSP to `2.3.11`.
- Upgrade Compose Multiplatform to `1.12.0`.

### Removed

- **Breaking change:** Remove `returningCompositionLocalProvider()` from the Molecule presenter API; use Compose runtime's stable `withCompositionLocal()` or `withCompositionLocals()` instead.

### Fixed

- Preserve decorated coroutine scope lifetimes in `FakeMoleculeScopeFactory` instead of creating unmanaged test jobs.

## [0.1.3] - 2026-08-21

### Added

- Allow robots to be contributed to any scope and find them automatically in child scopes during tests.

### Changed

- **Breaking binary change:** Robot waiting helpers accept suspending callbacks and enforce timeouts across callback execution and polling delays; existing Kotlin call sites remain source-compatible, but previously compiled consumers must recompile.
- Upgrade Gradle to `9.7.1` and replace deprecated raw `Project` dependency notation with explicit `ProjectDependency` declarations for Gradle 10 compatibility.

### Fixed

- Enforce module structure dependency rules for Android and JVM test fixtures while allowing fixtures to use test-only modules.
- Preserve assertion failures when `waitUntilCatching` times out so test runners report failed assertions correctly.

## [0.1.2] - 2026-08-13

### Changed

- Upgrade Metro to `1.4.2`.

## [0.1.1] - 2026-07-30

### Added

- Add the advanced `blueprints/list-detail` Kotlin Multiplatform blueprint with a modular architecture, adaptive phone and tablet navigation, bundled character portraits, four platform shells, and dedicated CI. The web app is available [in the docs](https://vrallev.github.io/app-platform/#web-list-detail-blueprint).
- Add an `enableDependencyCheck` module structure option that skips per-target dependency checks while preserving default module dependencies and lifecycle tasks.

### Changed

- Compile the App Platform Gradle plugin against Android Gradle Plugin `9.3.1` APIs and raise the plugin's required JVM version and published bytecode target from Java 11 to Java 17.

### Fixed

- Give module-structured projects unique default archive names based on their artifact IDs while preserving explicitly configured archive names.

## [0.1.0] - 2026-07-17

### Changed

- **Breaking change:** Move App Platform packages, Maven coordinates, and the Gradle plugin ID from `software.amazon.app.platform` to `software.ralf.app.platform`. Consumers must update their plugin IDs, dependency coordinates, and imports together.

## [0.0.17] - 2026-07-15

**IMPORTANT:** This is the last release under the `software.amazon` namespace. The next release will use `software.ralf`.

### Changed

- Upgrade Kotlin to `2.4.10`, KSP to `2.3.10`, Metro to `1.3.2`, Compose Multiplatform to `1.11.1`, and AndroidX Compose to `1.11.4`.
- Clarify the project's independent ownership and remove obsolete Amazon contribution, conduct, and attribution files.

## [0.0.16] - 2026-07-02

**IMPORTANT:** Ownership of this project was transferred from `amzn/app-platform` to `vRallev/app-platform`.

### Added

- Add `robot-compose-multiplatform-public` automatically to Kotlin Multiplatform `:app` test source sets when Compose UI is enabled, so Compose robot tests can run without a manual dependency.

### Changed

- Upgrade Metro to `1.3.0`.
- Use JDK 25 for the repository build and CI toolchain while keeping published module JVM compatibility unchanged.
- Update documentation, publishing metadata, and generated site links for the repository move to `vRallev/app-platform`.

### Fixed

- Make the App Platform Gradle plugin work with AGP built-in Kotlin Android application and library modules that do not apply `org.jetbrains.kotlin.android`, including Metro dependencies, module-structure dependencies, and compiler-plugin classpaths.
- Treat `:apps:`, `:app-*`, and `:apps-*` project path groups as app modules so module-structure checks allow those app boundaries to depend on `:impl` modules.

## [0.0.15] - 2026-06-15

### Added

- Add an opt-in `allowLibraryImplToImplDependencies` module structure option for dependencies between
  `:impl` modules within the same library.

### Changed

- Upgrade Kotlin to `2.4.0`, Android Lint to `9.2.1`, KSP to `2.3.9`, and kotlin-compile-testing to `0.13.0`.


## [0.0.14] - 2026-05-20

### Added

- Add `@ExperimentalAppPlatform` in `:common:public` for APIs that require explicit consumer opt-in.
- Add experimental `MoleculePresenter.presentDetached()` to compose presenter subtrees in detached Molecule hierarchies so busy parent presenters do not recompose slower child presenters.
- Add experimental Molecule presenter-backed text field state helpers for sharing text input state between presenters and Compose renderers.
- Add experimental `ReturningSaveableStateHolder` in `:presenter-molecule:public` for preserving `rememberSaveable` state in value-returning Molecule presenter subtrees.
- Add experimental Navigation 3 presenter backstack APIs and the `enableMoleculePresenterBackstack` Gradle plugin option.


## [0.0.13] - 2026-05-14

### Changed

- Use the `Dispatchers.Main` as default main dispatcher instead of the immediate main dispatcher.
- Upgrade Kotlin to `2.3.21`, Compose Multiplatform to `1.11.0`, Metro to `1.1.1`, and other dependencies.
- **Breaking change:** The App Platform Gradle plugin doesn't apply the KSP plugin automatically anymore. If you use kotlin-inject as DI framework, then you need to add a dependency on KSP yourself (similar to how you apply the App Platform Gradle plugin).

### Fixed

- Make the App Platform Gradle plugin compatible with AGP 9's Android KMP and built-in Kotlin behavior while staying compatible with AGP 8.


## [0.0.12] - 2026-05-12

### Changed

- `@ContributesRobot` no longer requires `@Inject` for robots with constructor parameters. The generated contribution now provides a constructor-calling `@Provides` function that injects the same arguments.
- `@ContributesRenderer` no longer requires `@Inject` for renderers with constructor parameters. The generated contribution now provides a constructor-calling `@Provides` function and reports an error when multiple constructors make that provider ambiguous.
- `@ContributesScoped` no longer requires `@Inject` for classes with constructor parameters. The generated contribution now provides a constructor-calling `@Provides` function and reports an error when multiple constructors make that provider ambiguous.


## [0.0.11] - 2026-05-03

### Changed

- **Breaking change:** Added an optional `Modifier` parameter to `BaseComposeRenderer.renderCompose()` and forwarded it to `ComposeRenderer.Compose()` so callers can apply standard Compose modifiers at the renderer boundary.
- Metro to `1.0.0`
- Moved the Navigation 3 recipe to `commonMain` now that it is KMP ready.

### Removed

- Removed Apple x86_64 targets from the repository builds by dropping `iosX64` where it was still configured, aligning with Compose Multiplatform's removal of Apple x86_64 target support: https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html#dropped-support-for-apple-x86-64-targets


## [0.0.10] - 2026-04-20

### Added

- Migrate the blueprints/starter app from kotlin-inject to Metro, see #178
- Add a compiler plugin for Metro extensions, see #179. The compiler plugin is now used by default, but the KSP implementations can be enabled by setting the Gradle property `-Papp.platform.metro.ksp=true`.

## Changed

- Metro to `1.0.0-RC2`


## [0.0.9] - 2026-04-13

### Added

- Convert the sample app to [Metro](https://zacsweers.github.io/metro/), see #173. With the recent Kotlin and Metro version updates, issues we saw with Metro and targets other than Android/JVM are solved, and Metro is now the [recommended default](https://vrallev.github.io/app-platform/di/) for dependency injection.

### Changed

- Kotlin to `2.3.20`
- Gradle to `9.4.1`
- metro to `0.13.2`


## [0.0.8] - 2026-01-27

### Added

- Added a recipe for `Presenter` integration with SwiftUI, see #154.

### Changed

- Kotlin to `2.2.21`, see #161
- KSP to `2.3.4`
- kotlin-inject to `0.9.0`
- kotlin-inject-anvil to `0.1.7`
- metro to `0.10.1`
- Remove testing for KSP1 and use KSP2

### Other Notes & Contributions

- Special thanks to [@rvenable](https://github.com/rvenable) for creating the original Swift APIs that served as the foundation for #154!


## [0.0.7] - 2025-09-26

### Changed

- Changed the min SDK from 21 to 23, see #149.

### Fixed

- Fix NPE when removing Android Views from multiple child renderers with the same parent on activity destruction, see #150.


## [0.0.6] - 2025-09-05

### Added

- Added support for [Metro](https://zacsweers.github.io/metro/) as dependency injection framework. User can choose between [`kotlin-inject-anvil`](https://github.com/amzn/kotlin-inject-anvil) and [Metro](https://zacsweers.github.io/metro/). For more details see the [documentation](https://vrallev.github.io/app-platform/di/) for how to setup and use both dependency injection frameworks with App Platform.

### Changed

- Changed the provided `CoroutineScope` within `ViewRenderer` from a custom scope to `MainScope()`, see #124.
- Disallow changing the parent View for `ViewRenderers`. For a different parent view `RendererFactory.getRenderer()` will now return a new `Renderer` instead of the cached instance. The cached instance is only returned for the same parent view, see #139.

### Deprecated

- Deprecated `diComponent()` and introduce `kotlinInjectComponent()` as replacement, see #106.
- Deprecated `RendererFactory.getChildRendererForParent()`. `RendererFactory.getRenderer()` now provides the same functionality, see #139.

### Fixed

- Fix and stop suppressing NPE when removing Android Views, which lead to an inconsistent state and potential crashes laters, see #136.
- Cancel the `CoroutineScope` in `ViewRenderer` in rare cases where `onDetach` for the view isn't triggered. This caused potential leaks, see #140.


## [0.0.5] - 2025-08-15

### Added

- Added support for the new [Android-KMP library plugin](https://developer.android.com/kotlin/multiplatform/plugin) in App Platform's Gradle plugin.
- Added a [recipe](https://vrallev.github.io/app-platform/presenter/#navigation-3) for how to use the Navigation 3 library with App Platform.

### Changed

- Upgraded Kotlin to `2.2.10`.


## [0.0.4] - 2025-07-25

### Added

- Added a search field to the wiki.
- Added a [blueprint project](https://github.com/vRallev/app-platform/tree/main/blueprints/starter) for App Platform that can be copied to spin up new projects faster, see #63.
- Added support for back press events in `Presenters`. The API is similar to the one from Compose Multiplatform and Android Compose. See the [documentation in the wiki](https://vrallev.github.io/app-platform/presenter/#back-gestures) for more details.
- Added a [recipes application](https://vrallev.github.io/app-platform/#web-recipe-app) showing solutions to common problems. All solutions have been [documented in the wiki](https://vrallev.github.io/app-platform/presenter/#recipes).

### Changed

- Upgraded Kotlin to `2.2.0`.


## [0.0.3] - 2025-05-28

### Added

- Wasm JS is now officially supported and artifacts are published.

### Changed

- Snapshots are now published to the Central Portal Snapshots repository at https://central.sonatype.com/repository/maven-snapshots/.
- Upgraded Kotlin to `2.1.21`.

### Removed

- Removed the deprecated `onEvent` function used in `MoleculePresenters`. This is no longer needed since Kotlin 2.0.20, see #21.


## [0.0.2] - 2025-05-02

### Changed

- **Breaking change:** Changed the constructor from `ComposeAndroidRendererFactory` to two factory functions instead. A new API allows you to use this factory without an Android View as parent, see #39.

### Deprecated

- Deprecated the `onEvent` function used in `MoleculePresenters`. This is no longer needed since Kotlin 2.0.20, see #21.

### Fixed

- Made the `ModuleStructureDependencyCheckTask` cacheable, see #19.
- Fixed violations for Gradle's project isolation feature, see #20.

### Other Notes

- Updated the sample application with a shared transition animation to highlight how animations can be implemented for `Template` updates, see #37.


## [0.0.1] - 2025-04-17

- Initial release.

[Unreleased]: https://github.com/vRallev/app-platform/compare/0.1.4...HEAD
[0.1.4]: https://github.com/vRallev/app-platform/compare/0.1.4
[0.1.3]: https://github.com/vRallev/app-platform/compare/0.1.3
[0.1.2]: https://github.com/vRallev/app-platform/compare/0.1.2
[0.1.1]: https://github.com/vRallev/app-platform/compare/0.1.1
[0.1.0]: https://github.com/vRallev/app-platform/compare/0.1.0
[0.0.17]: https://github.com/vRallev/app-platform/compare/0.0.17
[0.0.16]: https://github.com/vRallev/app-platform/compare/0.0.16
[0.0.15]: https://github.com/vRallev/app-platform/compare/0.0.15
[0.0.14]: https://github.com/vRallev/app-platform/compare/0.0.14
[0.0.13]: https://github.com/vRallev/app-platform/compare/0.0.13
[0.0.12]: https://github.com/vRallev/app-platform/compare/0.0.12
[0.0.11]: https://github.com/vRallev/app-platform/compare/0.0.11
[0.0.10]: https://github.com/vRallev/app-platform/compare/0.0.10
[0.0.9]: https://github.com/vRallev/app-platform/compare/0.0.9
[0.0.8]: https://github.com/vRallev/app-platform/compare/0.0.8
[0.0.7]: https://github.com/vRallev/app-platform/compare/0.0.7
[0.0.6]: https://github.com/vRallev/app-platform/compare/0.0.6
[0.0.5]: https://github.com/vRallev/app-platform/compare/0.0.5
[0.0.4]: https://github.com/vRallev/app-platform/compare/0.0.4
[0.0.3]: https://github.com/vRallev/app-platform/compare/0.0.3
[0.0.2]: https://github.com/vRallev/app-platform/compare/0.0.2
[0.0.1]: https://github.com/vRallev/app-platform/compare/0.0.1
