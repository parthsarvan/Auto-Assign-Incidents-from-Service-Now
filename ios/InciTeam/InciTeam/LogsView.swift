import SwiftUI

struct LogsView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var logs: [ServiceNowRunLogSummary] = []
    @State private var isLoading = false
    @State private var isPolling = false
    @State private var errorMessage = ""
    @State private var pollMessage = ""
    @State private var searchText = ""
    @State private var statusFilter: LogStatusFilter = .all
    @State private var resultFilter: AssignmentResultFilter = .all

    private let apiClient = InciTeamAPIClient()

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header
                    metrics
                    filters

                    if !pollMessage.isEmpty {
                        LogsNoticeCard(message: pollMessage, color: .green)
                    }

                    if isLoading {
                        ProgressView("Loading logs...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        LogsErrorCard(message: errorMessage) {
                            Task { await loadLogs() }
                        }
                    } else if filteredLogs.isEmpty {
                        LogsNoticeCard(message: logs.isEmpty ? "No ServiceNow logs are available yet." : "No logs match the current filters.", color: InciTeamTheme.primary)
                    } else {
                        ForEach(filteredLogs) { log in
                            LogEntryCard(log: log)
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("Logs")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task { await loadLogs() }
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .disabled(isLoading || isPolling)
            }

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
                    .disabled(isLoading || isPolling)
                }
            }
        }
        .task {
            await loadLogs()
        }
        .refreshable {
            await loadLogs()
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "list.bullet.rectangle.portrait.fill")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(InciTeamTheme.primary.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("ServiceNow Logs")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("Poll history and assignment outcomes for \(teamName).")
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
            LogMetricCard(title: "Entries", value: "\(logs.count)", color: InciTeamTheme.primary)
            LogMetricCard(title: "Healthy", value: "\(logs.filter { $0.status == "OK" }.count)", color: .green)
            LogMetricCard(title: "Errors", value: "\(logs.filter { $0.status == "ERROR" }.count)", color: .red)
            LogMetricCard(title: "Results", value: "\(logs.flatMap(\.assignmentResults).count)", color: .orange)
        }
    }

    private var filters: some View {
        VStack(spacing: 12) {
            TextField("Search incident, CI, caller, message...", text: $searchText)
                .textInputAutocapitalization(.never)
                .padding(12)
                .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 14, style: .continuous))

            Picker("Status", selection: $statusFilter) {
                ForEach(LogStatusFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)

            Picker("Result", selection: $resultFilter) {
                ForEach(AssignmentResultFilter.allCases) { filter in
                    Text(filter.title).tag(filter)
                }
            }
            .pickerStyle(.segmented)
        }
        .padding(14)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var canManageTeam: Bool {
        sessionStore.currentUser?.canManageCurrentTeam ?? false
    }

    private var teamName: String {
        sessionStore.currentUser?.workspace?.teamName ?? "current team"
    }

    private var filteredLogs: [ServiceNowRunLogSummary] {
        logs.filter { log in
            if statusFilter != .all && log.status != statusFilter.rawValue {
                return false
            }

            if resultFilter != .all && !log.assignmentResults.contains(where: { $0.status == resultFilter.rawValue }) {
                return false
            }

            let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            guard !query.isEmpty else {
                return true
            }

            let values = [
                log.type,
                log.status,
                log.message,
                log.assignmentConfirmation
            ]
            + log.incidents.flatMap { incident in
                [incident.number, incident.configurationItem, incident.assignmentGroup, incident.caller, incident.shortDescription]
            }
            + log.assignmentResults.flatMap { result in
                [result.incidentNumber, result.assigneeName, result.status, result.message]
            }

            return values
                .compactMap { $0?.lowercased() }
                .contains { $0.contains(query) }
        }
    }

    private func loadLogs() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            logs = try await apiClient.fetchServiceNowLogs(token: token)
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
            await loadLogs()
        } catch {
            errorMessage = error.localizedDescription
        }
        isPolling = false
    }
}

private enum LogStatusFilter: String, CaseIterable, Identifiable {
    case all = "ALL"
    case ok = "OK"
    case error = "ERROR"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: "All"
        case .ok: "OK"
        case .error: "Error"
        }
    }
}

private enum AssignmentResultFilter: String, CaseIterable, Identifiable {
    case all = "ALL"
    case success = "SUCCESS"
    case failed = "FAILED"
    case skipped = "SKIPPED"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: "All"
        case .success: "Success"
        case .failed: "Failed"
        case .skipped: "Skipped"
        }
    }
}

private struct LogEntryCard: View {
    let log: ServiceNowRunLogSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(log.type ?? "Log")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text(DateFormatting.displayDateTime(log.timestamp))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(InciTeamTheme.muted)
                }

                Spacer()

                StatusCapsule(title: log.status ?? "-", color: log.status == "OK" ? .green : .red)
            }

            if let message = log.message, !message.isEmpty {
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            HStack(spacing: 8) {
                StatusCapsule(title: "\(log.incidentCount) incidents", color: InciTeamTheme.primary)
                StatusCapsule(title: "\(log.assignmentResults.count) results", color: .orange)
            }

            if !log.assignmentSelections.isEmpty {
                LogSection(title: "Selections") {
                    ForEach(log.assignmentSelections) { selection in
                        Text("\(selection.incidentNumber ?? "-") → \(selection.assigneeName ?? "-")")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(InciTeamTheme.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }

            if !log.assignmentResults.isEmpty {
                LogSection(title: "Assignment Results") {
                    ForEach(log.assignmentResults) { result in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(result.incidentNumber ?? "-")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(InciTeamTheme.ink)
                                Spacer()
                                StatusCapsule(title: result.status ?? "-", color: result.status == "SUCCESS" ? .green : result.status == "FAILED" ? .red : .orange)
                            }
                            Text(result.message ?? "-")
                                .font(.caption)
                                .foregroundStyle(InciTeamTheme.muted)
                        }
                    }
                }
            }

            if !log.incidents.isEmpty {
                LogSection(title: "Incidents") {
                    ForEach(sortedIncidents) { incident in
                        VStack(alignment: .leading, spacing: 3) {
                            Text("\(incident.number ?? "-") • \(incident.priority ?? "-")")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(InciTeamTheme.ink)
                            Text(incident.shortDescription ?? incident.configurationItem ?? "-")
                                .font(.caption)
                                .foregroundStyle(InciTeamTheme.muted)
                                .lineLimit(2)
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var sortedIncidents: [ServiceNowIncidentSummary] {
        log.incidents.sorted { left, right in
            let leftDate = DateFormatting.instantDate(from: left.createdOn) ?? .distantFuture
            let rightDate = DateFormatting.instantDate(from: right.createdOn) ?? .distantFuture
            if leftDate != rightDate {
                return leftDate < rightDate
            }
            return (left.number ?? "") < (right.number ?? "")
        }
    }
}

private struct LogSection<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption.weight(.heavy))
                .foregroundStyle(InciTeamTheme.muted)
                .textCase(.uppercase)
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct LogMetricCard: View {
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

private struct StatusCapsule: View {
    let title: String
    let color: Color

    var body: some View {
        Text(title)
            .font(.caption.weight(.bold))
            .foregroundStyle(color)
            .padding(.horizontal, 9)
            .padding(.vertical, 5)
            .background(color.opacity(0.12), in: Capsule())
    }
}

private struct LogsNoticeCard: View {
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

private struct LogsErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load logs", systemImage: "exclamationmark.triangle.fill")
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
        LogsView()
    }
    .environment(SessionStore.previewSignedIn)
}
