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

      // false by default. Configures Molecule and provides access to the MoleculePresenter API.
      enableMoleculePresenters true

      // false by default. Adds the Navigation 3 presenter backstack module and enables Molecule presenters and Compose UI.
      enableMoleculePresenterBackstack true

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

      // false by default. Configures Molecule and provides access to the MoleculePresenter API.
      enableMoleculePresenters(true)

      // false by default. Adds the Navigation 3 presenter backstack module and enables Molecule presenters and Compose UI.
      enableMoleculePresenterBackstack(true)

      // false by default. Adds the necessary dependencies to use Compose Multiplatform with Renderers.
      enableComposeUi(true)

      // false by default. Verifies that this module follows conventions for our module structure and
      // adds default dependencies. For Android projects it sets the namespace to avoid conflicts.
      enableModuleStructure(true)
    }
    ```

!!! note

    All settings of App Platform are optional and opt-in, e.g. you can use Molecule Presenters without enabling
    the opinionated module structure. Compose UI can be enabled without using `Metro` or
    `kotlin-inject-anvil`. When you do want DI, Metro is the recommended default.

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
