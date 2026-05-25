import SwiftUI

struct RosterView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var viewMode: RosterViewMode = .week
    @State private var startDate = Date()
    @State private var availability: [AvailabilityRecord] = []
    @State private var leaves: [LeaveRecordSummary] = []
    @State private var breaks: [BreakRecordSummary] = []
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
                    controls

                    if isLoading {
                        ProgressView("Loading roster...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        ErrorCard(message: errorMessage) {
                            Task { await loadRoster() }
                        }
                    } else if groupedAvailability.isEmpty {
                        EmptyStateCard(
                            icon: "person.3.sequence.fill",
                            title: "No roster coverage",
                            message: "No on-shift availability exists for this date window."
                        )
                    } else {
                        ForEach(groupedAvailability) { group in
                            RosterGroupCard(
                                group: group,
                                dates: selectedDates,
                                leaveKeys: leaveKeys,
                                breakKeys: breakKeys
                            )
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("Roster")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadRoster()
        }
        .refreshable {
            await loadRoster()
        }
        .onChange(of: viewMode) { _, _ in
            startDate = Date()
            Task { await loadRoster() }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Roster", systemImage: "person.3.sequence.fill")
                .font(.title.weight(.bold))
                .foregroundStyle(InciTeamTheme.primaryDeep)

            Text("Schedule-aware availability for \(teamName).")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var controls: some View {
        VStack(spacing: 12) {
            Picker("Window", selection: $viewMode) {
                ForEach(RosterViewMode.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
            .pickerStyle(.segmented)

            HStack {
                Button {
                    moveWindow(by: -viewMode.dayCount)
                } label: {
                    Image(systemName: "chevron.left")
                        .frame(width: 38, height: 38)
                }
                .buttonStyle(.bordered)

                Spacer()

                VStack(spacing: 3) {
                    Text(windowTitle)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text("\(availability.count) scheduled records")
                        .font(.caption)
                        .foregroundStyle(InciTeamTheme.muted)
                }

                Spacer()

                Button {
                    moveWindow(by: viewMode.dayCount)
                } label: {
                    Image(systemName: "chevron.right")
                        .frame(width: 38, height: 38)
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(14)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private var selectedDates: [String] {
        DateFormatting.days(starting: startDate, count: viewMode.dayCount).map { DateFormatting.isoDate($0) }
    }

    private var groupedAvailability: [RosterGroup] {
        let grouped = Dictionary(grouping: availability) { record in
            "\(record.geoName) / \(record.shiftName)"
        }
        return grouped.keys.sorted().map { key in
            RosterGroup(title: key, records: grouped[key] ?? [])
        }
    }

    private var leaveKeys: Set<String> {
        buildImpactKeys(
            availabilityRecords: availability,
            impactRecords: leaves.map {
                RosterImpactRecord(
                    tmId: $0.tmId,
                    fullName: $0.fullName,
                    startTs: $0.startTs,
                    endTs: $0.endTs
                )
            }
        )
    }

    private var breakKeys: Set<String> {
        buildImpactKeys(
            availabilityRecords: availability,
            impactRecords: breaks.map {
                RosterImpactRecord(
                    tmId: $0.tmId,
                    fullName: $0.fullName,
                    startTs: $0.startTs,
                    endTs: $0.endTs
                )
            }
        )
    }

    private var teamName: String {
        sessionStore.currentUser?.workspace?.teamName ?? "current team"
    }

    private var windowTitle: String {
        guard viewMode == .week, let lastDate = selectedDates.last else {
            return DateFormatting.displayDate(DateFormatting.isoDate(startDate))
        }
        return "\(DateFormatting.displayDate(DateFormatting.isoDate(startDate))) - \(DateFormatting.displayDate(lastDate))"
    }

    private func moveWindow(by days: Int) {
        startDate = Calendar.current.date(byAdding: .day, value: days, to: startDate) ?? startDate
        Task { await loadRoster() }
    }

    private func loadRoster() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            async let availabilityData = apiClient.fetchAvailability(
                token: token,
                startDate: DateFormatting.isoDate(startDate),
                days: viewMode.dayCount
            )
            async let leaveData = apiClient.fetchLeaves(
                token: token,
                startDate: DateFormatting.isoDate(startDate),
                days: viewMode.dayCount
            )
            async let breakData = apiClient.fetchBreaks(
                token: token,
                startDate: DateFormatting.isoDate(startDate),
                days: viewMode.dayCount
            )
            availability = try await availabilityData
            leaves = try await leaveData
            breaks = try await breakData
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}

private enum RosterViewMode: String, CaseIterable, Identifiable {
    case day
    case week

    var id: String { rawValue }

    var title: String {
        switch self {
        case .day: "Day"
        case .week: "Week"
        }
    }

    var dayCount: Int {
        switch self {
        case .day: 1
        case .week: 7
        }
    }
}

private struct RosterGroup: Identifiable {
    let id = UUID()
    let title: String
    let records: [AvailabilityRecord]
}

private struct RosterImpactRecord {
    let tmId: Int64?
    let fullName: String
    let startTs: String
    let endTs: String
}

private struct RosterMemberChip: Hashable {
    let name: String
    let statusKey: String
}

private func rosterMemberKeys(tmId: Int64?, fullName: String) -> [String] {
    var keys: [String] = []
    if let tmId {
        keys.append("id:\(tmId)")
    }
    if !fullName.isEmpty {
        keys.append("name:\(fullName)")
    }
    return keys
}

private func rosterPrimaryMemberKey(tmId: Int64?, fullName: String) -> String {
    rosterMemberKeys(tmId: tmId, fullName: fullName).first ?? ""
}

private func rosterStatusKey(for record: AvailabilityRecord) -> String {
    "\(record.geoName) / \(record.shiftName)|\(record.date)|\(rosterPrimaryMemberKey(tmId: record.tmId, fullName: record.fullName))"
}

private func impactRecord(_ record: RosterImpactRecord, overlapsRosterDate date: String) -> Bool {
    guard let start = DateFormatting.instantDate(from: record.startTs),
          let end = DateFormatting.instantDate(from: record.endTs),
          let rosterDate = DateFormatting.date(from: date) else {
        return false
    }

    let calendar = Calendar.current
    let dayStart = calendar.startOfDay(for: rosterDate)
    guard let nextDayStart = calendar.date(byAdding: .day, value: 1, to: dayStart) else {
        return false
    }

    return start < nextDayStart && end >= dayStart
}

private func buildImpactKeys(
    availabilityRecords: [AvailabilityRecord],
    impactRecords: [RosterImpactRecord]
) -> Set<String> {
    var recordsByMember: [String: [RosterImpactRecord]] = [:]
    for record in impactRecords {
        for memberKey in rosterMemberKeys(tmId: record.tmId, fullName: record.fullName) {
            recordsByMember[memberKey, default: []].append(record)
        }
    }

    var impactKeys = Set<String>()
    for availabilityRecord in availabilityRecords {
        let relatedImpactRecords = rosterMemberKeys(
            tmId: availabilityRecord.tmId,
            fullName: availabilityRecord.fullName
        )
        .flatMap { recordsByMember[$0] ?? [] }

        if relatedImpactRecords.contains(where: { impactRecord($0, overlapsRosterDate: availabilityRecord.date) }) {
            impactKeys.insert(rosterStatusKey(for: availabilityRecord))
        }
    }

    return impactKeys
}

private struct RosterGroupCard: View {
    let group: RosterGroup
    let dates: [String]
    let leaveKeys: Set<String>
    let breakKeys: Set<String>

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(group.title)
                .font(.headline.weight(.heavy))
                .foregroundStyle(InciTeamTheme.primaryDeep)

            ForEach(dates, id: \.self) { date in
                VStack(alignment: .leading, spacing: 8) {
                    Text(DateFormatting.displayDate(date))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.muted)

                    let members = group.records
                        .filter { $0.date == date }
                        .map { RosterMemberChip(name: $0.fullName, statusKey: rosterStatusKey(for: $0)) }
                    if members.isEmpty {
                        Text("No coverage")
                            .font(.subheadline)
                            .foregroundStyle(InciTeamTheme.muted)
                    } else {
                        FlowLayout(items: members) { member in
                            StatusChip(name: member.name, status: status(for: member.statusKey))
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
        }
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private func status(for statusKey: String) -> RosterStatus {
        if leaveKeys.contains(statusKey) {
            return .leave
        }
        if breakKeys.contains(statusKey) {
            return .break
        }
        return .available
    }
}

private enum RosterStatus {
    case available
    case leave
    case `break`

    var color: Color {
        switch self {
        case .available: return .green
        case .leave: return .red
        case .break: return .yellow
        }
    }

    var title: String {
        switch self {
        case .available: return "Available"
        case .leave: return "Leave"
        case .break: return "Break"
        }
    }
}

private struct StatusChip: View {
    let name: String
    let status: RosterStatus

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(status.color)
                .frame(width: 8, height: 8)
            Text(name)
                .font(.caption.weight(.bold))
            Text(status.title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(status.color)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(status.color.opacity(0.12), in: Capsule())
    }
}

private struct FlowLayout<Data: RandomAccessCollection, Content: View>: View where Data.Element: Hashable {
    let items: Data
    let content: (Data.Element) -> Content

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 8)], alignment: .leading, spacing: 8) {
            ForEach(Array(items), id: \.self) { item in
                content(item)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

private struct ErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load data", systemImage: "exclamationmark.triangle.fill")
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

private struct EmptyStateCard: View {
    let icon: String
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.largeTitle)
                .foregroundStyle(InciTeamTheme.primary)
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}
