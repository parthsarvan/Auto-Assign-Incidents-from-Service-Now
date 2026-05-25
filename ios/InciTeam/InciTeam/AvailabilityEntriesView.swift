import SwiftUI

enum AvailabilityEntryKind {
    case leave
    case breakPeriod

    var title: String {
        switch self {
        case .leave: "Leaves"
        case .breakPeriod: "Breaks"
        }
    }

    var singularTitle: String {
        switch self {
        case .leave: "Leave"
        case .breakPeriod: "Break"
        }
    }

    var subtitle: String {
        switch self {
        case .leave: "Planned absences that assignment logic avoids."
        case .breakPeriod: "Short unavailable windows for active schedules."
        }
    }

    var systemImage: String {
        switch self {
        case .leave: "figure.walk.departure"
        case .breakPeriod: "cup.and.saucer.fill"
        }
    }

    var tone: FeatureTone {
        switch self {
        case .leave: .orange
        case .breakPeriod: .green
        }
    }
}

struct AvailabilityEntriesView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var entries: [AvailabilityEntrySummary] = []
    @State private var members: [TeamMemberSummary] = []
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var editorDraft: AvailabilityEditorDraft?
    @State private var entryToDelete: AvailabilityEntrySummary?

    let kind: AvailabilityEntryKind

    private let apiClient = InciTeamAPIClient()

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header

                    if !canManageTeam {
                        selfServiceBanner
                    }

                    if isLoading {
                        ProgressView("Loading \(kind.title.lowercased())...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        AvailabilityErrorCard(message: errorMessage) {
                            Task { await loadData() }
                        }
                    } else if entries.isEmpty {
                        AvailabilityEmptyCard(kind: kind, canCreate: canCreateEntry) {
                            editorDraft = AvailabilityEditorDraft(kind: kind, memberId: defaultMemberId)
                        }
                    } else {
                        ForEach(entries) { entry in
                            AvailabilityEntryCard(
                                entry: entry,
                                kind: kind,
                                canManage: canManageTeam,
                                onEdit: {
                                    editorDraft = AvailabilityEditorDraft(kind: kind, entry: entry)
                                },
                                onDelete: {
                                    entryToDelete = entry
                                }
                            )
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle(kind.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canCreateEntry {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editorDraft = AvailabilityEditorDraft(kind: kind, memberId: defaultMemberId)
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
            AvailabilityEditorView(
                draft: draft,
                kind: kind,
                members: members,
                fixedMember: canManageTeam ? nil : selfMember,
                onCancel: {
                    editorDraft = nil
                },
                onSave: { nextDraft in
                    await save(nextDraft)
                }
            )
        }
        .alert("Delete \(kind.singularTitle.lowercased())?", isPresented: deleteAlertBinding) {
            Button("Delete", role: .destructive) {
                guard let entryToDelete else {
                    return
                }
                Task { await delete(entryToDelete) }
            }
            Button("Cancel", role: .cancel) {
                entryToDelete = nil
            }
        } message: {
            Text("This removes the selected unavailable window from the current team.")
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: kind.systemImage)
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(kind.tone.color.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(kind.title)
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text(kind.subtitle)
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer()
        }
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var selfServiceBanner: some View {
        let text = selfMember == nil
            ? "Read-only access. Ask a manager to link your work email to a team member record before adding your own \(kind.singularTitle.lowercased())."
            : "Self-service access. You can add your own \(kind.singularTitle.lowercased()); managers can edit or delete team-wide records."
        return Label(text, systemImage: selfMember == nil ? "lock.fill" : "person.crop.circle.badge.checkmark")
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(InciTeamTheme.primaryDeep)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(kind.tone.color.opacity(0.12), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var canManageTeam: Bool {
        sessionStore.currentUser?.canManageCurrentTeam ?? false
    }

    private var selfMember: TeamMemberSummary? {
        guard let workEmail = sessionStore.currentUser?.workEmail?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
              !workEmail.isEmpty else {
            return nil
        }
        return members.first {
            $0.email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == workEmail
        }
    }

    private var canCreateEntry: Bool {
        canManageTeam || selfMember != nil
    }

    private var defaultMemberId: Int64? {
        canManageTeam ? nil : selfMember?.id
    }

    private var deleteAlertBinding: Binding<Bool> {
        Binding(
            get: { entryToDelete != nil },
            set: { isPresented in
                if !isPresented {
                    entryToDelete = nil
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
            async let memberData = apiClient.fetchTeamMembers(token: token)
            let entryData: [AvailabilityEntrySummary]
            switch kind {
            case .leave:
                entryData = try await apiClient.fetchLeaveEntries(token: token)
            case .breakPeriod:
                entryData = try await apiClient.fetchBreakEntries(token: token)
            }
            entries = entryData.sorted { left, right in
                (DateFormatting.instantDate(from: left.startTs) ?? .distantFuture)
                    < (DateFormatting.instantDate(from: right.startTs) ?? .distantFuture)
            }
            members = try await memberData.sorted { $0.fullName < $1.fullName }
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func save(_ draft: AvailabilityEditorDraft) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        do {
            let request = draft.request
            switch kind {
            case .leave:
                if let editingId = draft.editingId {
                    _ = try await apiClient.updateLeave(id: editingId, token: token, request: request)
                } else {
                    _ = try await apiClient.createLeave(token: token, request: request)
                }
            case .breakPeriod:
                if let editingId = draft.editingId {
                    _ = try await apiClient.updateBreak(id: editingId, token: token, request: request)
                } else {
                    _ = try await apiClient.createBreak(token: token, request: request)
                }
            }
            editorDraft = nil
            await loadData()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func delete(_ entry: AvailabilityEntrySummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        do {
            switch kind {
            case .leave:
                try await apiClient.deleteLeave(id: entry.id, token: token)
            case .breakPeriod:
                try await apiClient.deleteBreak(id: entry.id, token: token)
            }
            entryToDelete = nil
            await loadData()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct AvailabilityEditorDraft: Identifiable {
    let id = UUID()
    var editingId: Int64?
    var memberId: Int64?
    var startDate = Date()
    var endDate = Calendar.current.date(byAdding: .hour, value: 1, to: Date()) ?? Date()
    var reason = ""

    init(kind: AvailabilityEntryKind, memberId: Int64?) {
        self.memberId = memberId
        if kind == .leave {
            endDate = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        }
    }

    init(kind: AvailabilityEntryKind, entry: AvailabilityEntrySummary) {
        editingId = entry.id
        memberId = entry.teamMember?.id
        startDate = DateFormatting.instantDate(from: entry.startTs) ?? Date()
        endDate = DateFormatting.instantDate(from: entry.endTs)
            ?? Calendar.current.date(byAdding: .hour, value: kind == .leave ? 24 : 1, to: startDate)
            ?? startDate
        reason = entry.reason ?? ""
    }

    var request: AvailabilityEntryRequest {
        AvailabilityEntryRequest(
            teamMemberId: memberId ?? 0,
            startTs: DateFormatting.isoInstant(startDate),
            endTs: DateFormatting.isoInstant(endDate),
            reason: reason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : reason.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    var isValid: Bool {
        memberId != nil && endDate >= startDate
    }
}

private struct AvailabilityEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @State var draft: AvailabilityEditorDraft

    let kind: AvailabilityEntryKind
    let members: [TeamMemberSummary]
    let fixedMember: TeamMemberSummary?
    let onCancel: () -> Void
    let onSave: (AvailabilityEditorDraft) async -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section("Team Member") {
                    if let fixedMember {
                        Text(fixedMember.fullName)
                    } else {
                        Picker("Member", selection: memberSelection) {
                            Text("Select Member").tag(Int64?.none)
                            ForEach(members) { member in
                                Text(member.fullName.isEmpty ? "Unnamed member" : member.fullName)
                                    .tag(Optional(member.id))
                            }
                        }
                    }
                }

                Section("Window") {
                    DatePicker("Start", selection: $draft.startDate, displayedComponents: [.date, .hourAndMinute])
                    DatePicker("End", selection: $draft.endDate, displayedComponents: [.date, .hourAndMinute])
                    if draft.endDate < draft.startDate {
                        Text("End time must be on or after start time.")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section("Reason") {
                    TextField("Optional", text: $draft.reason, axis: .vertical)
                        .lineLimit(2...4)
                }
            }
            .navigationTitle(draft.editingId == nil ? "Add \(kind.singularTitle)" : "Edit \(kind.singularTitle)")
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
        .onAppear {
            if let fixedMember {
                draft.memberId = fixedMember.id
            }
        }
    }

    private var memberSelection: Binding<Int64?> {
        Binding(
            get: { draft.memberId },
            set: { draft.memberId = $0 }
        )
    }
}

private struct AvailabilityEntryCard: View {
    let entry: AvailabilityEntrySummary
    let kind: AvailabilityEntryKind
    let canManage: Bool
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.teamMember?.fullName ?? "Unknown member")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text(entry.teamMember?.geo?.name ?? "No geo")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(kind.tone.color)
                }

                Spacer()

                Text(DateFormatting.duration(start: entry.startTs, end: entry.endTs))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(kind.tone.color)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(kind.tone.color.opacity(0.12), in: Capsule())
            }

            VStack(alignment: .leading, spacing: 8) {
                Label(DateFormatting.displayDateTime(entry.startTs), systemImage: "play.fill")
                Label(DateFormatting.displayDateTime(entry.endTs), systemImage: "stop.fill")
                if let reason = entry.reason, !reason.isEmpty {
                    Label(reason, systemImage: "text.alignleft")
                }
            }
            .font(.caption.weight(.semibold))
            .foregroundStyle(InciTeamTheme.muted)

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

private struct AvailabilityEmptyCard: View {
    let kind: AvailabilityEntryKind
    let canCreate: Bool
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: kind.systemImage)
                .font(.largeTitle)
                .foregroundStyle(kind.tone.color)
            Text("No \(kind.title.lowercased()) yet")
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)
            Text(kind.subtitle)
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
                .multilineTextAlignment(.center)
            if canCreate {
                Button("Add \(kind.singularTitle)", systemImage: "plus", action: onAdd)
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct AvailabilityErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load availability", systemImage: "exclamationmark.triangle.fill")
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
        AvailabilityEntriesView(kind: .leave)
    }
    .environment(SessionStore.previewSignedIn)
}
