import SwiftUI

struct UserAccessView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var users: [UserSummary] = []
    @State private var teams: [TeamSummary] = []
    @State private var pendingRoles: [Int64: String] = [:]
    @State private var pendingTeamSelections: [Int64: Int64] = [:]
    @State private var pendingTeamRoles: [String: String] = [:]
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var successMessage = ""
    @State private var userToDelete: UserSummary?

    private let apiClient = InciTeamAPIClient()
    private let orgRoles = ["User", "Admin"]
    private let teamRoles = ["TEAM_ADMIN", "MANAGER", "MEMBER"]

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header

                    if !isGlobalAdmin {
                        adminOnlyCard
                    } else if isLoading {
                        ProgressView("Loading access...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else {
                        metrics

                        if !errorMessage.isEmpty {
                            AccessErrorCard(message: errorMessage) {
                                Task { await loadData() }
                            }
                        }

                        if !successMessage.isEmpty {
                            AccessNoticeCard(message: successMessage, color: .green)
                        }

                        if users.isEmpty {
                            AccessNoticeCard(message: "No organization users found.", color: InciTeamTheme.primary)
                        } else {
                            ForEach(users) { user in
                                UserAccessCard(
                                    user: user,
                                    teams: teams,
                                    orgRoles: orgRoles,
                                    teamRoles: teamRoles,
                                    pendingRole: roleBinding(for: user),
                                    pendingTeamSelection: teamSelectionBinding(for: user),
                                    pendingTeamRole: { membership in
                                        teamRoleBinding(userId: user.id, teamId: membership.teamId, fallback: membership.role)
                                    },
                                    onSaveRole: {
                                        Task { await updateRole(for: user) }
                                    },
                                    onAssignTeam: {
                                        Task { await assignTeam(to: user) }
                                    },
                                    onSaveTeamRole: { membership in
                                        Task { await updateTeamRole(for: user, membership: membership) }
                                    },
                                    onRemoveTeam: { membership in
                                        Task { await removeTeam(from: user, membership: membership) }
                                    },
                                    onDeleteAccount: {
                                        userToDelete = user
                                    }
                                )
                            }
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("User Access")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadData()
        }
        .refreshable {
            await loadData()
        }
        .alert("Delete account?", isPresented: deleteUserAlertBinding) {
            Button("Delete Account", role: .destructive) {
                guard let userToDelete else {
                    return
                }
                Task { await deleteUserAccount(userToDelete) }
            }
            Button("Cancel", role: .cancel) {
                userToDelete = nil
            }
        } message: {
            Text(deleteUserAlertMessage)
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "person.badge.key.fill")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(FeatureTone.red.color.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("User Access")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("Organization roles and team membership.")
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer()
        }
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var metrics: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 12)], spacing: 12) {
            AccessMetricCard(title: "Users", value: "\(users.count)", color: InciTeamTheme.primary)
            AccessMetricCard(title: "Teams", value: "\(teams.count)", color: .green)
            AccessMetricCard(title: "Mode", value: "Admin", color: .red)
        }
    }

    private var adminOnlyCard: some View {
        AccessNoticeCard(
            message: "User Access is available only to organization admins because it changes global roles and team memberships.",
            color: FeatureTone.red.color
        )
    }

    private var isGlobalAdmin: Bool {
        sessionStore.currentUser?.isGlobalAdmin ?? false
    }

    private var deleteUserAlertBinding: Binding<Bool> {
        Binding(
            get: { userToDelete != nil },
            set: { isPresented in
                if !isPresented {
                    userToDelete = nil
                }
            }
        )
    }

    private var deleteUserAlertMessage: String {
        guard let userToDelete else {
            return ""
        }
        if userToDelete.id == sessionStore.currentUser?.id {
            return "Delete your own account? You will be signed out immediately."
        }
        return "This removes team access, roster records, routing mappings, schedules, leaves, breaks, and push tokens."
    }

    private func loadData() async {
        guard isGlobalAdmin, let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            async let userData = apiClient.fetchUsers(token: token)
            async let teamData = apiClient.fetchWorkspaceTeams(token: token)
            users = try await userData.sorted { $0.username < $1.username }
            teams = try await teamData.sorted { $0.teamName < $1.teamName }
            reconcilePendingState()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func reconcilePendingState() {
        pendingRoles = Dictionary(uniqueKeysWithValues: users.map { ($0.id, $0.role) })
        pendingTeamSelections = [:]
        pendingTeamRoles = [:]
        for user in users {
            for membership in user.teamMemberships {
                pendingTeamRoles[teamRoleKey(userId: user.id, teamId: membership.teamId)] = membership.role
            }
        }
    }

    private func updateRole(for user: UserSummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }
        errorMessage = ""
        successMessage = ""
        do {
            _ = try await apiClient.updateUserRole(id: user.id, token: token, role: pendingRoles[user.id] ?? user.role)
            await loadData()
            successMessage = "Role updated."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func assignTeam(to user: UserSummary) async {
        guard let token = sessionStore.session?.token,
              let teamId = pendingTeamSelections[user.id] else {
            return
        }
        errorMessage = ""
        successMessage = ""
        do {
            _ = try await apiClient.assignUserToTeam(userId: user.id, teamId: teamId, token: token)
            await loadData()
            successMessage = "Team assigned."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func updateTeamRole(for user: UserSummary, membership: UserTeamMembershipSummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }
        let role = pendingTeamRoles[teamRoleKey(userId: user.id, teamId: membership.teamId)] ?? membership.role
        errorMessage = ""
        successMessage = ""
        do {
            _ = try await apiClient.updateUserTeamRole(userId: user.id, teamId: membership.teamId, token: token, role: role)
            await loadData()
            successMessage = "Team role updated."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func removeTeam(from user: UserSummary, membership: UserTeamMembershipSummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }
        errorMessage = ""
        successMessage = ""
        do {
            _ = try await apiClient.removeUserFromTeam(userId: user.id, teamId: membership.teamId, token: token)
            await loadData()
            successMessage = "Team access removed."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func deleteUserAccount(_ user: UserSummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }
        errorMessage = ""
        successMessage = ""
        do {
            let response = try await apiClient.deleteUserAccount(userId: user.id, token: token)
            userToDelete = nil
            if user.id == sessionStore.currentUser?.id {
                sessionStore.clearSessionAfterAccountDeletion()
                return
            }
            await loadData()
            successMessage = response.message ?? "Account deleted."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func roleBinding(for user: UserSummary) -> Binding<String> {
        Binding(
            get: { pendingRoles[user.id] ?? user.role },
            set: { pendingRoles[user.id] = $0 }
        )
    }

    private func teamSelectionBinding(for user: UserSummary) -> Binding<Int64?> {
        Binding(
            get: { pendingTeamSelections[user.id] },
            set: { teamId in
                if let teamId {
                    pendingTeamSelections[user.id] = teamId
                } else {
                    pendingTeamSelections.removeValue(forKey: user.id)
                }
            }
        )
    }

    private func teamRoleBinding(userId: Int64, teamId: Int64, fallback: String) -> Binding<String> {
        Binding(
            get: { pendingTeamRoles[teamRoleKey(userId: userId, teamId: teamId)] ?? fallback },
            set: { pendingTeamRoles[teamRoleKey(userId: userId, teamId: teamId)] = $0 }
        )
    }

    private func teamRoleKey(userId: Int64, teamId: Int64) -> String {
        "\(userId)-\(teamId)"
    }
}

private struct UserAccessCard: View {
    let user: UserSummary
    let teams: [TeamSummary]
    let orgRoles: [String]
    let teamRoles: [String]
    @Binding var pendingRole: String
    @Binding var pendingTeamSelection: Int64?
    let pendingTeamRole: (UserTeamMembershipSummary) -> Binding<String>
    let onSaveRole: () -> Void
    let onAssignTeam: () -> Void
    let onSaveTeamRole: (UserTeamMembershipSummary) -> Void
    let onRemoveTeam: (UserTeamMembershipSummary) -> Void
    let onDeleteAccount: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(user.displayName)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text(user.username)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(InciTeamTheme.muted)
                    if let currentTeamName = user.currentTeamName, !currentTeamName.isEmpty {
                        Text("Current team: \(currentTeamName)")
                            .font(.caption)
                            .foregroundStyle(InciTeamTheme.muted)
                    }
                }

                Spacer()

                Text(user.role)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(FeatureTone.red.color)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(FeatureTone.red.color.opacity(0.12), in: Capsule())
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Org Role")
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(InciTeamTheme.muted)
                    .textCase(.uppercase)
                Picker("Org Role", selection: $pendingRole) {
                    ForEach(orgRoles, id: \.self) { role in
                        Text(role).tag(role)
                    }
                }
                .pickerStyle(.segmented)

                Button("Update Role", systemImage: "checkmark.shield", action: onSaveRole)
                    .buttonStyle(.bordered)
            }

            VStack(alignment: .leading, spacing: 10) {
                Text("Team Access")
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(InciTeamTheme.muted)
                    .textCase(.uppercase)

                if user.teamMemberships.isEmpty {
                    Text("No team access yet.")
                        .font(.subheadline)
                        .foregroundStyle(InciTeamTheme.muted)
                } else {
                    ForEach(user.teamMemberships) { membership in
                        TeamMembershipRow(
                            membership: membership,
                            roles: teamRoles,
                            role: pendingTeamRole(membership),
                            onSave: { onSaveTeamRole(membership) },
                            onRemove: { onRemoveTeam(membership) }
                        )
                    }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Assign Team")
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(InciTeamTheme.muted)
                    .textCase(.uppercase)

                Picker("Team", selection: $pendingTeamSelection) {
                    Text("Select team").tag(Int64?.none)
                    ForEach(assignableTeams) { team in
                        Text(team.teamName).tag(Optional(team.id))
                    }
                }
                .disabled(assignableTeams.isEmpty)

                Button("Assign Team", systemImage: "plus", action: onAssignTeam)
                    .buttonStyle(.borderedProminent)
                    .disabled(pendingTeamSelection == nil)
            }

            Button("Delete Account", systemImage: "trash", role: .destructive, action: onDeleteAccount)
                .buttonStyle(.bordered)
                .tint(.red)
        }
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var assignableTeams: [TeamSummary] {
        let assignedIds = Set(user.teamMemberships.map(\.teamId))
        return teams.filter { !assignedIds.contains($0.id) }
    }
}

private struct TeamMembershipRow: View {
    let membership: UserTeamMembershipSummary
    let roles: [String]
    @Binding var role: String
    let onSave: () -> Void
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(membership.teamName)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    if membership.current {
                        Text("Current team")
                            .font(.caption)
                            .foregroundStyle(InciTeamTheme.primary)
                    }
                }
                Spacer()
            }

            Picker("Team Role", selection: $role) {
                ForEach(roles, id: \.self) { role in
                    Text(role).tag(role)
                }
            }

            HStack {
                Button("Save Team Role", systemImage: "checkmark", action: onSave)
                    .buttonStyle(.bordered)
                Button("Remove", systemImage: "trash", role: .destructive, action: onRemove)
                    .buttonStyle(.bordered)
            }
        }
        .padding(12)
        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct AccessMetricCard: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.heavy))
                .foregroundStyle(InciTeamTheme.muted)
            Text(value)
                .font(.title2.weight(.heavy))
                .foregroundStyle(color)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct AccessNoticeCard: View {
    let message: String
    let color: Color

    var body: some View {
        Text(message)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(InciTeamTheme.primaryDeep)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct AccessErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load access", systemImage: "exclamationmark.triangle.fill")
                .font(.headline.weight(.bold))
                .foregroundStyle(.orange)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
            Button("Try Again", action: retry)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

#Preview {
    NavigationStack {
        UserAccessView()
    }
    .environment(SessionStore.previewSignedIn)
}
