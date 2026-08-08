//
//  SwiftUiRootPresenterView.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/23/25.
//

import SwiftUI
import RecipesApp

/// Creates the view model hierarchy for the root presenter of this recipe, `SwiftUiHomePresenter`.
struct SwiftUiRootPresenterView: View {
    var homePresenter: Presenter

    var body: some View {
        PresenterView(
            presenter: homePresenter,
            viewModelType: BaseModel.self,
            handleViewModelError: { error in
                fatalError("View model error occurred: \(error)")
            }
        )
    }
}
