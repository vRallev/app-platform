//
//  PresenterViewModel.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import SwiftUI
import RecipesApp

/// A protocol for view models that create their own SwiftUI view representation.
protocol PresenterViewModel {
    associatedtype Renderer : View
    @ViewBuilder @MainActor func makeViewRenderer() -> Self.Renderer
}
