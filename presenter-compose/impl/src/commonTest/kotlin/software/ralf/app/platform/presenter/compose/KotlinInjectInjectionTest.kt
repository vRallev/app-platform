package software.ralf.app.platform.presenter.compose

import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import software.ralf.app.platform.presenter.PresenterCoroutineScope
import software.ralf.app.platform.presenter.PresenterCoroutineScopeComponent
import software.ralf.app.platform.scope.coroutine.MainCoroutineDispatcher

class KotlinInjectInjectionTest {

  @Test
  fun `the PresenterCoroutineScope can be injected lazily`() {
    val testScope = CoroutineScope(CoroutineName("TestName"))
    val testDispatcher = Dispatchers.Default

    val component = createTestComponent(testScope, testDispatcher)

    val composePresenterScope = component.composePresenterScopeFactory.createComposePresenterScope()

    assertThat(composePresenterScope.coroutineScope.coroutineContext[CoroutineName.Key]?.name)
      .isEqualTo("TestName")

    composePresenterScope.cancel()
  }
}

@Component
@SingleIn(AppScope::class)
abstract class KotlinInjectTestComponent(
  private val coroutineScope: CoroutineScope,
  private val coroutineDispatcher: CoroutineDispatcher,
) : PresenterCoroutineScopeComponent {
  abstract val composePresenterScopeFactory: ComposePresenterScopeFactory

  @Provides
  @ForScope(AppScope::class)
  fun provideAppScopeCoroutineScope(): CoroutineScope = coroutineScope

  @Provides
  @MainCoroutineDispatcher
  fun provideMainCoroutineDispatcher(): CoroutineDispatcher = coroutineDispatcher

  @Provides
  fun provideComposePresenterScopeFactory(
    factory: KotlinInjectTestComposePresenterScopeFactory
  ): ComposePresenterScopeFactory = factory
}

@Inject
@SingleIn(AppScope::class)
class KotlinInjectTestComposePresenterScopeFactory(
  @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
) :
  ComposePresenterScopeFactory by DefaultComposePresenterScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    coroutineContext = EmptyCoroutineContext,
    recompositionMode = RecompositionMode.Immediate,
  )

@KmpComponentCreate
expect fun createTestComponent(
  coroutineScope: CoroutineScope,
  coroutineDispatcher: CoroutineDispatcher,
): KotlinInjectTestComponent
