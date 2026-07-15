package software.ralf.app.platform.metro.compiler.renderer

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import software.ralf.app.platform.metro.compiler.fir.AppPlatformMetroExtensionsDiagnostics

internal object ContributesRendererChecker : FirClassChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirClass) {
    declaration.source ?: return
    val session = context.session

    val annotation =
      declaration.annotations.firstOrNull { candidate ->
        candidate.toAnnotationClassId(session) ==
          ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID
      } ?: return

    val classSymbol = declaration.symbol as? FirRegularClassSymbol ?: return

    if (declaration.classKind != ClassKind.CLASS) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_RENDERER_ERROR,
        "@ContributesRenderer can only be applied to classes, not " +
          "${declaration.classKind.name.lowercase().replace('_', ' ')}s.",
      )
      return
    }

    when (val modelTypeResolution = resolveRendererModelType(classSymbol, session)) {
      is RendererModelTypeResolution.Error -> {
        reporter.reportOn(
          annotation.source ?: declaration.source,
          AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_RENDERER_ERROR,
          modelTypeResolution.message,
        )
      }

      is RendererModelTypeResolution.Success -> Unit
    }

    if (isSingleInRendererScope(classSymbol, session)) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_RENDERER_ERROR,
        "Renderers should not be singletons in the RendererScope. The RendererFactory will " +
          "cache the Renderer when necessary. Remove the @SingleIn(RendererScope::class) " +
          "annotation.",
      )
    }

    val parameterCount = injectedRendererConstructorParameterCount(classSymbol, session)
    if (hasRendererInjectAnnotation(classSymbol, session)) {
      if (parameterCount == 0) {
        reporter.reportOn(
          annotation.source ?: declaration.source,
          AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_RENDERER_ERROR,
          "It's redundant to use @Inject when using @ContributesRenderer for a Renderer with " +
            "a zero-arg constructor.",
        )
      }
    } else if (rendererConstructors(classSymbol).size > 1) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_RENDERER_ERROR,
        "${classSymbol.name.asString()} has multiple constructors. Annotate the constructor " +
          "to use with @Inject, or remove the extra constructors so @ContributesRenderer can " +
          "generate a provider.",
      )
    }
  }
}
