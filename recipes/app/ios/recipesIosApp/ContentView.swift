//
//  ContentView.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/10/25.
//

import SwiftUI
import RecipesApp

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

struct ContentView: View {
    var appDelegate: AppDelegate
    
    @State var showComposeRecipes = false
    @State var showSwiftUIRecipe = false

    init(appDelegate: AppDelegate) {
        self.appDelegate = appDelegate
    }

    var body: some View {
        VStack {
            Spacer()
            
            Button(action: { showComposeRecipes.toggle() }) {
                Text("CMP-rendered recipes")
            }
            .buttonStyle(.borderedProminent)
            
            Spacer()
            
            Button(action: { showSwiftUIRecipe.toggle() }) {
                Text("SwiftUI recipe")
            }
            .buttonStyle(.borderedProminent)
            
            Spacer()
        }
        .sheet(isPresented: $showComposeRecipes) {
            ComposeView(rootScopeProvider: appDelegate)
                .ignoresSafeArea(.keyboard) // Compose has its own keyboard handler
        }
        .sheet(isPresented: $showSwiftUIRecipe) {
            SwiftUiRootPresenterView(
                homePresenter: SwiftUiHomePresenterBuilder(appDelegate: appDelegate).makeHomePresenter()
            )
        }
    }
}
