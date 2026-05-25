import SwiftUI

struct ContentView: View {
    @Environment(SessionStore.self) private var sessionStore
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if sessionStore.isSignedIn {
                WelcomeView()
            } else {
                LandingView()
            }
        }
        .task {
            sessionStore.signOutIfSessionExpired()
        }
        .onChange(of: scenePhase) { _, newPhase in
            guard newPhase == .active else {
                return
            }
            sessionStore.signOutIfSessionExpired()
        }
    }
}

#Preview("Signed Out") {
    ContentView()
        .environment(SessionStore.previewSignedOut)
}

#Preview("Signed In") {
    ContentView()
        .environment(SessionStore.previewSignedIn)
}
