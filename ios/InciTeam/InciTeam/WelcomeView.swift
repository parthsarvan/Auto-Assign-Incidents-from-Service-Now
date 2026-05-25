import SwiftUI
import PhotosUI
import UIKit

struct WelcomeView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var selectedPhotoItem: PhotosPickerItem?

    private let sections = InciTeamFeatureSection.all

    var body: some View {
        NavigationStack {
            ZStack {
                InciTeamTheme.background
                    .ignoresSafeArea()

                ScrollView {
                    LazyVStack(spacing: 16) {
                        header

                        ForEach(sections) { section in
                            FeatureSectionCard(section: section)
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.top, 16)
                    .padding(.bottom, 34)
                }
                .scrollIndicators(.hidden)
            }
            .navigationTitle("InciTeam")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        sessionStore.signOut()
                    } label: {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }
                    .tint(.red)
                }
            }
            .navigationDestination(for: InciTeamFeature.self) { feature in
                FeatureDestinationView(feature: feature)
            }
            .task {
                await sessionStore.refreshProfileDetails()
            }
            .onChange(of: selectedPhotoItem) { _, newItem in
                guard let newItem else {
                    return
                }
                Task {
                    await saveSelectedProfileImage(from: newItem)
                }
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .center, spacing: 18) {
                profilePhotoPicker

                VStack(alignment: .leading, spacing: 8) {
                    Text("Welcome, \(sessionStore.displayName)")
                        .font(.title.weight(.bold))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .minimumScaleFactor(0.82)

                    Text(workspaceLine)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.white.opacity(0.78))
                        .lineLimit(2)
                }

                Spacer(minLength: 0)
            }

            HStack(spacing: 10) {
                InfoPill(title: sessionStore.currentUser?.role ?? "User", systemImage: "person.fill")

                if let teamRole = sessionStore.currentUser?.workspace?.teamRole, !teamRole.isEmpty {
                    InfoPill(title: teamRole, systemImage: "shield.fill")
                }

                if let teamTimezone = sessionStore.currentUser?.workspace?.teamTimezone, !teamTimezone.isEmpty {
                    InfoPill(title: teamTimezone, systemImage: "clock.fill")
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .background(InciTeamTheme.headerGradient, in: RoundedRectangle(cornerRadius: 30, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .stroke(Color.white.opacity(0.22), lineWidth: 1)
        }
        .shadow(color: InciTeamTheme.primary.opacity(0.28), radius: 26, x: 0, y: 18)
    }

    private var profilePhotoPicker: some View {
        let imageData = sessionStore.profileImageData
        let initials = profileInitials

        return PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
            ProfileAvatarView(
                imageData: imageData,
                initials: initials
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Update profile photo")
    }

    private var profileInitials: String {
        let source = sessionStore.displayName
        let parts = source
            .split(separator: " ")
            .map(String.init)
        let initials = parts
            .prefix(2)
            .compactMap(\.first)
            .map(String.init)
            .joined()
        return initials.isEmpty ? "IT" : initials.uppercased()
    }

    private var workspaceLine: String {
        let workspace = sessionStore.currentUser?.workspace
        let organizationName = workspace?.organizationName ?? "Your organization"
        let teamName = workspace?.teamName ?? "Current team"
        return "\(organizationName) / \(teamName)"
    }

    private func saveSelectedProfileImage(from item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self) else {
            return
        }

        try? sessionStore.saveProfileImageData(data)
        selectedPhotoItem = nil
    }
}

private struct ProfileAvatarView: View {
    let imageData: Data?
    let initials: String

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            avatar
                .frame(width: 112, height: 112)
                .clipShape(RoundedRectangle(cornerRadius: 34, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 34, style: .continuous)
                        .stroke(Color.white.opacity(0.34), lineWidth: 1.4)
                }
                .shadow(color: .black.opacity(0.20), radius: 16, x: 0, y: 10)

            Image(systemName: "camera.fill")
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.primaryDeep)
                .frame(width: 38, height: 38)
                .background(.white, in: Circle())
                .shadow(color: .black.opacity(0.18), radius: 7, x: 0, y: 3)
                .offset(x: 7, y: 7)
        }
    }

    @ViewBuilder
    private var avatar: some View {
        if let imageData, let uiImage = UIImage(data: imageData) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
        } else {
            ZStack {
                Color.white.opacity(0.18)
                Text(initials)
                    .font(.largeTitle.weight(.heavy))
                    .foregroundStyle(.white)
            }
        }
    }
}

private struct FeatureSectionCard: View {
    let section: InciTeamFeatureSection

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Label(section.title, systemImage: section.systemImage)
                .font(.headline.weight(.heavy))
                .foregroundStyle(InciTeamTheme.primaryDeep)

            VStack(spacing: 10) {
                ForEach(section.features) { feature in
                    NavigationLink(value: feature) {
                        FeatureRow(feature: feature)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color.white.opacity(0.86), lineWidth: 1)
        }
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }
}

private struct FeatureRow: View {
    let feature: InciTeamFeature

    var body: some View {
        HStack {
            Image(systemName: feature.systemImage)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 38, height: 38)
                .background(feature.tone.color.gradient, in: RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(feature.title)
                    .font(.body.weight(.bold))
                    .foregroundStyle(InciTeamTheme.ink)

                Text(feature.subtitle)
                    .font(.caption)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer(minLength: 12)

            Image(systemName: "chevron.right")
                .font(.caption.weight(.heavy))
                .foregroundStyle(InciTeamTheme.muted)
        }
        .padding(12)
        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(InciTeamTheme.border.opacity(0.72), lineWidth: 1)
        }
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.04), radius: 8, x: 0, y: 4)
        .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct InfoPill: View {
    let title: String
    let systemImage: String

    var body: some View {
        Label(title, systemImage: systemImage)
            .font(.caption.weight(.bold))
            .foregroundStyle(.white)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(Color.white.opacity(0.18), in: Capsule())
            .overlay {
                Capsule()
                    .stroke(Color.white.opacity(0.20), lineWidth: 1)
            }
    }
}

#Preview {
    WelcomeView()
        .environment(SessionStore.previewSignedIn)
}
