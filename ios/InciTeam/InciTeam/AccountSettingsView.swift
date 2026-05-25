import SwiftUI

struct AccountSettingsView: View {
    @Environment(SessionStore.self) private var sessionStore
    @Environment(\.openURL) private var openURL
    @State private var confirmation = ""
    @State private var isDeleting = false
    @State private var errorMessage = ""
    @State private var isShowingDeleteConfirmation = false

    private let apiClient = InciTeamAPIClient()
    private let privacyURL = URL(string: "https://www.inciteam.com/privacy")

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header
                    accountDetails
                    privacyCard
                    dangerZone
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("Account")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Delete your account?", isPresented: $isShowingDeleteConfirmation) {
            Button("Delete Account", role: .destructive) {
                Task { await deleteAccount() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This permanently removes your InciTeam login, team access, push notification tokens, and roster records linked to your work email. This cannot be undone.")
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "person.crop.circle.badge.exclamationmark")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(FeatureTone.red.color.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("Account Settings")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("Review account details and manage deletion.")
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer()
        }
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var accountDetails: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Signed In As")
                .font(.caption.weight(.heavy))
                .foregroundStyle(InciTeamTheme.muted)
                .textCase(.uppercase)

            Text(sessionStore.currentUser?.username ?? "Current user")
                .font(.title2.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)

            VStack(spacing: 10) {
                AccountDetailRow(title: "Work Email", value: sessionStore.currentUser?.workEmail ?? "Not available")
                AccountDetailRow(title: "Organization Role", value: sessionStore.currentUser?.role ?? "User")
                AccountDetailRow(title: "Current Team", value: sessionStore.currentUser?.workspace?.teamName ?? "None")
                AccountDetailRow(title: "Team Role", value: sessionStore.currentUser?.workspace?.teamRole ?? "Member")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var privacyCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Privacy", systemImage: "hand.raised.fill")
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)

            Text("Review how InciTeam handles account data, ServiceNow data, AWS-hosted backend services, and push notifications.")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)

            Button("Open Privacy Policy", systemImage: "safari") {
                if let privacyURL {
                    openURL(privacyURL)
                }
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var dangerZone: some View {
        VStack(alignment: .leading, spacing: 14) {
            Label("Delete Account", systemImage: "exclamationmark.triangle.fill")
                .font(.headline.weight(.bold))
                .foregroundStyle(.red)

            Text("This removes your InciTeam login, team access, push notification tokens, and roster records linked to your work email, including routing mappings, schedules, leaves, and breaks.")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)

            Text("If your account is the last organization Admin or last TEAM_ADMIN for a team, assign another admin first so the workspace remains manageable.")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)

            if !errorMessage.isEmpty {
                Text(errorMessage)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Color.red.opacity(0.10), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Type DELETE to confirm")
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(InciTeamTheme.muted)
                    .textCase(.uppercase)

                TextField("DELETE", text: $confirmation)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .disabled(isDeleting)
                    .padding(12)
                    .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }

            Button {
                errorMessage = ""
                guard canSubmitDeletion else {
                    errorMessage = "Type DELETE to confirm account deletion."
                    return
                }
                isShowingDeleteConfirmation = true
            } label: {
                if isDeleting {
                    ProgressView()
                } else {
                    Label("Delete My Account", systemImage: "trash")
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(.red)
            .disabled(!canSubmitDeletion || isDeleting)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.red.opacity(0.08), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(Color.red.opacity(0.22), lineWidth: 1)
        }
    }

    private var canSubmitDeletion: Bool {
        confirmation.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() == "DELETE"
    }

    private func deleteAccount() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isDeleting = true
        errorMessage = ""
        do {
            _ = try await apiClient.deleteCurrentAccount(token: token)
            sessionStore.clearSessionAfterAccountDeletion()
        } catch {
            errorMessage = error.localizedDescription
            isDeleting = false
        }
    }
}

private struct AccountDetailRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack(alignment: .top) {
            Text(title)
                .font(.caption.weight(.heavy))
                .foregroundStyle(InciTeamTheme.muted)
                .textCase(.uppercase)
                .frame(width: 132, alignment: .leading)

            Text(value)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(InciTeamTheme.ink)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

#Preview {
    NavigationStack {
        AccountSettingsView()
    }
    .environment(SessionStore.previewSignedIn)
}
