import UIKit
import SwiftUI
import TemplateApp

struct ComposeView: UIViewControllerRepresentable {
    private var rootScopeProvider: RootScopeProvider

    init(rootScopeProvider: RootScopeProvider) {
        self.rootScopeProvider = rootScopeProvider
    }

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(rootScopeProvider: rootScopeProvider)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ComposeContentView: View {
    var rootScopeProvider: RootScopeProvider

    init(rootScopeProvider: RootScopeProvider) {
        self.rootScopeProvider = rootScopeProvider
    }

    var body: some View {
        ComposeView(rootScopeProvider: rootScopeProvider).ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
