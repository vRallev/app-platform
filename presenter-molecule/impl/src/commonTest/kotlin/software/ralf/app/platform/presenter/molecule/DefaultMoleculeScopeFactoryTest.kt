package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope

class DefaultMoleculeScopeFactoryTest {
  @Test
  fun `the default provided coroutine scope is used when creating a new MoleculeScope`() {
    val factory = factory(scope = CoroutineScope(CoroutineName("abc")))
    val moleculeScope = factory.createMoleculeScope()

    assertThat(moleculeScope.name).isEqualTo("abc")
  }

  @Test
  fun `the given coroutine scope is used when creating a new MoleculeScope`() {
    val factory = factory(scope = CoroutineScope(CoroutineName("abc")))
    val moleculeScope =
      factory.createMoleculeScopeFromCoroutineScope(CoroutineScope(CoroutineName("def")))

    assertThat(moleculeScope.name).isEqualTo("def")
  }

  @Test
  fun `default coroutine context elements are applied when creating a new MoleculeScope`() {
    val factory = factory(coroutineContext = CoroutineName("abc"))
    val moleculeScope = factory.createMoleculeScope()

    assertThat(moleculeScope.name).isEqualTo("abc")
  }

  @Test
  fun `the given coroutine context elements override default elements when creating a new MoleculeScope`() {
    val factory = factory(coroutineContext = CoroutineName("abc"))
    val moleculeScope =
      factory.createMoleculeScopeFromCoroutineScope(
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        coroutineContext = CoroutineName("def"),
      )

    assertThat(moleculeScope.name).isEqualTo("def")
  }

  private fun factory(
    scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) =
    DefaultMoleculeScopeFactory(
      coroutineScopeFactory = { scope },
      coroutineContext = coroutineContext,
      recompositionMode = RecompositionMode.Immediate,
    )

  private val MoleculeScope.name: String
    get() = requireNotNull(coroutineScope.coroutineContext[CoroutineName.Key]?.name)
}
