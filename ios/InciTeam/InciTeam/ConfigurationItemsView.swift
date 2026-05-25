import SwiftUI

struct ConfigurationItemsView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var items: [ConfigurationItemSummary] = []
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var editorDraft: ConfigurationItemEditorDraft?
    @State private var itemToDelete: ConfigurationItemSummary?

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
                        ProgressView("Loading configuration items...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        ConfigurationItemErrorCard(message: errorMessage) {
                            Task { await loadItems() }
                        }
                    } else if items.isEmpty {
                        ConfigurationItemEmptyCard(canManageTeam: canManageTeam) {
                            editorDraft = ConfigurationItemEditorDraft()
                        }
                    } else {
                        ForEach(items) { item in
                            ConfigurationItemCard(
                                item: item,
                                canManage: canManageTeam,
                                onEdit: {
                                    editorDraft = ConfigurationItemEditorDraft(item: item)
                                },
                                onDelete: {
                                    itemToDelete = item
                                }
                            )
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("CI")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canManageTeam {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editorDraft = ConfigurationItemEditorDraft()
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .task {
            await loadItems()
        }
        .refreshable {
            await loadItems()
        }
        .sheet(item: $editorDraft) { draft in
            ConfigurationItemEditorView(
                draft: draft,
                onCancel: {
                    editorDraft = nil
                },
                onSave: { nextDraft in
                    await save(nextDraft)
                }
            )
        }
        .alert("Delete configuration item?", isPresented: deleteAlertBinding) {
            Button("Delete", role: .destructive) {
                guard let itemToDelete else {
                    return
                }
                Task { await delete(itemToDelete) }
            }
            Button("Cancel", role: .cancel) {
                itemToDelete = nil
            }
        } message: {
            Text("This removes the CI from this team's routing configuration.")
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "server.rack")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(FeatureTone.slate.color.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("Configuration Items")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("\(items.count) supported systems in \(teamName).")
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
        Label("Read-only access. Team managers and admins can add, update, or remove CIs.", systemImage: "lock.fill")
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(InciTeamTheme.primaryDeep)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(InciTeamTheme.primary.opacity(0.10), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var canManageTeam: Bool {
        sessionStore.currentUser?.canManageCurrentTeam ?? false
    }

    private var teamName: String {
        sessionStore.currentUser?.workspace?.teamName ?? "current team"
    }

    private var deleteAlertBinding: Binding<Bool> {
        Binding(
            get: { itemToDelete != nil },
            set: { isPresented in
                if !isPresented {
                    itemToDelete = nil
                }
            }
        )
    }

    private func loadItems() async {
        guard let token = sessionStore.session?.token else {
            return
        }

        isLoading = true
        errorMessage = ""
        do {
            items = try await apiClient.fetchConfigurationItems(token: token)
                .sorted { $0.name < $1.name }
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func save(_ draft: ConfigurationItemEditorDraft) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        do {
            let request = draft.request
            if let editingId = draft.editingId {
                _ = try await apiClient.updateConfigurationItem(id: editingId, token: token, request: request)
            } else {
                _ = try await apiClient.createConfigurationItem(token: token, request: request)
            }
            editorDraft = nil
            await loadItems()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func delete(_ item: ConfigurationItemSummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        do {
            try await apiClient.deleteConfigurationItem(id: item.id, token: token)
            itemToDelete = nil
            await loadItems()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct ConfigurationItemEditorDraft: Identifiable {
    let id = UUID()
    var editingId: Int64?
    var name = ""
    var description = ""
    var serviceNowSysId = ""

    init() {}

    init(item: ConfigurationItemSummary) {
        editingId = item.id
        name = item.name
        description = item.description ?? ""
        serviceNowSysId = item.serviceNowSysId ?? ""
    }

    var request: ConfigurationItemRequest {
        ConfigurationItemRequest(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            description: description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? nil
                : description.trimmingCharacters(in: .whitespacesAndNewlines),
            serviceNowSysId: serviceNowSysId.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !serviceNowSysId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

private struct ConfigurationItemEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(SessionStore.self) private var sessionStore
    @State var draft: ConfigurationItemEditorDraft
    @State private var serviceNowSearch: String
    @State private var serviceNowResults: [ServiceNowLookupResult] = []
    @State private var isServiceNowLookupLoading = false
    @State private var serviceNowLookupComplete = false
    @State private var serviceNowLookupError = ""
    @State private var selectedServiceNowLabel: String
    @State private var selectedServiceNowSearch: String

    let onCancel: () -> Void
    let onSave: (ConfigurationItemEditorDraft) async -> Void

    private let apiClient = InciTeamAPIClient()

    init(
        draft: ConfigurationItemEditorDraft,
        onCancel: @escaping () -> Void,
        onSave: @escaping (ConfigurationItemEditorDraft) async -> Void
    ) {
        let initialSearch = draft.name
        let initialLabel = draft.serviceNowSysId.isEmpty ? "" : draft.name
        self._draft = State(initialValue: draft)
        self._serviceNowSearch = State(initialValue: initialSearch)
        self._selectedServiceNowLabel = State(initialValue: initialLabel)
        self._selectedServiceNowSearch = State(initialValue: initialSearch)
        self.onCancel = onCancel
        self.onSave = onSave
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("ServiceNow Link") {
                    TextField("Search by CI name, asset tag, or serial number", text: serviceNowSearchBinding)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                    Text("Select the matching ServiceNow record. InciTeam keeps the ServiceNow link behind the scenes.")
                        .font(.caption)
                        .foregroundStyle(InciTeamTheme.muted)

                    if isServiceNowLookupLoading {
                        HStack(spacing: 10) {
                            ProgressView()
                            Text("Searching ServiceNow...")
                                .foregroundStyle(InciTeamTheme.muted)
                        }
                    }

                    if !serviceNowLookupError.isEmpty {
                        Text(serviceNowLookupError)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }

                    if serviceNowLookupComplete
                        && !isServiceNowLookupLoading
                        && selectedServiceNowLabel.isEmpty
                        && serviceNowResults.isEmpty {
                        Text("No matching ServiceNow CI found. Try a fuller CI name from ServiceNow.")
                            .font(.caption)
                            .foregroundStyle(.orange)
                    }

                    ForEach(serviceNowResults) { result in
                        Button {
                            selectServiceNowItem(result)
                        } label: {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(result.primaryLabel)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(InciTeamTheme.ink)
                                Text(configurationItemDetail(result))
                                    .font(.caption)
                                    .foregroundStyle(InciTeamTheme.muted)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }

                    if !selectedServiceNowLabel.isEmpty {
                        Label("Linked ServiceNow CI: \(selectedServiceNowLabel)", systemImage: "link")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.green)
                    }
                }
                .task(id: serviceNowSearch) {
                    await searchServiceNowConfigurationItems(for: serviceNowSearch)
                }

                Section("Details") {
                    TextField("Name", text: $draft.name)
                    TextField("Description", text: $draft.description, axis: .vertical)
                        .lineLimit(2...4)
                }
            }
            .navigationTitle(draft.editingId == nil ? "Add CI" : "Edit CI")
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

    private var serviceNowSearchBinding: Binding<String> {
        Binding(
            get: { serviceNowSearch },
            set: { query in
                serviceNowSearch = query
                if query != selectedServiceNowSearch {
                    clearServiceNowSelection()
                }
            }
        )
    }

    @MainActor
    private func searchServiceNowConfigurationItems(for rawQuery: String) async {
        let query = rawQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        if !selectedServiceNowLabel.isEmpty && query == selectedServiceNowSearch {
            serviceNowResults = []
            serviceNowLookupComplete = false
            serviceNowLookupError = ""
            isServiceNowLookupLoading = false
            return
        }

        guard query.count >= 2 else {
            serviceNowResults = []
            serviceNowLookupComplete = false
            serviceNowLookupError = ""
            isServiceNowLookupLoading = false
            return
        }

        guard let token = sessionStore.session?.token else {
            return
        }

        isServiceNowLookupLoading = true
        serviceNowLookupError = ""
        serviceNowLookupComplete = false

        do {
            try await Task.sleep(nanoseconds: 300_000_000)
            try Task.checkCancellation()
            let results = try await apiClient.searchServiceNowConfigurationItems(token: token, query: query)
            try Task.checkCancellation()

            serviceNowResults = results
            serviceNowLookupComplete = true
            isServiceNowLookupLoading = false
        } catch is CancellationError {
            return
        } catch {
            serviceNowResults = []
            serviceNowLookupComplete = true
            serviceNowLookupError = error.localizedDescription
            isServiceNowLookupLoading = false
        }
    }

    private func selectServiceNowItem(_ result: ServiceNowLookupResult) {
        let label = result.primaryLabel
        draft.serviceNowSysId = result.sysId
        draft.name = label
        draft.description = [result.detail, result.secondaryDetail]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: " / ")
        selectedServiceNowLabel = label
        selectedServiceNowSearch = label
        serviceNowSearch = label
        serviceNowResults = []
        serviceNowLookupComplete = false
        serviceNowLookupError = ""
        isServiceNowLookupLoading = false
    }

    private func clearServiceNowSelection() {
        draft.serviceNowSysId = ""
        selectedServiceNowLabel = ""
        selectedServiceNowSearch = ""
        serviceNowLookupComplete = false
        serviceNowLookupError = ""
    }

    private func configurationItemDetail(_ result: ServiceNowLookupResult) -> String {
        let values = [result.detail, result.secondaryDetail]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return values.isEmpty ? "Configuration item" : values.joined(separator: " / ")
    }
}

private struct ConfigurationItemCard: View {
    let item: ConfigurationItemSummary
    let canManage: Bool
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "server.rack")
                    .font(.headline.weight(.heavy))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
                    .background(FeatureTone.slate.color.gradient, in: RoundedRectangle(cornerRadius: 15, style: .continuous))

                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text(item.description?.isEmpty == false ? item.description ?? "" : "No description")
                        .font(.subheadline)
                        .foregroundStyle(InciTeamTheme.muted)
                        .lineLimit(2)
                }

                Spacer()

                Text(item.serviceNowSysId?.isEmpty == false ? "Linked" : "Needs link")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(item.serviceNowSysId?.isEmpty == false ? .green : .orange)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background((item.serviceNowSysId?.isEmpty == false ? Color.green : Color.orange).opacity(0.12), in: Capsule())
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

private struct ConfigurationItemEmptyCard: View {
    let canManageTeam: Bool
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "server.rack")
                .font(.largeTitle)
                .foregroundStyle(InciTeamTheme.primary)
            Text("No configuration items yet")
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)
            Text("Search ServiceNow and link each supported CI before mapping ownership.")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
                .multilineTextAlignment(.center)
            if canManageTeam {
                Button("Add CI", systemImage: "plus", action: onAdd)
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct ConfigurationItemErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load configuration items", systemImage: "exclamationmark.triangle.fill")
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
        ConfigurationItemsView()
    }
    .environment(SessionStore.previewSignedIn)
}
