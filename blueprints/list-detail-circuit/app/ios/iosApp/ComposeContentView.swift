import UIKit
import SwiftUI
import ListDetailCircuitApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ComposeContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.all)
    }
}
