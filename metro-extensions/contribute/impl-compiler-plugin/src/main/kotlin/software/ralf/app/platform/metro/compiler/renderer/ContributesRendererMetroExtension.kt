package software.ralf.app.platform.metro.compiler.renderer

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
import software.ralf.app.platform.metro.compiler.ClassIds

public class ContributesRendererMetroExtension(private val session: FirSession) :
  MetroContributionExtension {

  private val predicate = ContributesRendererIds.PREDICATE

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
    if (scopeClassId != ClassIds.RENDERER_SCOPE) return emptyList()

    return annotatedClasses.mapNotNull { parentSymbol ->
      val contributionInterfaceClassId =
        parentSymbol.classId.createNestedClassId(ContributesRendererIds.NESTED_INTERFACE_NAME)
      val contributionSymbol =
        session.symbolProvider.getClassLikeSymbolByClassId(contributionInterfaceClassId)
          as? FirRegularClassSymbol ?: return@mapNotNull null
      val scope = contributionSymbol.declaredMemberScope(session, memberRequiredPhase = null)
      val metroContributionName =
        scope.getClassifierNames().firstOrNull { it.identifier.startsWith("MetroContributionTo") }
          ?: return@mapNotNull null
      val metroContributionSymbol =
        scope.getSingleClassifier(metroContributionName) as? FirRegularClassSymbol
          ?: return@mapNotNull null

      MetroContributionExtension.Contribution(
        supertype = metroContributionSymbol.defaultType(),
        replaces = emptyList(),
        originClassId = parentSymbol.classId,
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
      return ContributesRendererMetroExtension(session)
    }
  }
}
