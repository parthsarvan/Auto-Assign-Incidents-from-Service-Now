import Foundation
import Observation

@MainActor
@Observable
final class SessionStore {
    private let apiClient: InciTeamAPIClient
    private let storage: SessionStorage
    private let profileImageStorage = ProfileImageStorage()
    private let defaults = UserDefaults.standard
    private var notificationService: PushNotificationService?
    private(set) var session: StoredSession?
    private(set) var displayFirstName: String?
    private(set) var profileImageData: Data?

    var isSignedIn: Bool {
        session != nil
    }

    var currentUser: AuthenticatedUser? {
        session?.user
    }

    var displayName: String {
        if let firstName = currentUser?.firstName?.trimmingCharacters(in: .whitespacesAndNewlines), !firstName.isEmpty {
            return firstName
        }
        if let rosterFirstName = displayFirstName, !rosterFirstName.isEmpty {
            return rosterFirstName
        }
        return currentUser?.username ?? "InciTeam user"
    }

    init(
        apiClient: InciTeamAPIClient? = nil,
        storage: SessionStorage? = nil,
        restoreStoredSession: Bool = true,
        initialSession: StoredSession? = nil
    ) {
        self.apiClient = apiClient ?? InciTeamAPIClient()
        self.storage = storage ?? KeychainSessionStorage()
        self.session = initialSession

        if restoreStoredSession {
            restoreSession()
        }
    }

    func signIn(username: String, password: String) async throws {
        let normalizedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedUsername.isEmpty, !password.isEmpty else {
            throw SessionStoreError.missingCredentials
        }

        let response = try await apiClient.signIn(username: normalizedUsername, password: password)
        let storedSession = StoredSession(
            token: response.token,
            user: AuthenticatedUser(response: response)
        )
        try storage.saveSession(storedSession)
        session = storedSession
        loadLocalProfileState(for: storedSession.user.id)
        if let firstName = response.firstName?.trimmingCharacters(in: .whitespacesAndNewlines), !firstName.isEmpty {
            displayFirstName = firstName
            defaults.set(firstName, forKey: firstNameDefaultsKey(userId: storedSession.user.id))
        }
        Task {
            await refreshProfileDetails()
        }
        await notificationService?.requestAuthorizationAndRegister()
    }

    func signOut() {
        let existingSession = session
        Task {
            await notificationService?.unregisterCurrentDeviceToken(session: existingSession)
        }
        clearStoredSession()
    }

    func signOutIfSessionExpired() {
        guard session?.isExpired == true else {
            return
        }
        clearStoredSession()
    }

    func clearSessionAfterAccountDeletion() {
        if let userId = currentUser?.id {
            profileImageStorage.deleteImageData(userId: userId)
            defaults.removeObject(forKey: firstNameDefaultsKey(userId: userId))
        }
        clearStoredSession()
    }

    func configureNotificationService(_ notificationService: PushNotificationService) {
        self.notificationService = notificationService
        Task {
            guard session != nil else {
                return
            }
            await notificationService.requestAuthorizationAndRegister()
            await notificationService.syncDeviceTokenIfPossible(session: session)
        }
    }

    func receiveDeviceToken(_ deviceToken: String) async {
        notificationService?.rememberDeviceToken(deviceToken)
        await notificationService?.syncDeviceTokenIfPossible(session: session)
    }

    func refreshProfileDetails() async {
        guard let session,
              let workEmail = session.user.workEmail?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
              !workEmail.isEmpty else {
            return
        }

        do {
            let members = try await apiClient.fetchTeamMembers(token: session.token)
            guard let matchedMember = members.first(where: {
                $0.email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == workEmail
            }) else {
                return
            }
            let firstName = matchedMember.firstName.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !firstName.isEmpty else {
                return
            }
            displayFirstName = firstName
            defaults.set(firstName, forKey: firstNameDefaultsKey(userId: session.user.id))
        } catch {
            return
        }
    }

    func saveProfileImageData(_ data: Data) throws {
        guard let userId = currentUser?.id else {
            return
        }
        profileImageData = try profileImageStorage.saveImageData(data, userId: userId)
    }

    private func restoreSession() {
        do {
            guard let storedSession = try storage.loadSession() else {
                return
            }
            if storedSession.isExpired {
                try? storage.clearSession()
                return
            }
            session = storedSession
            loadLocalProfileState(for: storedSession.user.id)
        } catch {
            try? storage.clearSession()
            session = nil
        }
    }

    private func loadLocalProfileState(for userId: Int64) {
        displayFirstName = defaults.string(forKey: firstNameDefaultsKey(userId: userId))
        profileImageData = profileImageStorage.loadImageData(userId: userId)
    }

    private func clearStoredSession() {
        try? storage.clearSession()
        session = nil
        displayFirstName = nil
        profileImageData = nil
    }

    private func firstNameDefaultsKey(userId: Int64) -> String {
        "inciteam.firstName.\(userId)"
    }
}

extension SessionStore {
    static var previewSignedOut: SessionStore {
        SessionStore(
            storage: PreviewSessionStorage(),
            restoreStoredSession: false
        )
    }

    static var previewSignedIn: SessionStore {
        SessionStore(
            storage: PreviewSessionStorage(),
            restoreStoredSession: false,
            initialSession: StoredSession(
                token: "preview",
                user: AuthenticatedUser(
                    response: SignInResponse(
                        token: "preview",
                        userId: 5,
                        username: "psarvanT",
                        firstName: "Parth",
                        lastName: "Sarvan",
                        workEmail: "psarvan@test.com",
                        role: "Admin",
                        workspace: WorkspaceSummary(
                            organizationId: 3,
                            organizationName: "Test",
                            teamId: 3,
                            teamName: "Team A",
                            teamRole: "TEAM_ADMIN",
                            teamTimezone: "America/Los_Angeles"
                        )
                    )
                )
            )
        )
    }
}

enum SessionStoreError: LocalizedError {
    case missingCredentials

    var errorDescription: String? {
        switch self {
        case .missingCredentials:
            return "Enter your username and password."
        }
    }
}
