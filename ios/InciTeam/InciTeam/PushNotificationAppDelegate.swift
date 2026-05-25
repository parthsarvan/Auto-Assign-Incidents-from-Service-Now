import UIKit
import UserNotifications

final class PushNotificationAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        NotificationCenter.default.post(
            name: .inciTeamDidReceiveDeviceToken,
            object: nil,
            userInfo: ["deviceToken": token]
        )
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        NotificationCenter.default.post(
            name: .inciTeamDidFailToRegisterForRemoteNotifications,
            object: nil,
            userInfo: ["error": error]
        )
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        NotificationCenter.default.post(
            name: .inciTeamDidOpenRemoteNotification,
            object: nil,
            userInfo: response.notification.request.content.userInfo
        )
    }
}

extension Notification.Name {
    static let inciTeamDidReceiveDeviceToken = Notification.Name("inciTeamDidReceiveDeviceToken")
    static let inciTeamDidFailToRegisterForRemoteNotifications = Notification.Name("inciTeamDidFailToRegisterForRemoteNotifications")
    static let inciTeamDidOpenRemoteNotification = Notification.Name("inciTeamDidOpenRemoteNotification")
}
