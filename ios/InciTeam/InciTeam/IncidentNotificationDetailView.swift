import SwiftUI

struct IncidentNotificationDetailView: View {
    let detail: IncidentNotificationDetail
    let onClose: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                InciTeamTheme.background
                    .ignoresSafeArea()

                VStack(alignment: .leading, spacing: 18) {
                    HStack(spacing: 14) {
                        Image(systemName: "bell.badge.fill")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(.white)
                            .frame(width: 54, height: 54)
                            .background(InciTeamTheme.primary.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Incident Assigned")
                                .font(.title2.weight(.bold))
                                .foregroundStyle(InciTeamTheme.primaryDeep)
                            Text(detail.incidentNumber)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(InciTeamTheme.primary)
                        }

                        Spacer()
                    }

                    VStack(spacing: 12) {
                        IncidentNotificationRow(
                            title: "Incident",
                            value: detail.incidentNumber,
                            systemImage: "number"
                        )

                        IncidentNotificationRow(
                            title: "Title",
                            value: detail.title,
                            systemImage: "text.alignleft"
                        )

                        IncidentNotificationRow(
                            title: "Priority",
                            value: detail.priority,
                            systemImage: "flag.fill"
                        )
                    }
                }
                .padding(18)
                .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 26, style: .continuous))
                .shadow(color: InciTeamTheme.primaryDeep.opacity(0.10), radius: 24, x: 0, y: 14)
                .padding(18)
            }
            .navigationTitle("Notification")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done", action: onClose)
                }
            }
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }
}

private struct IncidentNotificationRow: View {
    let title: String
    let value: String
    let systemImage: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: systemImage)
                .font(.headline.weight(.semibold))
                .foregroundStyle(InciTeamTheme.primary)
                .frame(width: 34, height: 34)
                .background(InciTeamTheme.primary.opacity(0.12), in: RoundedRectangle(cornerRadius: 11, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(InciTeamTheme.muted)
                    .textCase(.uppercase)
                Text(value.isEmpty ? "-" : value)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(InciTeamTheme.ink)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer()
        }
        .padding(12)
        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

#Preview {
    IncidentNotificationDetailView(
        detail: IncidentNotificationDetail(
            userInfo: [
                "incidentNumber": "INC0010024",
                "title": "Unable to access account",
                "priority": "2 - High",
                "ci": "Hidden CI"
            ]
        )!,
        onClose: {}
    )
}
