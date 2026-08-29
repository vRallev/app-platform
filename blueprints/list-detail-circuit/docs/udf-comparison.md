# Unidirectional dataflow comparison

Both blueprints use Compose runtime in their presenters. A presenter reads observable state,
returns a value for the UI, and handles events that change state or navigation. The next
composition produces the next value. Neither implementation puts repository mutations or
navigation decisions in a list row's UI code.

The differences are in the contracts and how the application connects them:

| Concern | App Platform blueprint | Circuit blueprint |
| --- | --- | --- |
| Feature entrypoint | `ListDetailPresenter` returns a `BaseModel` | `ListDetailScreen` selects a presenter/UI pair through Circuit factories |
| Presenter contract | `MoleculePresenter<Input, Model>.present(input)` | `Presenter<State>.present()`; screen arguments and navigators are assisted constructor inputs |
| UI state | Models implement `BaseModel` and carry named callbacks | States implement `CircuitUiState` and carry an `eventSink` for sealed `CircuitUiEvent` types |
| Child composition | Parents call child presenters and combine their models | Composite presenters call child presenters and combine their states |
| UI dispatch | `RendererFactory` looks up a renderer by model type; `ModelDelegate` unwraps parents | `Ui.Factory` looks up a UI by `Screen`; tablet UI receives the two concrete child states |
| Phone navigation | Presenter entries in an App Platform backstack, rendered by Navigation 3 | Serializable `Screen` entries in Circuit's `SaveableNavStack`, controlled by `Navigator` and rendered with Circuit's `NavDecoration` |
| Runtime lifetime | App Platform `Scope`, `MoleculeScope`, and a platform-owned `TemplateProvider` | Metro owns graph dependencies; the host composition owns presenter work and saveable selection/navigation |
| Headless integration | Start a template stream, unwrap delegated models, invoke callbacks | Resolve the root presenter from the production Circuit graph, collect states with Circuit's test API, send events |

Circuit's test library uses Molecule internally to run composable presenters without Compose UI.
This experiment changes the application framework while keeping the underlying Compose state
model and the Metro dependency-injection system comparable.

## Trace a phone selection

In the [App Platform implementation](../../list-detail/list-detail/impl/src/commonMain/kotlin/software/ralf/app/platform/listdetail/PhoneListDetailPresenter.kt),
the parent replaces the list model's `onCharacterSelected` callback. That callback updates the
shared selection and pushes a `CharacterDetailPresenter` onto the presenter backstack.
The renderer factory resolves the pushed presenter's model to its UI.

In the Circuit version:

1. [`CharacterListUi`](../list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/CharacterListUi.kt)
   sends `Event.SelectCharacter(character.id)` to the state's event sink.
2. [`CharacterListPresenter`](../list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/CharacterListPresenter.kt)
   calls `navigator.goTo(CharacterDetailScreen(characterId))`.
3. [`PhoneListDetailPresenter`](../list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/PhoneListDetailPresenter.kt)
   wraps Circuit's navigator to record the selection when navigation succeeds. The real Circuit
   stack and selection change in one Compose snapshot.
4. The composite presenter reads the stack and composes each entry's presenter with the same
   production factories used for standalone Circuit screens. It emits an immutable snapshot
   containing the screen keys and their states.
5. [`PhoneListDetailUi`](../list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/PhoneListDetailUi.kt)
   uses Circuit's navigation decoration to animate the active entry and resolves that entry's UI
   through Circuit. It does not run a second copy of the presenter.

The detail toolbar sends `CharacterDetailPresenter.Event.Back`. System Back sends
`PhoneListDetailPresenter.Event.Back`. Both reach the same Circuit navigator and pop the detail
entry. The selected row stays highlighted when the list returns.

## Trace a tablet selection

[`TabletListDetailPresenter`](../list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/TabletListDetailPresenter.kt)
uses the same list and detail presenter factories. Its navigator adapts a detail-screen request
into a selection change, so choosing a character replaces the adjacent detail without pushing a
phone navigation entry. The child list presenter does not choose the layout.

The parent resolves the selected ID against the current repository value. If that ID is missing,
it selects the first character. If the repository is empty, it emits an explicit `Empty` state.
Detail presenters also observe the repository: editing a character updates its detail, and
removing a character from an open phone detail produces `Missing(characterId)`.

## State and restoration

[`ListDetailPresenter`](../list-detail/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/ListDetailPresenter.kt)
owns selection above the phone/tablet branch using `rememberSaveable`. This preserves selection
during resizing and Android recreation without making it an application singleton. Disposing
the feature and starting a fresh composition starts a new selection.

The phone branch owns its `rememberSaveableNavStack`. Switching to tablet removes that branch;
switching back creates a fresh list stack with the retained selection highlighted. This matches
the original blueprint rather than reopening its former phone detail.

[`AppGraph`](../app-framework/impl/src/commonMain/kotlin/software/ralf/circuit/listdetail/AppGraph.kt)
registers the screen serializers with `SerializableCircuitSaver`, so Android can save and restore
the phone stack during Activity recreation. Each phone UI entry has a saved-state slot for UI state
such as list scroll position. Navigation identity uses the record key, so a repository or selection
update does not become a new navigation transition. A popped entry keeps its last state until its
exit animation finishes.

The original Android `TemplateProvider` lives in a ViewModel and keeps its separate presenter
composition running across Activity recreation. Here `CircuitContent` belongs to the Activity's
UI composition. Recreation restores the saved selection and stack, while presenter effects and
flow collection restart. The behavior is equivalent for this in-memory app; preserving in-flight
work across recreation would require an explicit retained owner. The headless tests do not exercise
the UI host's saved-state machinery, so the Android recreation test verifies that separately.

## Why a composite presenter

Circuit's [official inbox sample](https://github.com/slackhq/circuit/tree/0.38.0/samples/inbox)
demonstrates composing child presenters and using a selection navigator for list/detail UI.
This blueprint uses that pattern so the complete production presentation can run in a headless
test, including adaptive decisions and the real phone stack.

It uses Circuit's public `NavDecoration` API instead of delegating child presentation to
`NavigableCircuitContent`. Consequently, phone entries remain composed while they are on the
stack, and this host does not automatically provide navigation-result handling or a separate
presenter retention registry for each entry. These list/detail presenters do not need those
facilities. A larger flow with result passing or expensive background work should reconsider
that hosting choice and its headless test boundary.

The portrait, name, and age use Circuit's shared-element scopes. They animate only in phone
navigation; tablet panes and previews without an animated scope use the unchanged modifier.
CircuitX's gesture decoration supports predictive Back on Android 14 and later, and an interactive
pop gesture on iOS. Its completed pop is forwarded as the same typed Back event; the presenter still
owns the navigation change. Older Android versions, Desktop, and Wasm use Circuit's default
transitions. Transition styling follows Circuit, so its motion is not pixel-for-pixel identical to
Navigation 3.

## App Platform's remaining role

The Gradle plugin enables the `:public`, `:impl`, `:testing`, and `:impl-robots` module conventions
and `checkModuleStructureDependencies`. Its automatic runtime dependency injection is explicitly
disabled with `addPublicModuleDependencies(false)`. Compose, Metro, serialization, and Circuit
are configured directly.

The application assembly module is the scoped exception that can import concrete implementations,
as in the original blueprint. No App Platform scopes, presenters, renderers, robot registration,
or test utilities are used at runtime.
