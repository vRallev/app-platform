//
//  AppPlatform+Extensions.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import RecipesApp
import SwiftUI

extension Presenter {
    /// Returns an async sequence of type `Model` from a `Presenter` model `StateFlow`.
    func viewModels<Model>(ofType type: Model.Type) -> AsyncThrowingStream<Model, Error> {
        model
            .values()
            .compactMap { $0 as? Model }
            .asAsyncThrowingStream()
    }
}

enum KotlinFlowError {
    case unexpectedValueInKotlinFlow(value: Any, expectedType: String)
}

extension Kotlinx_coroutines_coreFlow {

    /// Returns an async sequence of Any? from the Kotlin Flow.
    ///
    /// The Flows send Any, so we lose type information and need to cast at runtime instead of getting a type-safe compile time check.
    /// You can use `valuesOfType` instead which returns a stream that throws an error if the values are not of the right type.
    /// `valuesOfType` is usually preferred because we want to catch bad values from Kotlin instead of the Flow going silent.
    func values() -> AsyncThrowingStream<Any?, Error> {
        let collector = Kotlinx_coroutines_coreFlowCollectorImpl<Any?>()
        collect(collector: collector, completionHandler: collector.onComplete(_:))
        return collector.values
    }

    /// Returns an async sequence from the Kotlin Flow.
    ///
    /// The Flows send Any, so we lose type information and need to cast at runtime instead of getting a type-safe compile time check.
    /// If the Flow sends the right type, this stream will throw an error.
    /// This is usually preferred because we want to catch bad values from Kotlin instead of the Flow going silent.
    func valuesOfType<T>(_ type: T.Type = T.self) -> AsyncThrowingStream<T, Error> {
        let collector = Kotlinx_coroutines_coreFlowCollectorImpl<T>()
        Task { @MainActor in
            do {
                try await collect(collector: collector)
                collector.onComplete(nil)
            } catch {
                collector.onComplete(error)
            }
        }
        return collector.values
    }
}

fileprivate class Kotlinx_coroutines_coreFlowCollectorImpl<Value>: Kotlinx_coroutines_coreFlowCollector {

    let values: AsyncThrowingStream<Value, Error>
    private let continuation: AsyncThrowingStream<Value, Error>.Continuation

    init() {
        let (values, continuation) = AsyncThrowingStream<Value, Error>.makeStream()
        self.values = values
        self.continuation = continuation
    }

    func emit(value: Any?) async throws {
        if let castedValue = value as? Value {
            continuation.yield(castedValue)
        }
    }

    func onComplete(_ error: Error?) {
        continuation.finish(throwing: error)
    }

    deinit {
        print("Deiniting collector")
    }
}

extension AsyncSequence {

    func asAsyncThrowingStream() -> AsyncThrowingStream<Element, Error> {
        if let self = self as? AsyncThrowingStream<Element, Error> {
            return self
        }
        var asyncIterator = self.makeAsyncIterator()
        return AsyncThrowingStream<Element, Error> {
            try await asyncIterator.next()
        }
    }
}

extension Int {
    /// Converts Swift Int to Kotlin's Int type for interop.
    func toKotlinInt() -> KotlinInt {
        return KotlinInt(integerLiteral: self)
    }
}

extension BaseModel {
    /// Gets the view for some `BaseModel`
    ///
    /// Returns. created by `makeViewRenderer()` if a model conforms to `PresenterViewModel` otherwise, crash the build for
    /// debug builds or return a default view.
    @MainActor func getViewRenderer() -> AnyView {
        guard let viewModel = self as? (any PresenterViewModel) else {
            assertionFailure("ViewModel \(self) does not conform to `PresenterViewModel`")
            
            // This is an implementation detail. If crashing is preferred even in production builds, `fatalError(..)`
            // can be used instead
            return AnyView(Text("Error, some ViewModel was not implemented!"))
        }

        return AnyView(viewModel.makeViewRenderer())
    }
}
