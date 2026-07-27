# Blueprints

This folder contains reusable templates ("blueprints") to help you quickly get started with projects
using [App Platform](https://github.com/vRallev/app-platform). Choose the starter for a minimal new
project or the list-detail blueprint for a more advanced, adaptive application.

## 📁 [`starter/`](starter/)

The `starter/` blueprint provides everything you need to bootstrap a new project with App Platform.
It includes:

- Pre-configured `build.gradle.kts` files for Kotlin Multiplatform
- Android + iOS + Desktop + WASM targets with Compose UI enabled
- App Platform integrations like Molecule presenters and Metro dependency injection
- A working module structure with navigation and templates

## 📁 [`list-detail/`](list-detail/)

The `list-detail/` blueprint demonstrates a complete, adaptive App Platform application.
It includes:

- Android, iOS, Desktop, and WASM targets with shared Compose UI
- Adaptive phone and tablet layouts with single-pane navigation and two-pane presentation
- Molecule presenters, Metro dependency injection, and model-driven renderers
- Navigation 3 and shared-element transitions between the list and detail screens
- Reusable feature modules, fakes, and UI-test robots
- Pre-configured Groovy Gradle build files and standalone CI workflows
