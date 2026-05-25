import SwiftUI

struct DiagnosticsView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var diagnostics: AssignmentDiagnosticsResponse?
    @State private var isLoading = false
    @State private var errorMessage = ""

    private let apiClient = InciTeamAPIClient()

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header

                    if isLoading {
                        ProgressView("Running dry run...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        DiagnosticsErrorCard(message: errorMessage) {
                            Task { await loadDiagnostics() }
                        }
                    } else if let diagnostics {
                        metricGrid(diagnostics)

                        if diagnostics.incidents.isEmpty {
                            DiagnosticsNoticeCard(message: "No unassigned incidents are currently available for diagnostics.", color: .green)
                        } else {
                            ForEach(diagnostics.incidents) { incident in
                                DiagnosticIncidentCard(incident: incident)
                            }
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("Diagnostics")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task { await loadDiagnostics() }
                } label: {
                    Image(systemName: "play.circle.fill")
                }
                .disabled(isLoading)
            }
        }
        .task {
            await loadDiagnostics()
        }
        .refreshable {
            await loadDiagnostics()
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "stethoscope")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(FeatureTone.purple.color.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("Assignment Diagnostics")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("Dry-run routing checks for \(teamName).")
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer()
        }
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var teamName: String {
        sessionStore.currentUser?.workspace?.teamName ?? "current team"
    }

    private func metricGrid(_ diagnostics: AssignmentDiagnosticsResponse) -> some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 12)], spacing: 12) {
            DiagnosticMetricCard(title: "Incidents", value: "\(diagnostics.incidentCount)", color: InciTeamTheme.primary)
            DiagnosticMetricCard(title: "Assignable", value: "\(diagnostics.assignableCount)", color: .green)
            DiagnosticMetricCard(title: "Skipped", value: "\(diagnostics.skippedCount)", color: .orange)
        }
    }

    private func loadDiagnostics() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            diagnostics = try await apiClient.fetchAssignmentDiagnostics(token: token)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}

private struct DiagnosticIncidentCard: View {
    let incident: AssignmentDiagnosticItem

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(incident.incidentNumber ?? "Unknown Incident")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text("CI: \(incident.configurationItem ?? "-") • Priority: \(incident.priority ?? "-")")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(InciTeamTheme.muted)
                }

                Spacer()

                DiagnosticStatusCapsule(status: incident.status)
            }

            if let description = incident.shortDescription, !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            if let reason = incident.reason, !reason.isEmpty {
                Label(reason, systemImage: "info.circle.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(InciTeamTheme.primary.opacity(0.10), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            if let suggestion = incident.suggestion {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Suggested Assignee")
                        .font(.caption.weight(.heavy))
                        .foregroundStyle(InciTeamTheme.muted)
                        .textCase(.uppercase)
                    DiagnosticLine(label: "Name", value: suggestion.assigneeName ?? "-")
                    DiagnosticLine(label: "Email", value: suggestion.assigneeEmail ?? "-")
                    DiagnosticLine(label: "Geo / Shift", value: "\(suggestion.geo ?? "-") / \(suggestion.shift ?? "-")")
                }
                .padding(12)
                .background(Color.green.opacity(0.10), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }

            if !incident.candidateChecks.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Candidate Review")
                        .font(.caption.weight(.heavy))
                        .foregroundStyle(InciTeamTheme.muted)
                        .textCase(.uppercase)

                    ForEach(incident.candidateChecks) { candidate in
                        CandidateCheckRow(candidate: candidate)
                    }
                }
            }
        }
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }
}

private struct CandidateCheckRow: View {
    let candidate: AssignmentCandidateCheck

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(candidate.teamMemberName ?? "Unknown member")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(InciTeamTheme.ink)
                Spacer()
                if candidate.selected {
                    DiagnosticSmallCapsule(title: "Selected", color: .green)
                } else if candidate.eligible {
                    DiagnosticSmallCapsule(title: "Eligible", color: InciTeamTheme.primary)
                } else {
                    DiagnosticSmallCapsule(title: "Filtered", color: .gray)
                }
            }

            Text(candidate.reason ?? candidate.matchStatus ?? "-")
                .font(.caption)
                .foregroundStyle(InciTeamTheme.muted)

            HStack(spacing: 8) {
                if candidate.onLeave {
                    DiagnosticSmallCapsule(title: "Leave", color: .orange)
                }
                if candidate.onBreak {
                    DiagnosticSmallCapsule(title: "Break", color: .blue)
                }
                if let schedules = candidate.activeSchedules, !schedules.isEmpty {
                    DiagnosticSmallCapsule(title: schedules, color: InciTeamTheme.primary)
                }
            }
        }
        .padding(10)
        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct DiagnosticMetricCard: View {
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

private struct DiagnosticLine: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.caption.weight(.bold))
                .foregroundStyle(InciTeamTheme.muted)
            Spacer()
            Text(value)
                .font(.caption.weight(.semibold))
                .foregroundStyle(InciTeamTheme.ink)
        }
    }
}

private struct DiagnosticStatusCapsule: View {
    let status: String?

    var body: some View {
        let isAssignable = status == "ASSIGNABLE"
        Text(status ?? "-")
            .font(.caption.weight(.bold))
            .foregroundStyle(isAssignable ? .green : .orange)
            .padding(.horizontal, 9)
            .padding(.vertical, 5)
            .background((isAssignable ? Color.green : Color.orange).opacity(0.12), in: Capsule())
    }
}

private struct DiagnosticSmallCapsule: View {
    let title: String
    let color: Color

    var body: some View {
        Text(title)
            .font(.caption2.weight(.bold))
            .foregroundStyle(color)
            .lineLimit(1)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.12), in: Capsule())
    }
}

private struct DiagnosticsNoticeCard: View {
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

private struct DiagnosticsErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not run diagnostics", systemImage: "exclamationmark.triangle.fill")
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
        DiagnosticsView()
    }
    .environment(SessionStore.previewSignedIn)
}
