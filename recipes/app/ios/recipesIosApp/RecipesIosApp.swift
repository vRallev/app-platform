//
//  RecipesIosApp.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/10/25.
//

import SwiftUI
import RecipesApp

class AppDelegate: NSObject, UIApplicationDelegate, RootScopeProvider {
    let demoApplication: DemoApplication = DemoApplication()
    
    var rootScope: Scope {
        get { demoApplication.rootScope }
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        demoApplication.create(appComponent: IosAppComponentKt.createIosAppComponent(application: application, rootScopeProvider: demoApplication))
        return true
    }
}

@main
struct recipesIosApp: App {
    
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    var body: some Scene {
        WindowGroup {
            ContentView(appDelegate: appDelegate)
        }
    }
}
