import UIKit
import UserNotifications

@MainActor
final class PushNotificationService {
    private let apiClient: InciTeamAPIClient
    private var currentDeviceToken: String?

    init() {
        self.apiClient = InciTeamAPIClient()
    }

    init(apiClient: InciTeamAPIClient) {
        self.apiClient = apiClient
    }

    func requestAuthorizationAndRegister() async {
        do {
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
            guard granted else {
                return
            }
            UIApplication.shared.registerForRemoteNotifications()
        } catch {
            return
        }
    }

    func rememberDeviceToken(_ deviceToken: String) {
        currentDeviceToken = deviceToken
    }

    func syncDeviceTokenIfPossible(session: StoredSession?) async {
        guard let session, let currentDeviceToken else {
            return
        }

        do {
            try await apiClient.registerMobileDeviceToken(
                currentDeviceToken,
                authToken: session.token,
                environment: apnsEnvironment
            )
        } catch {
            return
        }
    }

    func unregisterCurrentDeviceToken(session: StoredSession?) async {
        guard let session, let currentDeviceToken else {
            return
        }

        try? await apiClient.unregisterMobileDeviceToken(
            currentDeviceToken,
            authToken: session.token,
            environment: apnsEnvironment
        )
    }

    private var apnsEnvironment: String {
        #if DEBUG
        return "development"
        #else
        return "production"
        #endif
    }
}
