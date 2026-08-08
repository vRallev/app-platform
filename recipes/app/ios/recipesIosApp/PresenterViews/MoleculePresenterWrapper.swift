//
//  MoleculePresenterWrapper.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import RecipesApp

/// Wraps a Molecule Presenter that has been converted into a regular Presenter.
///
/// In order to convert a Molecule Presenter to a regular Presenter, we need to create a MoleculeScope,
/// and that scope needs to be cancelled when we are done,
/// so we create this class which will automatically cancel the scope upon deinit.
class MoleculePresenterWrapper: Presenter {
    var model: Kotlinx_coroutines_coreStateFlow { wrapped.model }

    private let wrapped: Presenter
    private let scope: MoleculeScope

    init(moleculeScopeFactory: MoleculeScopeFactory, moleculePresenter: MoleculePresenter, input: Any) {
        let scope = moleculeScopeFactory.createMoleculeScope()
        self.scope = scope
        self.wrapped = scope.launchMoleculePresenter(presenter: moleculePresenter, input: input)
    }

    deinit {
        scope.cancel()
    }

}
