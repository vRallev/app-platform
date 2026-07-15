package software.ralf.app.platform.metro.compiler.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.psi.KtElement

internal object AppPlatformMetroExtensionsDiagnostics : KtDiagnosticsContainer() {
  val CONTRIBUTES_RENDERER_ERROR by
    error1<KtElement, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)

  val CONTRIBUTES_ROBOT_ERROR by
    error1<KtElement, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)

  val CONTRIBUTES_SCOPED_ERROR by
    error1<KtElement, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)

  override fun getRendererFactory(): BaseDiagnosticRendererFactory {
    return AppPlatformMetroExtensionsErrorMessages
  }
}

private object AppPlatformMetroExtensionsErrorMessages : BaseDiagnosticRendererFactory() {
  override val MAP by
    KtDiagnosticFactoryToRendererMap("AppPlatformMetroExtensions") { map ->
      map.put(
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_RENDERER_ERROR,
        "{0}",
        CommonRenderers.STRING,
      )
      map.put(
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_ROBOT_ERROR,
        "{0}",
        CommonRenderers.STRING,
      )
      map.put(
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_SCOPED_ERROR,
        "{0}",
        CommonRenderers.STRING,
      )
    }
}
