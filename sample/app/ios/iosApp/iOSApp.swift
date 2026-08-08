import SampleApp
import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate, RootScopeProvider {

    private let demoApplication: DemoApplication = DemoApplication()

    var rootScope: Scope {
        get {
            demoApplication.rootScope
        }
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        demoApplication.create(appGraph: IosAppGraphKt.createIosAppGraph(application: application, rootScopeProvider: demoApplication))
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
