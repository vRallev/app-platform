# Template App for App Platform

This is a Kotlin Multiplatform template application built using the [App Platform](https://github.com/vRallev/app-platform). It provides a modern, opinionated starting point for building scalable, testable, and multiplatform Compose applications.

## Overview

This template demonstrates:

- Kotlin Multiplatform targeting Android, iOS, WebAssembly (WASM), and Desktop (JVM)
- [App Platform](https://github.com/vRallev/app-platform) conventions for Metro DI, state, rendering, and navigation
- Molecule-powered presenters
- Scoped dependency injection using Metro graphs, `@ContributesBinding`, `@SingleIn`, `@ContributesScoped`, and `@ContributesRenderer`
- Reactive state with `StateFlow`
- Compose UI for Android, Desktop, and WASM
- Modular code structure for feature separation

## Features

- `ExampleRepository`: A simple `StateFlow`-based repository that emits data
- `ExampleValueGenerator`: A scoped class that updates the repository with random values every 3 seconds
- `NavigationHeaderPresenter` and `NavigationDetailPresenter`: Molecule presenters driving the top bar and content UI
- `NavigationHeaderRenderer` and `NavigationDetailRenderer`: A ComposeRenderer showing example state

## Modules

- `:app` – Main app entrypoint using Compose + App Platform + Metro
- `:templates` – Main module for templates and the entry point into the application
- `:navigation` – Example feature module

## Running the App

### Android

```bash
./gradlew :app:installDebug
```

### WASM (WebAssembly)

```bash
./gradlew :app:wasmJsBrowserDevelopmentRun
```

### iOS

#### Option 1: Run from IntelliJ IDEA or Android Studio

1. Install the [Kotlin Multiplatform IDE plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform).

2. Select the iosApp run configuration and run the app.

#### Option 2: Run via Xcode

1. Open the Xcode project:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. Select a simulator and run the app (`Cmd + R`)

> The required Kotlin Multiplatform framework will be built automatically as part of the Xcode build process (`./gradlew :app:embedAndSignAppleFrameworkForXcode`).

### Desktop (JVM)

```bash
./gradlew :app:run
```

> This runs the desktop Compose app using the JVM target.

## Configuration

You can modify app behavior by editing:

- `gradle.properties` – JVM and native memory settings
- `libs.versions.toml` – Centralized dependency version catalog
- `app/build.gradle.kts` – Platform-specific targets and UI modules

## Contributing

Feel free to fork and adapt this template for your own projects. If you find bugs or improvements related to App Platform usage, consider opening issues or PRs against [vRallev/app-platform](https://github.com/vRallev/app-platform).

## License

This project inherits the license of the [App Platform](https://github.com/vRallev/app-platform).
