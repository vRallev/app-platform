package software.ralf.app.platform.metro

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Scope
import software.ralf.app.platform.ksp.ContextAware

internal interface MetroContextAware : ContextAware {
  val injectFqName
    get() = Inject::class.requireQualifiedName()

  private val scopeFqName
    get() = Scope::class.requireQualifiedName()

  fun KSAnnotation.isMetroScopeAnnotation(): Boolean {
    return annotationType.resolve().isMetroScopeAnnotation()
  }

  private fun KSType.isMetroScopeAnnotation(): Boolean {
    return declaration.annotations.any {
      // Don't use requireQualifiedName(), because @ContributingAnnotation might not be
      // on the compile classpath.
      it.annotationType.resolve().declaration.qualifiedName?.asString() == scopeFqName
    }
  }
}
