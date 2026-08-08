//
//  SwiftUiHomePresenterBuilder.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import RecipesApp

struct SwiftUiHomePresenterBuilder {
    private let appComponent: AppComponent
    
    init(appDelegate: AppDelegate) {
        self.appComponent = appDelegate.demoApplication.appComponent
    }

    func makeHomePresenter() -> Presenter {
        MoleculePresenterWrapper(
            moleculeScopeFactory: appComponent.moleculeScopeFactory,
            moleculePresenter: appComponent.swiftUiHomePresenter,
            input: Void()
        )
    }
}
