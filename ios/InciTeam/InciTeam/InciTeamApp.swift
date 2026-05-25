//
//  InciTeamApp.swift
//  InciTeam
//
//  Created by Parth Sarvan on 5/20/26.
//

import SwiftUI

@main
struct InciTeamApp: App {
    @UIApplicationDelegateAdaptor(PushNotificationAppDelegate.self) private var appDelegate
    @State private var sessionStore = SessionStore()
    @State private var pushNotificationService = PushNotificationService()
    @State private var openedIncidentNotification: IncidentNotificationDetail?

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(sessionStore)
                .sheet(item: $openedIncidentNotification) { detail in
                    IncidentNotificationDetailView(detail: detail) {
                        openedIncidentNotification = nil
                    }
                }
                .task {
                    sessionStore.configureNotificationService(pushNotificationService)
                }
                .task {
                    for await notification in NotificationCenter.default.notifications(
                        named: .inciTeamDidReceiveDeviceToken
                    ) {
                        guard let deviceToken = notification.userInfo?["deviceToken"] as? String else {
                            continue
                        }
                        await sessionStore.receiveDeviceToken(deviceToken)
                    }
                }
                .task {
                    for await _ in NotificationCenter.default.notifications(
                        named: .inciTeamAuthenticationExpired
                    ) {
                        sessionStore.signOutIfSessionExpired()
                    }
                }
                .task {
                    for await notification in NotificationCenter.default.notifications(
                        named: .inciTeamDidOpenRemoteNotification
                    ) {
                        guard let detail = IncidentNotificationDetail(userInfo: notification.userInfo ?? [:]) else {
                            continue
                        }
                        openedIncidentNotification = detail
                    }
                }
        }
    }
}
