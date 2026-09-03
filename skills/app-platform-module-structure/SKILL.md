---
name: app-platform-module-structure
description: Design and verify App Platform module boundaries. Use when placing shared APIs, implementations, internal code, fakes, robots, or app assembly; adding Gradle modules or dependencies; enabling module structure checks; or fixing forbidden dependency errors.
---

# App Platform Module Structure

App Platform applies dependency inversion at the Gradle module boundary. Consumers compile against small public contracts, concrete implementations stay hidden, and the application chooses implementations at its assembly boundary. This feature is optional and separate from scopes, presenters, renderers, and DI.

Follow the consuming project's App Platform version, target setup, and Gradle conventions. Before editing, read `settings.gradle` or `settings.gradle.kts`, repository instructions, and nearby module build files. Examples omit imports.

## Choose a boundary

First identify the owning library, its consumers, the stable contract they need, and the concrete details they should not see. Also check whether the library has platform code, alternate implementations, reusable fakes, or reusable UI robots.

Choose the smallest boundary that preserves dependency inversion. Do not create a module only because several files share a category. A library can have only `:public` when its reusable code needs no hidden implementation, such as utilities or shared UI components.

## Module types

Keep sibling modules under one library path, such as `:account:public` and `:account:impl`.

| Module | Put here | Used by |
| --- | --- | --- |
| `:public` | Shared contracts, models, and code intentionally safe to expose | Other libraries, implementations, tests, and apps |
| `:impl` or `:impl-name` | Concrete implementations, presenter and renderer implementations, DI bindings, and platform details | Final app assembly; zero or more per library |
| `:internal` | Private implementation-side code shared by sibling non-public modules | Non-public modules in the same library; rarely app assembly |
| `:testing` | Reusable fakes and test helpers for the public API | Tests and other test-only modules |
| `:*-robots` | Reusable UI or integration-test robots; usually `:impl-robots` | UI or integration tests and other robot modules |
| `:app` or another recognized app path | Final composition, selected implementations, and platform host code | The final binary |

Keep implementation-private models in `:impl`. Keep platform launcher, manifest, packaging, window, activity, and native host code in the platform app shell or the target source set that needs it.

## Dependency rules

- Among structured modules, `:public` depends only on other `:public` modules. Keep implementation and platform types out of public signatures.
- Feature implementations depend on other libraries' public contracts. Final app assembly selects and imports their `:impl` modules.
- `:impl` to `:impl` dependencies are forbidden by default. A library can share private code through `:internal`, or allow same-library implementation dependencies so one implementation builds on another.
- `:internal` stays on the implementation side of its library. A sibling `:public` module cannot import it. App assembly may import it, but ordinary cross-library consumers may not.
- Ordinary production modules add `:testing` only to test configurations and robots only to UI or integration-test configurations. Test-only `:testing` and robot modules may use `:testing` from their main source sets. Robot modules may also depend on other robot modules. None belongs on the final runtime classpath.
- App modules may import `:impl` and `:internal` modules. Their production classpaths must still exclude `:testing` and robot modules.

Gradle's `api` versus `implementation` choice is separate. Use `api` only when a dependency's types are part of the exposed API.

If many modules need a concrete type, stop and define or revise its public contract. Do not move test helpers into production code to work around visibility.

## Enable the structure

Enable the option in each participating module, directly or through the consuming project's convention plugin:

```kotlin
plugins {
  id("software.ralf.app.platform")
}

appPlatform {
  enableModuleStructure(true)
}
```

Enabling it in one module does not configure its consumers. For each enabled module, the plugin:

- Checks that the module name is a supported type.
- Adds its sibling `:public` dependency to `:impl`, `:internal`, `:testing`, and robot modules when that module exists.
- Adds the matching implementation to an `:impl-robots` module as an implementation dependency.
- Supplies default archive names and Android namespaces while preserving explicit values.
- Registers `checkModuleStructureDependencies` and connects it to Gradle's `check` lifecycle when available.

Automatic Android and Android-KMP namespaces require a `GROUP` Gradle property when no explicit namespace is set. Public modules omit `.public` from the generated namespace, and hyphens become dots.

Do not repeat automatic sibling dependencies unless the consuming build has a documented reason.

## Assemble the application

The app boundary imports concrete feature modules and builds the root graph, scope, template, and platform entry points. When it relies on App Platform's default implementations, enable them there:

```kotlin
appPlatform {
  addImplModuleDependencies(true)
}
```

This adds App Platform implementation artifacts for enabled framework features. It does not discover or import the application's feature `:impl` modules.

A shared KMP application assembly may use a path such as `:app-framework:impl`. Its leaf name still makes it an implementation module, even though it intentionally imports feature implementations. Keep the module-structure defaults but disable dependency enforcement only at that composition root:

```kotlin
appPlatform {
  enableModuleStructure {
    enableDependencyCheck(false)
  }
  addImplModuleDependencies(true)
}
```

Fix ordinary feature dependency violations instead of disabling their checks.

Some libraries use `:internal` for code shared by implementations. Others let one `:impl` module build on another and avoid a separate internal module. Enable that choice on the consuming implementation:

```kotlin
appPlatform {
  enableModuleStructure {
    allowLibraryImplToImplDependencies(true)
  }
}
```

This allows project implementation dependencies only within the same library. Cross-library and external implementation dependencies remain forbidden.

## Add or split a library

1. Map current consumers and choose the smallest stable contract.
2. Add the required modules to Gradle settings, using one library parent:

   ```kotlin
   include(":account:public")
   include(":account:impl")
   include(":account:testing")
   include(":account:impl-robots")
   ```

3. Put contracts and shared models in `:public`. Keep public APIs free of implementation and platform types.
4. Put concrete code in one or more `:impl` modules. Use target source sets inside an implementation for platform-specific code; use named implementation modules when apps or vendors select different implementations.
5. Choose how sibling implementations share code: add `:internal`, or enable same-library implementation dependencies and reuse an implementation module.
6. Put reusable fakes in `:testing` and reusable UI robots in a robot module. Keep one-off test helpers beside their tests.
7. Select implementations and assemble DI at the app boundary.
8. Copy target and App Platform feature configuration from the closest matching modules instead of enabling every feature everywhere.

A typical graph is:

```text
:checkout:impl  -> :account:public
:app            -> :checkout:impl, :account:impl
:checkout tests -> :account:testing
```

## Test and verify

Run the consuming project's focused compile and test tasks for each changed module, then run:

```shell
./gradlew checkModuleStructureDependencies
```

The aggregate task checks supported production compile classpaths across Android, JVM, and KMP targets. It also checks Android and JVM test fixtures. Public test fixtures may use `:public` and `:testing`, but not `:impl`.

When changing `:public`, run the project's API checks. When moving DI contributions or implementation selection, compile the real app graph and platform entry point. Recheck Gradle settings after adding or renaming modules.

The dependency task checks declared dependencies whose names follow the module structure. It does not prove that public APIs are well designed, that transitive dependencies are harmless, or that source-set boundaries are correct. Review exported signatures and the effective dependency graph as well.

Public docs: [module structure](https://vrallev.github.io/app-platform/module-structure/), [setup](https://vrallev.github.io/app-platform/setup/), and [testing](https://vrallev.github.io/app-platform/testing/).
