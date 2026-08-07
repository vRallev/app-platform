package software.ralf.app.platform.metro.compiler.robot

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.MetroOptions
import dev.zacsweers.metro.compiler.api.fir.MetroContributionExtension
import dev.zacsweers.metro.compiler.compat.CompatContext
import dev.zacsweers.metro.compiler.fir.MetroFirTypeResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import software.ralf.app.platform.metro.compiler.fir.extractScopeClassId

public class ContributesRobotMetroExtension(private val session: FirSession) :
  MetroContributionExtension {

  private val predicate = ContributesRobotIds.PREDICATE

  private val annotatedClasses by lazy {
    session.predicateBasedProvider
      .getSymbolsByPredicate(predicate)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(predicate)
  }

  override fun getContributions(
    scopeClassId: ClassId,
    typeResolverFactory: MetroFirTypeResolver.Factory,
  ): List<MetroContributionExtension.Contribution> {
    return annotatedClasses.flatMap { parentSymbol ->
      val annotationScopeClassId =
        extractScopeClassId(parentSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
          ?: return@flatMap emptyList()
      if (annotationScopeClassId != scopeClassId) return@flatMap emptyList()

      val contributionInterfaceClassId =
        parentSymbol.classId.createNestedClassId(ContributesRobotIds.NESTED_INTERFACE_NAME)
      val contributionSymbol =
        session.symbolProvider.getClassLikeSymbolByClassId(contributionInterfaceClassId)
          as? FirRegularClassSymbol ?: return@flatMap emptyList()
      val scope = contributionSymbol.declaredMemberScope(session, memberRequiredPhase = null)
      val metroContributionName =
        scope.getClassifierNames().firstOrNull { it.identifier.startsWith("MetroContributionTo") }
          ?: return@flatMap emptyList()
      val metroContributionSymbol =
        scope.getSingleClassifier(metroContributionName) as? FirRegularClassSymbol
          ?: return@flatMap emptyList()
      val robotGraphContributionClassId =
        parentSymbol.classId.createNestedClassId(
          ContributesRobotIds.NESTED_ROBOT_GRAPH_INTERFACE_NAME
        )
      val robotGraphContributionSymbol =
        session.symbolProvider.getClassLikeSymbolByClassId(robotGraphContributionClassId)
          as? FirRegularClassSymbol ?: return@flatMap emptyList()

      listOf(
        MetroContributionExtension.Contribution(
          supertype = metroContributionSymbol.defaultType(),
          replaces = emptyList(),
          originClassId = parentSymbol.classId,
        ),
        MetroContributionExtension.Contribution(
          supertype = robotGraphContributionSymbol.defaultType(),
          replaces = emptyList(),
          originClassId = parentSymbol.classId,
        ),
      )
    }
  }

  @AutoService(MetroContributionExtension.Factory::class)
  public class Factory : MetroContributionExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroContributionExtension {
      return ContributesRobotMetroExtension(session)
    }
  }
}
