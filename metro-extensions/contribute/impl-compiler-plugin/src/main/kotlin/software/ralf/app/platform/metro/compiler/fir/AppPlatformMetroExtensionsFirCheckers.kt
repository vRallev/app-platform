package software.ralf.app.platform.metro.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import software.ralf.app.platform.metro.compiler.renderer.ContributesRendererChecker
import software.ralf.app.platform.metro.compiler.robot.ContributesRobotChecker
import software.ralf.app.platform.metro.compiler.scoped.ContributesScopedChecker

internal class AppPlatformMetroExtensionsFirCheckers(session: FirSession) :
  FirAdditionalCheckersExtension(session) {
  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val classCheckers: Set<FirClassChecker> =
        setOf(ContributesRendererChecker, ContributesRobotChecker, ContributesScopedChecker)
    }
}
