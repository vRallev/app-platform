import TemplateApp
import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate, RootScopeProvider {

    private let templateApplication: Application = Application()

    var rootScope: Scope {
        get {
            templateApplication.rootScope
        }
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        templateApplication.create(appGraph: IosAppGraphKt.createIosAppGraph(application: application, rootScopeProvider: templateApplication))
        return true
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ComposeContentView(rootScopeProvider: appDelegate)
        }
    }
}
