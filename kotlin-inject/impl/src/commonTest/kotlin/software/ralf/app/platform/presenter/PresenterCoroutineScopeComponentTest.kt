package software.ralf.app.platform.presenter

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.coroutines.ContinuationInterceptor
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
import software.ralf.app.platform.scope.coroutine.MainCoroutineDispatcher

class PresenterCoroutineScopeComponentTest {

  @Test
  fun `the presenter scope combines the app scope with the main dispatcher`() {
    val appScope = CoroutineScope(CoroutineName("App"))
    val mainDispatcher = Dispatchers.Default

    val presenterScope =
      createPresenterCoroutineScopeTestComponent(appScope, mainDispatcher)
        .consumer
        .coroutineScope

    assertThat(presenterScope.coroutineContext[CoroutineName.Key]?.name).isEqualTo("App")
    assertThat(presenterScope.coroutineContext[ContinuationInterceptor.Key])
      .isSameInstanceAs(mainDispatcher)
  }
}

@Component
@SingleIn(AppScope::class)
abstract class PresenterCoroutineScopeTestComponent(
  private val appScope: CoroutineScope,
  private val mainDispatcher: CoroutineDispatcher,
) : PresenterCoroutineScopeComponent {
  abstract val consumer: PresenterCoroutineScopeConsumer

  @Provides
  @ForScope(AppScope::class)
  fun provideAppScope(): CoroutineScope = appScope

  @Provides
  @MainCoroutineDispatcher
  fun provideMainDispatcher(): CoroutineDispatcher = mainDispatcher
}

@Inject
class PresenterCoroutineScopeConsumer(
  @PresenterCoroutineScope val coroutineScope: CoroutineScope
)

@KmpComponentCreate
expect fun createPresenterCoroutineScopeTestComponent(
  appScope: CoroutineScope,
  mainDispatcher: CoroutineDispatcher,
): PresenterCoroutineScopeTestComponent
