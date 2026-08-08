//
//  SwiftUiChildPresenterView.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/23/25.
//

import RecipesApp
import SwiftUI

extension SwiftUiChildPresenter.Model: PresenterViewModel {
    func makeViewRenderer() -> some View {
        SwiftUiChildPresenterView(model: self)
    }
}

struct SwiftUiChildPresenterView: View {
    var model: SwiftUiChildPresenter.Model

    var body: some View {
        Text("Index: \(model.index)")
            .font(.system(size: 36))
        Text("Counter: \(model.counter)")
            .font(.system(size: 36))
        Button(action: { model.onEvent(SwiftUiChildPresenterEventAddPeer()) }) {
            Text("Add peer")
        }
        .buttonStyle(.borderedProminent)
    }
}
