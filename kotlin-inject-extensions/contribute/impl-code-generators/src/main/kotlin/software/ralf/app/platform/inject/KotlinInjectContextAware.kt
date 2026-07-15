package software.ralf.app.platform.inject

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.Scope
import software.ralf.app.platform.ksp.ContextAware

@Suppress("TooManyFunctions")
internal interface KotlinInjectContextAware : ContextAware {
  val injectFqName
    get() = Inject::class.requireQualifiedName()

  private val scopeFqName
    get() = Scope::class.requireQualifiedName()

  fun KSAnnotation.isKotlinInjectScopeAnnotation(): Boolean {
    return annotationType.resolve().isKotlinInjectScopeAnnotation()
  }

  private fun KSType.isKotlinInjectScopeAnnotation(): Boolean {
    return declaration.annotations.any {
      it.annotationType.resolve().declaration.requireQualifiedName() == scopeFqName
    }
  }
}
