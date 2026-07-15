package software.ralf.app.platform.metro.compiler.scoped

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.fir.AppPlatformMetroExtensionsDiagnostics

internal object ContributesScopedChecker : FirClassChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirClass) {
    declaration.source ?: return
    val session = context.session
    val classSymbol = declaration.symbol as? FirRegularClassSymbol ?: return

    val contributesScopedAnnotation =
      declaration.annotations.firstOrNull { candidate ->
        candidate.toAnnotationClassId(session) == ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID
      }
    if (contributesScopedAnnotation != null) {
      if (declaration.classKind != ClassKind.CLASS) {
        reporter.reportOn(
          contributesScopedAnnotation.source ?: declaration.source,
          AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_SCOPED_ERROR,
          "@ContributesScoped can only be applied to classes, not " +
            "${declaration.classKind.name.lowercase().replace('_', ' ')}s.",
        )
        return
      }

      if (!implementsScoped(classSymbol, session)) {
        reporter.reportOn(
          contributesScopedAnnotation.source ?: declaration.source,
          AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_SCOPED_ERROR,
          "In order to use @ContributesScoped, ${classSymbol.name.asString()} must implement " +
            "${ClassIds.SCOPED.asSingleFqName()}.",
        )
        return
      }

      if (directOtherSupertypes(classSymbol, session).size > 1) {
        reporter.reportOn(
          contributesScopedAnnotation.source ?: declaration.source,
          AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_SCOPED_ERROR,
          "In order to use @ContributesScoped, ${classSymbol.name.asString()} is allowed to " +
            "have only one other super type besides Scoped.",
        )
        return
      }

      if (
        !hasScopedInjectAnnotation(classSymbol, session) && scopedConstructors(classSymbol).size > 1
      ) {
        reporter.reportOn(
          contributesScopedAnnotation.source ?: declaration.source,
          AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_SCOPED_ERROR,
          "${classSymbol.name.asString()} has multiple constructors. Annotate the constructor " +
            "to use with @Inject, or remove the extra constructors so @ContributesScoped can " +
            "generate a provider.",
        )
        return
      }
    }

    val contributesBindingAnnotation =
      declaration.annotations.firstOrNull { candidate ->
        candidate.toAnnotationClassId(session) == ClassIds.CONTRIBUTES_BINDING
      } ?: return

    if (implementsScoped(classSymbol, session)) {
      reporter.reportOn(
        contributesBindingAnnotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_SCOPED_ERROR,
        "${classSymbol.name.asString()} implements Scoped, but uses @ContributesBinding " +
          "instead of @ContributesScoped. When implementing Scoped the annotation " +
          "@ContributesScoped must be used instead of @ContributesBinding to bind both super " +
          "types correctly. It's not necessary to use @ContributesBinding.",
      )
    }
  }
}
