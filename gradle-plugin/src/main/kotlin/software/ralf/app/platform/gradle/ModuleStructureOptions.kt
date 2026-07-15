package software.ralf.app.platform.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/** Options for configuring the App Platform module structure. */
public open class ModuleStructureOptions @Inject constructor(objects: ObjectFactory) {
  private val allowLibraryImplToImplDependencies: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /**
   * Allows an `:impl` module to depend on another `:impl` module in the same library. Dependencies
   * on `:impl` modules from other libraries remain forbidden.
   */
  public fun allowLibraryImplToImplDependencies(allow: Boolean) {
    allowLibraryImplToImplDependencies.set(allow)
    allowLibraryImplToImplDependencies.finalizeValueOnRead()
  }

  internal fun isLibraryImplToImplDependenciesAllowed(): Property<Boolean> =
    allowLibraryImplToImplDependencies
}
