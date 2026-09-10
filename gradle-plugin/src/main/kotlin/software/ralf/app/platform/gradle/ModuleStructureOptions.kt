package software.ralf.app.platform.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/** Options for configuring the App Platform module structure. */
public open class ModuleStructureOptions @Inject constructor(objects: ObjectFactory) {
  private val enableDependencyCheck: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)

  private val enableTestDependencyCheck: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)

  private val allowLibraryImplToImplDependencies: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Enables or disables dependency rule enforcement while retaining module structure defaults. */
  public fun enableDependencyCheck(enable: Boolean) {
    enableDependencyCheck.set(enable)
    enableDependencyCheck.finalizeValueOnRead()
  }

  /** Enables or disables dependency rule enforcement for ordinary test compilations. */
  public fun enableTestDependencyCheck(enable: Boolean) {
    enableTestDependencyCheck.set(enable)
    enableTestDependencyCheck.finalizeValueOnRead()
  }

  /**
   * Allows an `:impl` module to depend on another `:impl` module in the same library. Dependencies
   * on `:impl` modules from other libraries remain forbidden.
   */
  public fun allowLibraryImplToImplDependencies(allow: Boolean) {
    allowLibraryImplToImplDependencies.set(allow)
    allowLibraryImplToImplDependencies.finalizeValueOnRead()
  }

  internal fun isDependencyCheckEnabled(): Property<Boolean> = enableDependencyCheck

  internal fun isTestDependencyCheckEnabled(): Property<Boolean> = enableTestDependencyCheck

  internal fun isLibraryImplToImplDependenciesAllowed(): Property<Boolean> =
    allowLibraryImplToImplDependencies
}
