package software.ralf.circuit.listdetail

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.serialization.CircuitSerializerRegistration
import com.slack.circuit.serialization.SerializableCircuitSaver
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import software.ralf.circuit.listdetail.screen.DefaultScreenSizeProvider

@ContributesTo(AppScope::class)
interface AppGraph {
  val circuit: Circuit

  val screenSizeProvider: DefaultScreenSizeProvider

  @Multibinds val presenterFactories: Set<Presenter.Factory>

  @Multibinds val uiFactories: Set<Ui.Factory>

  @Provides
  @SingleIn(AppScope::class)
  fun provideCircuitSaver(): CircuitSaver =
    SerializableCircuitSaver(
      registrations =
        listOf(
          CircuitSerializerRegistration {
            it.subclass(ListDetailScreen::class, ListDetailScreen.serializer())
            it.subclass(CharacterListScreen::class, CharacterListScreen.serializer())
            it.subclass(CharacterDetailScreen::class, CharacterDetailScreen.serializer())
          }
        )
    )

  @Provides
  @SingleIn(AppScope::class)
  fun provideCircuit(
    presenterFactories: Set<Presenter.Factory>,
    uiFactories: Set<Ui.Factory>,
    circuitSaver: CircuitSaver,
  ): Circuit =
    Circuit.Builder()
      .addPresenterFactories(presenterFactories)
      .addUiFactories(uiFactories)
      .setAnimatedNavDecoratorFactory(GestureNavigationDecorationFactory())
      .setCircuitSaver(circuitSaver)
      .build()
}
