import SwiftUI

struct SummaryView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var health: ServiceNowHealthResponse?
    @State private var validation: ServiceNowValidationResponse?
    @State private var logs: [ServiceNowRunLogSummary] = []
    @State private var coverage: CoverageSummaryResponse?
    @State private var handoff: LeaveHandoffResponse?
    @State private var pollMessage = ""
    @State private var isLoading = false
    @State private var isPolling = false
    @State private var errorMessage = ""

    private let apiClient = InciTeamAPIClient()

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header

                    if !canManageTeam {
                        managerOnlyCard
                    } else if isLoading {
                        ProgressView("Loading summary...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else {
                        if !errorMessage.isEmpty {
                            SummaryNoticeCard(title: "Could not load summary", message: errorMessage, color: .orange)
                        }
                        if !pollMessage.isEmpty {
                            SummaryNoticeCard(title: "Poll completed", message: pollMessage, color: .green)
                        }

                        metricGrid
                        serviceNowCard
                        coverageCard
                        handoffCard
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("Summary")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canManageTeam {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await pollNow() }
                    } label: {
                        if isPolling {
                            ProgressView()
                        } else {
                            Image(systemName: "arrow.triangle.2.circlepath")
                        }
                    }
                    .disabled(isPolling)
                }
            }
        }
        .task {
            await loadData()
        }
        .refreshable {
            await loadData()
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "gauge.with.dots.needle.bottom.50percent")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(InciTeamTheme.primary.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("Operations Summary")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("\(teamName) health, coverage, and routing risk.")
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer()
        }
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var metricGrid: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 12)], spacing: 12) {
            SummaryMetricCard(
                title: "Latest Poll",
                value: latestPollStatus,
                footnote: latestPoll?.message ?? "No poll activity yet.",
                color: latestPoll?.status == "ERROR" ? .red : .green
            )
            SummaryMetricCard(
                title: "Assignments",
                value: "\(latestSuccessCount)/\(latestFailedCount)/\(latestSkippedCount)",
                footnote: "Success / Failed / Skipped",
                color: latestFailedCount > 0 ? .red : .blue
            )
            SummaryMetricCard(
                title: "Validation",
                value: validation?.valid == true ? "Clear" : "\(validation?.issues.count ?? 0) Issues",
                footnote: validation?.message ?? "Not checked yet.",
                color: validation?.valid == true ? .green : .orange
            )
            SummaryMetricCard(
                title: "Coverage",
                value: "\(coverage?.gapCount ?? 0) Gaps",
                footnote: "\(coverage?.ciRiskCount ?? 0) CI risks",
                color: (coverage?.gapCount ?? 0) > 0 || (coverage?.ciRiskCount ?? 0) > 0 ? .orange : .green
            )
        }
    }

    private var serviceNowCard: some View {
        SummaryDetailCard(
            title: "ServiceNow Health",
            subtitle: health?.healthy == true ? "Connected" : "Needs attention",
            systemImage: "cloud.fill",
            color: health?.healthy == true ? .green : .red
        ) {
            VStack(alignment: .leading, spacing: 8) {
                SummaryLine(label: "Status", value: health?.status ?? "-")
                SummaryLine(label: "Message", value: health?.message ?? "-")
                SummaryLine(label: "Instance", value: health?.instanceUrl ?? "-")
                SummaryLine(label: "Last poll", value: DateFormatting.shortDateTime(health?.lastPollAt))
            }
        }
    }

    private var coverageCard: some View {
        SummaryDetailCard(
            title: "Coverage Outlook",
            subtitle: "Next 7 days",
            systemImage: "calendar.badge.exclamationmark",
            color: (coverage?.gapCount ?? 0) > 0 || (coverage?.ciRiskCount ?? 0) > 0 ? .orange : .green
        ) {
            VStack(alignment: .leading, spacing: 10) {
                SummaryLine(label: "Window", value: "\(coverage?.startDate ?? "-") to \(coverage?.endDate ?? "-")")
                SummaryLine(label: "Covered", value: "\(coverage?.coveredGeoShiftDays ?? 0) of \(coverage?.totalGeoShiftDays ?? 0) geo/shift days")

                ForEach((coverage?.issues ?? []).prefix(4)) { issue in
                    Text(issue.message ?? "Coverage issue")
                        .font(.caption)
                        .foregroundStyle(InciTeamTheme.muted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
        }
    }

    private var handoffCard: some View {
        SummaryDetailCard(
            title: "Leave Handoff",
            subtitle: "\(handoff?.activeIncidentCount ?? 0) active incidents",
            systemImage: "person.crop.circle.badge.exclamationmark",
            color: (handoff?.activeIncidentCount ?? 0) > 0 ? .orange : .green
        ) {
            VStack(alignment: .leading, spacing: 10) {
                SummaryLine(label: "Impacted members", value: "\(handoff?.impactedMemberCount ?? 0)")
                SummaryLine(label: "Checked", value: DateFormatting.shortDateTime(handoff?.checkedAt))

                ForEach((handoff?.items ?? []).prefix(3)) { item in
                    Text("\(item.teamMemberName ?? item.email ?? "Team member"): \(item.incidents.count) active")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(InciTeamTheme.muted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
        }
    }

    private var managerOnlyCard: some View {
        SummaryNoticeCard(
            title: "Manager access required",
            message: "Summary is available to team managers and admins because it includes poll controls and setup diagnostics.",
            color: InciTeamTheme.primary
        )
    }

    private var canManageTeam: Bool {
        sessionStore.currentUser?.canManageCurrentTeam ?? false
    }

    private var teamName: String {
        sessionStore.currentUser?.workspace?.teamName ?? "current team"
    }

    private var latestPoll: ServiceNowRunLogSummary? {
        logs.first { $0.type == "POLL" }
    }

    private var latestPollStatus: String {
        guard let latestPoll else {
            return "Waiting"
        }
        return latestPoll.status == "ERROR" ? "Issue" : "Healthy"
    }

    private var latestSuccessCount: Int {
        latestPoll?.assignmentResults.filter { $0.status == "SUCCESS" }.count ?? 0
    }

    private var latestFailedCount: Int {
        latestPoll?.assignmentResults.filter { $0.status == "FAILED" }.count ?? 0
    }

    private var latestSkippedCount: Int {
        latestPoll?.assignmentResults.filter { $0.status == "SKIPPED" }.count ?? 0
    }

    private func loadData() async {
        guard canManageTeam, let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            async let healthData = apiClient.fetchServiceNowHealth(token: token)
            async let validationData = apiClient.fetchServiceNowValidation(token: token)
            async let logData = apiClient.fetchServiceNowLogs(token: token)
            async let coverageData = apiClient.fetchCoverageSummary(token: token, days: 7)
            async let handoffData = apiClient.fetchLeaveHandoff(token: token)

            health = try await healthData
            validation = try await validationData
            logs = try await logData
            coverage = try await coverageData
            handoff = try await handoffData
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func pollNow() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isPolling = true
        pollMessage = ""
        errorMessage = ""
        do {
            let result = try await apiClient.pollServiceNowNow(token: token)
            pollMessage = result.message ?? "Poll completed."
            await loadData()
        } catch {
            errorMessage = error.localizedDescription
        }
        isPolling = false
    }
}

private struct SummaryMetricCard: View {
    let title: String
    let value: String
    let footnote: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption.weight(.heavy))
                .foregroundStyle(InciTeamTheme.muted)
                .textCase(.uppercase)
            Text(value)
                .font(.title3.weight(.heavy))
                .foregroundStyle(color)
            Text(footnote)
                .font(.caption)
                .foregroundStyle(InciTeamTheme.muted)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.07), radius: 14, x: 0, y: 8)
    }
}

private struct SummaryDetailCard<Content: View>: View {
    let title: String
    let subtitle: String
    let systemImage: String
    let color: Color
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .foregroundStyle(.white)
                    .frame(width: 42, height: 42)
                    .background(color.gradient, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text(subtitle)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(color)
                }
                Spacer()
            }
            content
        }
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }
}

private struct SummaryLine: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top) {
            Text(label)
                .font(.caption.weight(.bold))
                .foregroundStyle(InciTeamTheme.muted)
            Spacer()
            Text(value)
                .font(.caption.weight(.semibold))
                .foregroundStyle(InciTeamTheme.ink)
                .multilineTextAlignment(.trailing)
        }
    }
}

private struct SummaryNoticeCard: View {
    let title: String
    let message: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundStyle(color)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(color.opacity(0.10), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

#Preview {
    NavigationStack {
        SummaryView()
    }
    .environment(SessionStore.previewSignedIn)
}
