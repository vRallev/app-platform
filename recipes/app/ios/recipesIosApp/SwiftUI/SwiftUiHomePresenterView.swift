//
//  SwiftUiHomePresenterView.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import SwiftUI
import RecipesApp

extension SwiftUiHomePresenter.Model: PresenterViewModel {

    func makeViewRenderer() -> some View {
        SwiftUiHomePresenterView(model: self)
    }
}

// Creates a binding for the workflow presenter model backstack so we can provide it to
// NavigationStack. The backstack is indexed here as the type of the Binding needs to be hashable.
// SwiftUiHomePresenter.Model accepts a modified list of indices
extension SwiftUiHomePresenter.Model {
    func pathBinding() -> Binding<[Int]> {
        .init {
            // drop the first value of the backstack from the path because that should be the root view
            Array(self.modelBackstack.indices.dropFirst())
        } set: { modifiedIndices in

            // the resulting backstack indices the presenter should compute on is the first index (0) that was
            // dropped as well as the remaining indices post modification
            let indicesBackstack = [0] + modifiedIndices.map { $0.toKotlinInt() }

            self.onEvent(
                SwiftUiHomePresenterEventBackstackModificationEvent (
                    indicesBackstack: indicesBackstack
                )
            )
        }
    }
}

struct SwiftUiHomePresenterView: View {
    var model: SwiftUiHomePresenter.Model

    var body: some View {
        NavigationStackView(model: self.model)
    }
}

private struct NavigationStackView: View {
    var backstack: [BaseModel]
    var model: SwiftUiHomePresenter.Model

    init(model: SwiftUiHomePresenter.Model) {
        self.backstack = model.modelBackstack
        self.model = model
    }

    var body: some View {
        NavigationStack(path: model.pathBinding()) {
            backstack[0].getViewRenderer()
                .navigationDestination(for: Int.self) { index in
                    backstack[index].getViewRenderer()
                }
        }
    }
}
