package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import software.ralf.app.platform.presenter.PresenterCoroutineScope

class MetroInjectionTest {

  @Test
  fun `the PresenterCoroutineScope can be injected lazily`() {
    val testScope = CoroutineScope(CoroutineName("TestName"))

    val component = createGraphFactory<MetroTestComponent.Factory>().create(testScope)

    val moleculeScope = component.moleculeScopeFactory.createMoleculeScope()

    assertThat(moleculeScope.coroutineScope.coroutineContext[CoroutineName.Key]?.name)
      .isEqualTo("TestName")

    moleculeScope.cancel()
  }
}

@Suppress("unused")
@DependencyGraph
@SingleIn(AppScope::class)
interface MetroTestComponent {

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(
      @Provides @PresenterCoroutineScope coroutineScope: CoroutineScope
    ): MetroTestComponent
  }

  val moleculeScopeFactory: MoleculeScopeFactory

  @Binds val MetroTestMoleculeScopeFactory.bind: MoleculeScopeFactory
}

@Inject
@SingleIn(AppScope::class)
class MetroTestMoleculeScopeFactory(
  @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = { coroutineScopeFactory() },
    coroutineContext = EmptyCoroutineContext,
    recompositionMode = RecompositionMode.Immediate,
  )
