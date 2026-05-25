import SwiftUI

struct ScheduleView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var schedules: [TeamMemberScheduleSummary] = []
    @State private var members: [TeamMemberSummary] = []
    @State private var geos: [GeoSummary] = []
    @State private var shifts: [ShiftSummary] = []
    @State private var geoShiftMappings: [GeoShiftMappingSummary] = []
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var editorDraft: ScheduleEditorDraft?
    @State private var scheduleToDelete: TeamMemberScheduleSummary?

    private let apiClient = InciTeamAPIClient()

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header

                    if !canManageTeam {
                        ReadOnlyBanner()
                    }

                    if isLoading {
                        ProgressView("Loading schedules...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        ScheduleErrorCard(message: errorMessage) {
                            Task { await loadData() }
                        }
                    } else if schedules.isEmpty {
                        ScheduleEmptyCard(canManageTeam: canManageTeam) {
                            editorDraft = ScheduleEditorDraft()
                        }
                    } else {
                        ForEach(schedules) { schedule in
                            ScheduleCard(
                                schedule: schedule,
                                canManage: canManageTeam,
                                onEdit: {
                                    editorDraft = ScheduleEditorDraft(schedule: schedule)
                                },
                                onDelete: {
                                    scheduleToDelete = schedule
                                }
                            )
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("Schedule")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canManageTeam {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editorDraft = ScheduleEditorDraft()
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .task {
            await loadData()
        }
        .refreshable {
            await loadData()
        }
        .sheet(item: $editorDraft) { draft in
            ScheduleEditorView(
                draft: draft,
                members: members,
                geos: geos,
                shifts: shifts,
                geoShiftMappings: geoShiftMappings,
                onCancel: {
                    editorDraft = nil
                },
                onSave: { nextDraft in
                    await save(nextDraft)
                }
            )
        }
        .alert("Delete schedule?", isPresented: deleteAlertBinding) {
            Button("Delete", role: .destructive) {
                guard let scheduleToDelete else {
                    return
                }
                Task {
                    await delete(scheduleToDelete)
                }
            }
            Button("Cancel", role: .cancel) {
                scheduleToDelete = nil
            }
        } message: {
            Text("This removes the selected coverage window from the current team.")
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "calendar")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(InciTeamTheme.primary.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("Schedule")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("Shift coverage for \(teamName).")
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer()
        }
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var canManageTeam: Bool {
        sessionStore.currentUser?.canManageCurrentTeam ?? false
    }

    private var teamName: String {
        sessionStore.currentUser?.workspace?.teamName ?? "current team"
    }

    private var deleteAlertBinding: Binding<Bool> {
        Binding(
            get: { scheduleToDelete != nil },
            set: { isPresented in
                if !isPresented {
                    scheduleToDelete = nil
                }
            }
        )
    }

    private func loadData() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            async let scheduleData = apiClient.fetchSchedules(token: token)
            async let memberData = apiClient.fetchTeamMembers(token: token)
            async let geoData = apiClient.fetchGeos(token: token)
            async let shiftData = apiClient.fetchShifts(token: token)
            async let mappingData = apiClient.fetchGeoShiftMappings(token: token)

            schedules = try await scheduleData.sorted { left, right in
                left.startDate < right.startDate
            }
            members = try await memberData.sorted { $0.fullName < $1.fullName }
            geos = try await geoData.sorted { $0.name < $1.name }
            shifts = try await shiftData.sorted { $0.name < $1.name }
            geoShiftMappings = try await mappingData
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func save(_ draft: ScheduleEditorDraft) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        do {
            let request = draft.request
            if let editingId = draft.editingId {
                try await apiClient.updateSchedule(id: editingId, token: token, request: request)
            } else {
                try await apiClient.createSchedule(token: token, request: request)
            }
            editorDraft = nil
            await loadData()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func delete(_ schedule: TeamMemberScheduleSummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        do {
            try await apiClient.deleteSchedule(id: schedule.id, token: token)
            scheduleToDelete = nil
            await loadData()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct ScheduleEditorDraft: Identifiable {
    let id = UUID()
    var editingId: Int64?
    var selectedMemberIds: Set<Int64> = []
    var geoId: Int64?
    var shiftId: Int64?
    var startDate = Date()
    var endDate = Date()
    var coverageDays = Set(DateFormatting.dayNames.map(\.value))

    init() {}

    init(schedule: TeamMemberScheduleSummary) {
        editingId = schedule.id
        if let memberId = schedule.teamMember?.id {
            selectedMemberIds = [memberId]
        }
        geoId = schedule.geo?.id
        shiftId = schedule.shift?.id
        startDate = DateFormatting.date(from: schedule.startDate) ?? Date()
        endDate = DateFormatting.date(from: schedule.endDate) ?? Date()
        coverageDays = Set(DateFormatting.parseCoverageDays(schedule.coverageDays))
    }

    var isEditing: Bool {
        editingId != nil
    }

    var request: TeamMemberScheduleRequest {
        TeamMemberScheduleRequest(
            teamMemberId: isEditing ? selectedMemberIds.first : nil,
            teamMemberIds: isEditing ? nil : Array(selectedMemberIds).sorted(),
            geoId: geoId ?? 0,
            shiftId: shiftId ?? 0,
            startDate: DateFormatting.isoDate(startDate),
            endDate: DateFormatting.isoDate(endDate),
            coverageDays: Array(coverageDays).sorted()
        )
    }

    var isValid: Bool {
        !selectedMemberIds.isEmpty
            && geoId != nil
            && shiftId != nil
            && !coverageDays.isEmpty
            && endDate >= Calendar.current.startOfDay(for: startDate)
    }
}

private struct ScheduleEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @State var draft: ScheduleEditorDraft

    let members: [TeamMemberSummary]
    let geos: [GeoSummary]
    let shifts: [ShiftSummary]
    let geoShiftMappings: [GeoShiftMappingSummary]
    let onCancel: () -> Void
    let onSave: (ScheduleEditorDraft) async -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section("Coverage") {
                    Picker("Geo", selection: geoSelection) {
                        Text("Select Geo").tag(Int64?.none)
                        ForEach(geos) { geo in
                            Text(geo.name).tag(Optional(geo.id))
                        }
                    }

                    Picker("Shift", selection: shiftSelection) {
                        Text("Select Shift").tag(Int64?.none)
                        ForEach(allowedShifts) { shift in
                            Text(shift.name).tag(Optional(shift.id))
                        }
                    }
                    .disabled(draft.geoId == nil || allowedShifts.isEmpty)

                    if draft.geoId != nil && allowedShifts.isEmpty {
                        Text("No shifts are mapped to this geo yet.")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section(draft.isEditing ? "Team Member" : "Team Members") {
                    if draft.geoId == nil {
                        Text("Select a geo first.")
                            .foregroundStyle(InciTeamTheme.muted)
                    } else if filteredMembers.isEmpty {
                        Text("No team members belong to the selected geo.")
                            .foregroundStyle(InciTeamTheme.muted)
                    } else {
                        ForEach(filteredMembers) { member in
                            Button {
                                toggleMember(member.id)
                            } label: {
                                HStack {
                                    VStack(alignment: .leading) {
                                        Text(member.fullName.isEmpty ? "Unnamed member" : member.fullName)
                                            .foregroundStyle(InciTeamTheme.ink)
                                        if let email = member.email {
                                            Text(email)
                                                .font(.caption)
                                                .foregroundStyle(InciTeamTheme.muted)
                                        }
                                    }
                                    Spacer()
                                    if draft.selectedMemberIds.contains(member.id) {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundStyle(InciTeamTheme.primary)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                Section("Dates") {
                    DatePicker("Start Date", selection: $draft.startDate, displayedComponents: .date)
                    DatePicker("End Date", selection: $draft.endDate, displayedComponents: .date)
                }

                Section("Repeat") {
                    CoveragePresetPicker(coverageDays: $draft.coverageDays)
                    ForEach(DateFormatting.dayNames, id: \.value) { day in
                        Toggle(day.long, isOn: coverageDayBinding(day.value))
                    }
                }
            }
            .navigationTitle(draft.isEditing ? "Edit Schedule" : "Add Schedule")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task {
                            await onSave(draft)
                            dismiss()
                        }
                    }
                    .disabled(!draft.isValid)
                }
            }
        }
        .onChange(of: draft.geoId) { _, _ in
            reconcileSelectionsForGeo()
        }
    }

    private var geoSelection: Binding<Int64?> {
        Binding(
            get: { draft.geoId },
            set: { draft.geoId = $0 }
        )
    }

    private var shiftSelection: Binding<Int64?> {
        Binding(
            get: { draft.shiftId },
            set: { draft.shiftId = $0 }
        )
    }

    private var filteredMembers: [TeamMemberSummary] {
        guard let geoId = draft.geoId else {
            return []
        }
        return members.filter { $0.geo?.id == geoId }
    }

    private var allowedShifts: [ShiftSummary] {
        guard let geoId = draft.geoId else {
            return []
        }
        let allowedIds = Set(geoShiftMappings.compactMap { mapping in
            mapping.geo?.id == geoId ? mapping.shift?.id : nil
        })
        return shifts.filter { allowedIds.contains($0.id) }
    }

    private func toggleMember(_ memberId: Int64) {
        if draft.isEditing {
            draft.selectedMemberIds = [memberId]
            return
        }

        if draft.selectedMemberIds.contains(memberId) {
            draft.selectedMemberIds.remove(memberId)
        } else {
            draft.selectedMemberIds.insert(memberId)
        }
    }

    private func reconcileSelectionsForGeo() {
        let validMemberIds = Set(filteredMembers.map(\.id))
        draft.selectedMemberIds = draft.selectedMemberIds.intersection(validMemberIds)
        let allowedShiftIds = Set(allowedShifts.map(\.id))
        if let shiftId = draft.shiftId, !allowedShiftIds.contains(shiftId) {
            draft.shiftId = nil
        }
        if draft.shiftId == nil, allowedShifts.count == 1 {
            draft.shiftId = allowedShifts[0].id
        }
    }

    private func coverageDayBinding(_ day: String) -> Binding<Bool> {
        Binding(
            get: { draft.coverageDays.contains(day) },
            set: { isSelected in
                if isSelected {
                    draft.coverageDays.insert(day)
                } else {
                    draft.coverageDays.remove(day)
                }
            }
        )
    }
}

private struct CoveragePresetPicker: View {
    @Binding var coverageDays: Set<String>

    var body: some View {
        Picker("Preset", selection: presetBinding) {
            Text("Every day").tag(CoveragePreset.everyDay)
            Text("Weekdays").tag(CoveragePreset.weekdays)
            Text("Weekend").tag(CoveragePreset.weekend)
            Text("Custom").tag(CoveragePreset.custom)
        }
    }

    private var presetBinding: Binding<CoveragePreset> {
        Binding(
            get: { CoveragePreset(days: coverageDays) },
            set: { preset in
                if preset != .custom {
                    coverageDays = preset.days
                }
            }
        )
    }
}

private enum CoveragePreset: Hashable {
    case everyDay
    case weekdays
    case weekend
    case custom

    init(days: Set<String>) {
        if days == Self.everyDay.days {
            self = .everyDay
        } else if days == Self.weekdays.days {
            self = .weekdays
        } else if days == Self.weekend.days {
            self = .weekend
        } else {
            self = .custom
        }
    }

    var days: Set<String> {
        switch self {
        case .everyDay:
            return Set(DateFormatting.dayNames.map(\.value))
        case .weekdays:
            return Set(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"])
        case .weekend:
            return Set(["SATURDAY", "SUNDAY"])
        case .custom:
            return []
        }
    }
}

private struct ScheduleCard: View {
    let schedule: TeamMemberScheduleSummary
    let canManage: Bool
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(schedule.teamMember?.fullName ?? "Unknown member")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text("\(schedule.geo?.name ?? "-") / \(schedule.shift?.name ?? "-")")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(InciTeamTheme.primary)
                }

                Spacer()

                Text(DateFormatting.formatCoverageDays(schedule.coverageDays))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(InciTeamTheme.primary.opacity(0.12), in: Capsule())
            }

            HStack(spacing: 12) {
                ScheduleMetaPill(icon: "calendar", text: "\(DateFormatting.displayDate(schedule.startDate)) - \(DateFormatting.displayDate(schedule.endDate))")
                if let days = DateFormatting.dayCount(startDate: schedule.startDate, endDate: schedule.endDate) {
                    ScheduleMetaPill(icon: "clock", text: "\(days) day\(days == 1 ? "" : "s")")
                }
            }

            if canManage {
                HStack {
                    Button("Edit", systemImage: "pencil", action: onEdit)
                        .buttonStyle(.bordered)
                    Button("Delete", systemImage: "trash", role: .destructive, action: onDelete)
                        .buttonStyle(.bordered)
                }
            }
        }
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }
}

private struct ScheduleMetaPill: View {
    let icon: String
    let text: String

    var body: some View {
        Label(text, systemImage: icon)
            .font(.caption.weight(.bold))
            .foregroundStyle(InciTeamTheme.muted)
            .padding(.horizontal, 9)
            .padding(.vertical, 6)
            .background(InciTeamTheme.row, in: Capsule())
    }
}

private struct ReadOnlyBanner: View {
    var body: some View {
        Label("Read-only access. Contact a team manager or admin to make schedule changes.", systemImage: "lock.fill")
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(InciTeamTheme.primaryDeep)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(InciTeamTheme.primary.opacity(0.10), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct ScheduleEmptyCard: View {
    let canManageTeam: Bool
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.largeTitle)
                .foregroundStyle(InciTeamTheme.primary)
            Text("No schedules yet")
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)
            Text("Schedules define who covers each mapped shift window.")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
                .multilineTextAlignment(.center)
            if canManageTeam {
                Button("Add Schedule", systemImage: "plus", action: onAdd)
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct ScheduleErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load schedules", systemImage: "exclamationmark.triangle.fill")
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
