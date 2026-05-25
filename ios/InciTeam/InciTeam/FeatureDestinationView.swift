import SwiftUI

struct FeatureDestinationView: View {
    let feature: InciTeamFeature

    var body: some View {
        switch feature {
        case .roster:
            RosterView()
        case .schedule:
            ScheduleView()
        case .teamMembers:
            TeamMembersView()
        case .leaves:
            AvailabilityEntriesView(kind: .leave)
        case .breaks:
            AvailabilityEntriesView(kind: .breakPeriod)
        case .configurationItems:
            ConfigurationItemsView()
        case .ciUserMapping:
            CiUserMappingView()
        case .summary:
            SummaryView()
        case .logs:
            LogsView()
        case .diagnostics:
            DiagnosticsView()
        case .accountSettings:
            AccountSettingsView()
        case .userAccess:
            UserAccessView()
        }
    }

    private var placeholder: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            VStack(spacing: 18) {
                Image(systemName: feature.systemImage)
                    .font(.system(size: 52, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 86, height: 86)
                    .background(feature.tone.color.gradient, in: RoundedRectangle(cornerRadius: 24, style: .continuous))

                VStack(spacing: 8) {
                    Text(feature.title)
                        .font(.largeTitle.weight(.bold))
                        .foregroundStyle(InciTeamTheme.primaryDeep)
                        .multilineTextAlignment(.center)

                    Text("Data view pending")
                        .font(.headline)
                        .foregroundStyle(InciTeamTheme.muted)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(28)
            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .stroke(Color.white.opacity(0.86), lineWidth: 1)
            }
            .shadow(color: feature.tone.color.opacity(0.14), radius: 24, x: 0, y: 16)
            .padding(24)
        }
        .navigationTitle(feature.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        FeatureDestinationView(feature: .summary)
    }
}
