import ListDetailApp
import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate, RootScopeProvider {

    private let listDetailApplication: Application = Application()

    var rootScope: Scope {
        get {
            listDetailApplication.rootScope
        }
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        listDetailApplication.create(
            appGraph: IosAppGraphKt.createIosAppGraph(
                application: application,
                rootScopeProvider: listDetailApplication
            )
        )
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
