package software.ralf.app.platform.metro.compiler

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.MetroOptions
import dev.zacsweers.metro.compiler.api.fir.MetroContributionHintExtension
import dev.zacsweers.metro.compiler.api.fir.MetroContributionHintExtension.ContributionHint
import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import software.ralf.app.platform.metro.compiler.fir.extractScopeClassId
import software.ralf.app.platform.metro.compiler.renderer.ContributesRendererIds
import software.ralf.app.platform.metro.compiler.renderer.rendererContributionMetadata
import software.ralf.app.platform.metro.compiler.robot.ContributesRobotIds
import software.ralf.app.platform.metro.compiler.scoped.ContributesScopedIds
import software.ralf.app.platform.metro.compiler.scoped.contributesScopedMetadata

/** Supplies source-class hints for binding containers that App Platform generates in IR. */
internal class AppPlatformContributionHintExtension(private val session: FirSession) :
  MetroContributionHintExtension {

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesRendererIds.PREDICATE)
    register(ContributesRobotIds.PREDICATE)
    register(ContributesScopedIds.PREDICATE)
    register(ContributesScopedIds.SINGLE_IN_PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return buildList {
      annotatedClasses(ContributesRendererIds.PREDICATE).forEach { sourceClass ->
        if (rendererContributionMetadata(sourceClass, session) != null) {
          add(ContributionHint(sourceClass.classId, ClassIds.RENDERER_SCOPE))
        }
      }
      annotatedClasses(ContributesRobotIds.PREDICATE).forEach { sourceClass ->
        extractScopeClassId(
            sourceClass,
            ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID,
            session,
          )
          ?.let { scope -> add(ContributionHint(sourceClass.classId, scope)) }
      }
      annotatedClasses(ContributesScopedIds.PREDICATE).forEach { sourceClass ->
        if (contributesScopedMetadata(sourceClass, session) == null) return@forEach
        extractScopeClassId(
            sourceClass,
            ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID,
            session,
          )
          ?.let { scope -> add(ContributionHint(sourceClass.classId, scope)) }
      }
    }
      .distinct()
  }

  private fun annotatedClasses(
    predicate: org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
  ): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(predicate)
      .filterIsInstance<FirRegularClassSymbol>()
  }
}

@AutoService(MetroContributionHintExtension.Factory::class)
public class AppPlatformContributionHintExtensionFactory : MetroContributionHintExtension.Factory {
  override fun create(
    session: FirSession,
    options: MetroOptions,
    compatContext: CompatContext,
  ): MetroContributionHintExtension? {
    if (!options.generateClassesInIr) return null
    return AppPlatformContributionHintExtension(session)
  }
}
