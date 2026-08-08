//
//  PresenterView.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import SwiftUI
import RecipesApp

/// Displays the view model hieararchy from a root `Presenter`.
///
/// `PresenterView` can be instantiated by passing in a Kotlin `Presenter` or an `AsyncSequence` of `ViewModels`.
///
/// Note that `PresenterView` should only be used at the root of a `Presenter` hierarchy. `Presenters` are hierarchical. The view for a parent
/// `Presenter` model should also present the model of its children, so `PresenterView` is only be needed for the root parent `Presenter`.
struct PresenterView<Model: BaseModel>: View {
    @StateObject var viewModelObserver: ViewModelObserver

    init(presenter: Presenter, viewModelType: Model.Type, handleViewModelError: @escaping (Error) -> ()) {
        self.init(viewModels: presenter.viewModels(ofType: viewModelType), handleViewModelError: handleViewModelError)
    }

    init<ViewModels: AsyncSequence>(viewModels: ViewModels, handleViewModelError: @escaping (Error) -> ()) where ViewModels.Element == Model {
        self._viewModelObserver = StateObject(wrappedValue: ViewModelObserver(
            viewModels: viewModels,
            handleError: handleViewModelError
        ))
    }

    var body: some View {
        if let viewModel = viewModelObserver.viewModel {
            viewModel.getViewRenderer()
        }
    }

    @MainActor
    class ViewModelObserver: ObservableObject {
        @Published var viewModel: Model?
        private var task: Task<Void, Never>? = nil

        init<ViewModels: AsyncSequence>(viewModels: ViewModels, handleError: @escaping (Error) -> ()) where ViewModels.Element == Model {
            task = Task { @MainActor [weak self] in
                do {
                    for try await viewModel in viewModels {
                        self?.viewModel = viewModel
                    }
                } catch {
                    handleError(error)
                }
            }
        }

        deinit {
            task?.cancel()
        }
    }
}
