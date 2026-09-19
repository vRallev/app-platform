# Setup

## Gradle

App Platform, its various features and dependencies are all configured through a Gradle plugin. The various options
are explained in more detail in many of the following sections.

=== "build.gradle"

    ```groovy
    plugins {
      id 'software.ralf.app.platform' version 'x.y.z'
    }

    appPlatform {
      // false by default. Adds dependencies on the APIs for scopes, presenters and renderers in order to use the App Platform.
      addPublicModuleDependencies true

      // false by default. Helpful for final application modules that must consume concrete implementations and not only APIs.
      addImplModuleDependencies true

      // false by default. Recommended DI option. Configures Metro and adds App Platform specific extensions as dependency.
      enableMetro true

      // false by default. Alternative DI option. Configures KSP and adds the kotlin-inject-anvil library as dependency.
      enableKotlinInject true

      // false by default. Configures Molecule and provides access to the ComposePresenter API.
      enableComposePresenters true

      // false by default. Adds the Navigation 3 presenter backstack module and enables Compose presenters and Compose UI.
      enableComposePresenterBackstack true

      // false by default. Adds the necessary dependencies to use Compose Multiplatform with Renderers.
      enableComposeUi true

      // false by default. Verifies that this module follows conventions for our module structure and
      // adds default dependencies. For Android projects it sets the namespace to avoid conflicts.
      enableModuleStructure true
    }
    ```

=== "build.gradle.kts"

    ```kotlin
    plugins {
      id("software.ralf.app.platform") version "x.y.z"
    }

    appPlatform {
      // false by default. Adds dependencies on the APIs for scopes, presenters and renderers in order to use the App Platform.
      addPublicModuleDependencies(true)

      // false by default. Helpful for final application modules that must consume concrete implementations and not only APIs.
      addImplModuleDependencies(true)

      // false by default. Recommended DI option. Configures Metro and adds App Platform specific extensions as dependency.
      enableMetro(true)

      // false by default. Alternative DI option. Configures KSP and adds the kotlin-inject-anvil library as dependency.
      enableKotlinInject(true)

      // false by default. Configures Molecule and provides access to the ComposePresenter API.
      enableComposePresenters(true)

      // false by default. Adds the Navigation 3 presenter backstack module and enables Compose presenters and Compose UI.
      enableComposePresenterBackstack(true)

      // false by default. Adds the necessary dependencies to use Compose Multiplatform with Renderers.
      enableComposeUi(true)

      // false by default. Verifies that this module follows conventions for our module structure and
      // adds default dependencies. For Android projects it sets the namespace to avoid conflicts.
      enableModuleStructure(true)
    }
    ```

!!! note

    All settings of App Platform are optional and opt-in, e.g. you can use Compose Presenters without enabling
    the opinionated module structure. Compose UI can be enabled without using `Metro` or
    `kotlin-inject-anvil`. When you do want DI, Metro is the recommended default.

## Coding agent skills

This repository includes project-neutral skills for coding agents working on applications built with App Platform. Choose the skill that matches the change:

| Skill | Use for |
| --- | --- |
| [Module structure](https://github.com/vRallev/app-platform/blob/main/skills/app-platform-module-structure/SKILL.md) | Public, implementation, testing, and robot module boundaries; app assembly; and Gradle checks |
| [Scopes](https://github.com/vRallev/app-platform/blob/main/skills/app-platform-scope/SKILL.md) | App Platform lifetimes, coroutine scopes, and Metro graph integration |
| [Presenters](https://github.com/vRallev/app-platform/blob/main/skills/app-platform-presenters/SKILL.md) | `ComposePresenter` models, state, composition, hosting, and template selection |
| [Renderers](https://github.com/vRallev/app-platform/blob/main/skills/app-platform-renderers/SKILL.md) | Compose and Android View renderers, factories, child rendering, and template layouts |
| [Testing](https://github.com/vRallev/app-platform/blob/main/skills/app-platform-testing/SKILL.md) | Fakes, shared unit tests, optional Desktop renderer tests, and robot integration tests |

## Snapshot

To import snapshot builds use following repository:

=== "build.gradle"

    ```groovy
    maven {
      url = 'https://central.sonatype.com/repository/maven-snapshots/'
    }
    ```

=== "build.gradle.kts"

    ```kotlin
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
    ```
