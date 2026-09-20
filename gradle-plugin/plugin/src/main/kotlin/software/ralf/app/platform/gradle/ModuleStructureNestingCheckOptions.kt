package software.ralf.app.platform.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/** Options for checking library nesting within the project that enables the check. */
public open class ModuleStructureNestingCheckOptions @Inject constructor(objects: ObjectFactory) {
  private val enableLibraryNestingCheck: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)

  private val allowedNestedLibraryPaths: SetProperty<String> =
    objects.setProperty(String::class.java).convention(emptySet())

  /** Enables or disables library nesting validation. Enabled by default. */
  public fun enableLibraryNestingCheck(enable: Boolean) {
    enableLibraryNestingCheck.set(enable)
    enableLibraryNestingCheck.finalizeValueOnRead()
  }

  /**
   * Allows nesting in the given containing libraries within this project's subtree. Paths must be
   * absolute and exact, e.g. `:features:legacy`. Exceptions do not exempt nested libraries from
   * their own checks. Repeated calls add exceptions.
   */
  public fun allowNestedLibrariesIn(vararg libraryPaths: String) {
    allowNestedLibrariesIn(libraryPaths.asIterable())
  }

  /** Accepts a collection of containing library paths. See [allowNestedLibrariesIn]. */
  public fun allowNestedLibrariesIn(libraryPaths: Iterable<String>) {
    allowedNestedLibraryPaths.addAll(libraryPaths)
    allowedNestedLibraryPaths.finalizeValueOnRead()
  }

  internal fun isLibraryNestingCheckEnabled(): Property<Boolean> = enableLibraryNestingCheck

  internal fun allowedNestedLibraryPaths(): SetProperty<String> = allowedNestedLibraryPaths
}
