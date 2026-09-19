//
//  ComposePresenterWrapper.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import RecipesApp

/// Wraps a Compose Presenter that has been converted into a regular Presenter.
///
/// In order to convert a Compose Presenter to a regular Presenter, we need to create a ComposePresenterScope,
/// and that scope needs to be cancelled when we are done,
/// so we create this class which will automatically cancel the scope upon deinit.
class ComposePresenterWrapper: Presenter {
    var model: Kotlinx_coroutines_coreStateFlow { wrapped.model }

    private let wrapped: Presenter
    private let scope: ComposePresenterScope

    init(composePresenterScopeFactory: ComposePresenterScopeFactory, composePresenter: ComposePresenter, input: Any) {
        let scope = composePresenterScopeFactory.createComposePresenterScope()
        self.scope = scope
        self.wrapped = scope.launchComposePresenter(presenter: composePresenter, input: input)
    }

    deinit {
        scope.cancel()
    }

}
