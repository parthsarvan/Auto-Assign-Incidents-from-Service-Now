import SwiftUI

struct CiUserMappingView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var configurationItems: [ConfigurationItemSummary] = []
    @State private var members: [TeamMemberSummary] = []
    @State private var mappings: [CiUserMappingSummary] = []
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var editorDraft: CiMappingEditorDraft?

    private let apiClient = InciTeamAPIClient()

    var body: some View {
        ZStack {
            InciTeamTheme.background
                .ignoresSafeArea()

            ScrollView {
                LazyVStack(spacing: 16) {
                    header

                    if !canManageTeam {
                        readOnlyBanner
                    }

                    if isLoading {
                        ProgressView("Loading CI mappings...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        CiMappingErrorCard(message: errorMessage) {
                            Task { await loadData() }
                        }
                    } else if groups.isEmpty {
                        CiMappingEmptyCard(canManageTeam: canManageTeam) {
                            editorDraft = CiMappingEditorDraft()
                        }
                    } else {
                        ForEach(groups) { group in
                            CiMappingGroupCard(
                                group: group,
                                canManage: canManageTeam,
                                onEdit: {
                                    editorDraft = CiMappingEditorDraft(group: group)
                                }
                            )
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("CI User Mapping")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canManageTeam {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editorDraft = CiMappingEditorDraft()
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
            CiMappingEditorView(
                draft: draft,
                items: configurationItems,
                members: members,
                groups: groups,
                onCancel: {
                    editorDraft = nil
                },
                onSave: { nextDraft in
                    await save(nextDraft)
                }
            )
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "point.3.connected.trianglepath.dotted")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(FeatureTone.slate.color.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("CI User Mapping")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("Ordered ownership for ServiceNow routing.")
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            }

            Spacer()
        }
        .padding(18)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }

    private var readOnlyBanner: some View {
        Label("Read-only access. Team managers and admins can update CI ownership order.", systemImage: "lock.fill")
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(InciTeamTheme.primaryDeep)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(InciTeamTheme.primary.opacity(0.10), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var canManageTeam: Bool {
        sessionStore.currentUser?.canManageCurrentTeam ?? false
    }

    private var groups: [CiMappingGroup] {
        configurationItems.map { item in
            let itemMappings = mappings
                .filter { $0.configurationItem?.id == item.id }
                .sorted { left, right in
                    let leftOrder = left.sortOrder ?? Int.max
                    let rightOrder = right.sortOrder ?? Int.max
                    if leftOrder != rightOrder {
                        return leftOrder < rightOrder
                    }
                    return (left.teamMember?.fullName ?? "") < (right.teamMember?.fullName ?? "")
                }
            return CiMappingGroup(item: item, mappings: itemMappings)
        }
        .sorted { $0.item.name < $1.item.name }
    }

    private func loadData() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            async let itemData = apiClient.fetchConfigurationItems(token: token)
            async let memberData = apiClient.fetchTeamMembers(token: token)
            async let mappingData = apiClient.fetchCiUserMappings(token: token)
            configurationItems = try await itemData.sorted { $0.name < $1.name }
            members = try await memberData.sorted { $0.fullName < $1.fullName }
            mappings = try await mappingData
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func save(_ draft: CiMappingEditorDraft) async {
        guard let token = sessionStore.session?.token,
              let configurationItemId = draft.configurationItemId else {
            return
        }

        do {
            _ = try await apiClient.replaceCiUserMappingsForCi(
                token: token,
                request: CiUserMappingBulkRequest(
                    configurationItemId: configurationItemId,
                    teamMemberIds: draft.memberIds
                )
            )
            editorDraft = nil
            await loadData()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct CiMappingGroup: Identifiable {
    let item: ConfigurationItemSummary
    let mappings: [CiUserMappingSummary]

    var id: Int64 { item.id }

    var memberIds: [Int64] {
        mappings.compactMap { $0.teamMember?.id }
    }
}

struct CiMappingEditorDraft: Identifiable {
    let id = UUID()
    var configurationItemId: Int64?
    var memberIds: [Int64] = []
    var memberToAddId: Int64?

    init() {}

    init(group: CiMappingGroup) {
        configurationItemId = group.item.id
        memberIds = group.memberIds
    }

    var isValid: Bool {
        configurationItemId != nil && !memberIds.isEmpty
    }
}

private struct CiMappingEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @State var draft: CiMappingEditorDraft

    let items: [ConfigurationItemSummary]
    let members: [TeamMemberSummary]
    let groups: [CiMappingGroup]
    let onCancel: () -> Void
    let onSave: (CiMappingEditorDraft) async -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section("Configuration Item") {
                    Picker("CI", selection: itemSelection) {
                        Text("Select CI").tag(Int64?.none)
                        ForEach(items) { item in
                            Text(item.name).tag(Optional(item.id))
                        }
                    }
                }

                Section("Add Owner") {
                    Picker("Team Member", selection: memberToAddSelection) {
                        Text("Select Member").tag(Int64?.none)
                        ForEach(availableMembers) { member in
                            Text(member.fullName.isEmpty ? "Unnamed member" : member.fullName)
                                .tag(Optional(member.id))
                        }
                    }

                    Button("Add to Order", systemImage: "plus") {
                        addSelectedMember()
                    }
                    .disabled(draft.memberToAddId == nil)
                }

                Section("Assignment Order") {
                    if selectedMembers.isEmpty {
                        Text("Add one or more owners. This order becomes the assignment order for the CI.")
                            .foregroundStyle(InciTeamTheme.muted)
                    } else {
                        ForEach(Array(selectedMembers.enumerated()), id: \.element.id) { index, member in
                            HStack(spacing: 12) {
                                Text("\(index + 1)")
                                    .font(.caption.weight(.heavy))
                                    .foregroundStyle(.white)
                                    .frame(width: 28, height: 28)
                                    .background(InciTeamTheme.primary, in: Circle())

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(member.fullName)
                                        .foregroundStyle(InciTeamTheme.ink)
                                    Text(member.geo?.name ?? "No geo")
                                        .font(.caption)
                                        .foregroundStyle(InciTeamTheme.muted)
                                }

                                Spacer()

                                Button {
                                    moveMember(member.id, by: -1)
                                } label: {
                                    Image(systemName: "chevron.up")
                                }
                                .disabled(index == 0)

                                Button {
                                    moveMember(member.id, by: 1)
                                } label: {
                                    Image(systemName: "chevron.down")
                                }
                                .disabled(index == selectedMembers.count - 1)

                                Button(role: .destructive) {
                                    draft.memberIds.removeAll { $0 == member.id }
                                } label: {
                                    Image(systemName: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("CI Owner Order")
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
    }

    private var itemSelection: Binding<Int64?> {
        Binding(
            get: { draft.configurationItemId },
            set: { itemId in
                draft.configurationItemId = itemId
                if let itemId, let group = groups.first(where: { $0.item.id == itemId }) {
                    draft.memberIds = group.memberIds
                } else {
                    draft.memberIds = []
                }
                draft.memberToAddId = nil
            }
        )
    }

    private var memberToAddSelection: Binding<Int64?> {
        Binding(
            get: { draft.memberToAddId },
            set: { draft.memberToAddId = $0 }
        )
    }

    private var selectedMembers: [TeamMemberSummary] {
        draft.memberIds.compactMap { memberId in
            members.first { $0.id == memberId }
        }
    }

    private var availableMembers: [TeamMemberSummary] {
        members.filter { !draft.memberIds.contains($0.id) }
    }

    private func addSelectedMember() {
        guard let memberToAddId = draft.memberToAddId,
              !draft.memberIds.contains(memberToAddId) else {
            return
        }
        draft.memberIds.append(memberToAddId)
        draft.memberToAddId = nil
    }

    private func moveMember(_ memberId: Int64, by offset: Int) {
        guard let currentIndex = draft.memberIds.firstIndex(of: memberId) else {
            return
        }
        let nextIndex = currentIndex + offset
        guard draft.memberIds.indices.contains(nextIndex) else {
            return
        }
        draft.memberIds.swapAt(currentIndex, nextIndex)
    }
}

private struct CiMappingGroupCard: View {
    let group: CiMappingGroup
    let canManage: Bool
    let onEdit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(group.item.name)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                }

                Spacer()

                Text("\(group.mappings.count) owner\(group.mappings.count == 1 ? "" : "s")")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(InciTeamTheme.primary.opacity(0.12), in: Capsule())
            }

            if group.mappings.isEmpty {
                Text("No owners mapped yet.")
                    .font(.subheadline)
                    .foregroundStyle(InciTeamTheme.muted)
            } else {
                VStack(spacing: 8) {
                    ForEach(Array(group.mappings.enumerated()), id: \.element.id) { index, mapping in
                        HStack(spacing: 10) {
                            Text("\(index + 1)")
                                .font(.caption.weight(.heavy))
                                .foregroundStyle(.white)
                                .frame(width: 26, height: 26)
                                .background(InciTeamTheme.primary, in: Circle())
                            Text(mapping.teamMember?.fullName ?? "Unknown member")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(InciTeamTheme.ink)
                            Spacer()
                            Text(mapping.teamMember?.geo?.name ?? "-")
                                .font(.caption)
                                .foregroundStyle(InciTeamTheme.muted)
                        }
                        .padding(10)
                        .background(InciTeamTheme.row, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                }
            }

            if canManage {
                Button(group.mappings.isEmpty ? "Configure" : "Update Order", systemImage: "slider.horizontal.3", action: onEdit)
                    .buttonStyle(.bordered)
            }
        }
        .padding(16)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: InciTeamTheme.primaryDeep.opacity(0.08), radius: 18, x: 0, y: 10)
    }
}

private struct CiMappingEmptyCard: View {
    let canManageTeam: Bool
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "point.3.connected.trianglepath.dotted")
                .font(.largeTitle)
                .foregroundStyle(InciTeamTheme.primary)
            Text("No configuration items")
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)
            Text("Add configuration items in setup before mapping owners.")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
                .multilineTextAlignment(.center)
            if canManageTeam {
                Button("Refresh", systemImage: "arrow.clockwise", action: onAdd)
                    .buttonStyle(.bordered)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct CiMappingErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load CI mappings", systemImage: "exclamationmark.triangle.fill")
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
        CiUserMappingView()
    }
    .environment(SessionStore.previewSignedIn)
}
