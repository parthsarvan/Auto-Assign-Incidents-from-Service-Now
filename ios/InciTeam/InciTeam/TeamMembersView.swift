import SwiftUI

struct TeamMembersView: View {
    @Environment(SessionStore.self) private var sessionStore
    @State private var members: [TeamMemberSummary] = []
    @State private var geos: [GeoSummary] = []
    @State private var joinedUsers: [JoinedTeamUserSummary] = []
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var successMessage = ""
    @State private var editorDraft: TeamMemberEditorDraft?
    @State private var memberToDelete: TeamMemberSummary?

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

                    if !successMessage.isEmpty {
                        TeamMemberNoticeCard(message: successMessage, color: .green)
                    }

                    if isLoading {
                        ProgressView("Loading team members...")
                            .frame(maxWidth: .infinity)
                            .padding(24)
                            .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    } else if !errorMessage.isEmpty {
                        TeamMemberErrorCard(message: errorMessage) {
                            Task { await loadData() }
                        }
                    } else if members.isEmpty {
                        TeamMemberEmptyCard(canManageTeam: canManageTeam) {
                            editorDraft = TeamMemberEditorDraft()
                        }
                    } else {
                        ForEach(members) { member in
                            TeamMemberCard(
                                member: member,
                                canManage: canManageTeam,
                                onEdit: {
                                    editorDraft = TeamMemberEditorDraft(member: member)
                                },
                                onDelete: {
                                    memberToDelete = member
                                }
                            )
                        }
                    }
                }
                .padding(18)
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle("Team Members")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if canManageTeam {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editorDraft = TeamMemberEditorDraft()
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
            TeamMemberEditorView(
                draft: draft,
                geos: geos,
                joinedUsers: joinedUsers,
                existingMembers: members,
                onCancel: {
                    editorDraft = nil
                },
                onSave: { nextDraft in
                    await save(nextDraft)
                }
            )
        }
        .alert("Delete team member?", isPresented: deleteAlertBinding) {
            Button("Delete", role: .destructive) {
                guard let memberToDelete else {
                    return
                }
                Task { await delete(memberToDelete) }
            }
            Button("Cancel", role: .cancel) {
                memberToDelete = nil
            }
        } message: {
            Text("This removes roster routing, schedules, leaves, and breaks. If a linked InciTeam account exists, that account will also be deleted.")
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "person.2.fill")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(FeatureTone.slate.color.gradient, in: RoundedRectangle(cornerRadius: 17, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text("Team Members")
                    .font(.title.weight(.bold))
                    .foregroundStyle(InciTeamTheme.primaryDeep)
                Text("\(members.count) people in \(teamName)")
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
        Label("Read-only access. Team managers and admins can add, update, or remove members.", systemImage: "lock.fill")
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
            get: { memberToDelete != nil },
            set: { isPresented in
                if !isPresented {
                    memberToDelete = nil
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
            async let geoData = apiClient.fetchGeos(token: token)
            if canManageTeam {
                async let joinedUserData = apiClient.fetchJoinedTeamUsers(token: token)
                joinedUsers = try await joinedUserData.sorted { $0.displayName < $1.displayName }
            } else {
                joinedUsers = []
            }
            members = try await memberData.sorted { $0.fullName < $1.fullName }
            geos = try await geoData.sorted { $0.name < $1.name }
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func save(_ draft: TeamMemberEditorDraft) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        errorMessage = ""
        successMessage = ""
        do {
            let request = draft.request
            if let editingId = draft.editingId {
                _ = try await apiClient.updateTeamMember(id: editingId, token: token, request: request)
            } else {
                _ = try await apiClient.createTeamMember(token: token, request: request)
            }
            editorDraft = nil
            await loadData()
            successMessage = draft.editingId == nil ? "Team member added." : "Team member updated."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func delete(_ member: TeamMemberSummary) async {
        guard let token = sessionStore.session?.token else {
            return
        }

        errorMessage = ""
        successMessage = ""
        do {
            let response = try await apiClient.deleteTeamMember(id: member.id, token: token)
            memberToDelete = nil
            await loadData()
            successMessage = response.message ?? "Team member deleted."
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct TeamMemberEditorDraft: Identifiable {
    let id = UUID()
    var editingId: Int64?
    var selectedJoinedUserId: Int64?
    var firstName = ""
    var lastName = ""
    var email = ""
    var phone = ""
    var serviceNowSysId = ""
    var geoId: Int64?

    init() {}

    init(member: TeamMemberSummary) {
        editingId = member.id
        firstName = member.firstName
        lastName = member.lastName
        email = member.email ?? ""
        phone = member.phone ?? ""
        serviceNowSysId = member.serviceNowSysId ?? ""
        geoId = member.geo?.id
    }

    var request: TeamMemberRequest {
        TeamMemberRequest(
            firstName: firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            lastName: lastName.trimmingCharacters(in: .whitespacesAndNewlines),
            email: email.trimmingCharacters(in: .whitespacesAndNewlines),
            phone: phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : phone.trimmingCharacters(in: .whitespacesAndNewlines),
            serviceNowSysId: serviceNowSysId.trimmingCharacters(in: .whitespacesAndNewlines),
            geoId: geoId ?? 0
        )
    }

    var isValid: Bool {
        !firstName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !lastName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !serviceNowSysId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && geoId != nil
    }
}

private struct TeamMemberEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(SessionStore.self) private var sessionStore
    @State var draft: TeamMemberEditorDraft
    @State private var serviceNowSearch: String
    @State private var serviceNowResults: [ServiceNowLookupResult] = []
    @State private var isServiceNowLookupLoading = false
    @State private var serviceNowLookupComplete = false
    @State private var serviceNowLookupError = ""
    @State private var selectedServiceNowLabel: String
    @State private var selectedServiceNowSearch: String

    let geos: [GeoSummary]
    let joinedUsers: [JoinedTeamUserSummary]
    let existingMembers: [TeamMemberSummary]
    let onCancel: () -> Void
    let onSave: (TeamMemberEditorDraft) async -> Void

    private let apiClient = InciTeamAPIClient()

    init(
        draft: TeamMemberEditorDraft,
        geos: [GeoSummary],
        joinedUsers: [JoinedTeamUserSummary],
        existingMembers: [TeamMemberSummary],
        onCancel: @escaping () -> Void,
        onSave: @escaping (TeamMemberEditorDraft) async -> Void
    ) {
        let initialSearch = draft.email.isEmpty
            ? [draft.firstName, draft.lastName].joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines)
            : draft.email
        let initialLabel = draft.serviceNowSysId.isEmpty
            ? ""
            : [draft.firstName, draft.lastName].joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines)

        self._draft = State(initialValue: draft)
        self._serviceNowSearch = State(initialValue: initialSearch)
        self._selectedServiceNowLabel = State(initialValue: initialLabel)
        self._selectedServiceNowSearch = State(initialValue: initialSearch)
        self.geos = geos
        self.joinedUsers = joinedUsers
        self.existingMembers = existingMembers
        self.onCancel = onCancel
        self.onSave = onSave
    }

    var body: some View {
        NavigationStack {
            Form {
                if draft.editingId == nil {
                    Section("Joined InciTeam User") {
                        Picker("User", selection: joinedUserSelection) {
                            Text("Optional").tag(Int64?.none)
                            ForEach(joinedUsers) { user in
                                Text(joinedUserLabel(user))
                                    .tag(Optional(user.id))
                            }
                        }
                    }
                }

                Section("Identity") {
                    TextField("First Name", text: $draft.firstName)
                        .textContentType(.givenName)
                    TextField("Last Name", text: $draft.lastName)
                        .textContentType(.familyName)
                    TextField("Email", text: emailBinding)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                    TextField("Phone", text: $draft.phone)
                        .keyboardType(.phonePad)
                }

                Section("Coverage") {
                    Picker("Geo", selection: geoSelection) {
                        Text("Select Geo").tag(Int64?.none)
                        ForEach(geos) { geo in
                            Text(geo.name).tag(Optional(geo.id))
                        }
                    }
                }

                Section("ServiceNow Link") {
                    TextField("Search by email, name, or ServiceNow username", text: serviceNowSearchBinding)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                    Text("Select the matching ServiceNow user. InciTeam keeps the ServiceNow link behind the scenes.")
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
                        Text("No matching ServiceNow user found. Try the user's email or ServiceNow username.")
                            .font(.caption)
                            .foregroundStyle(.orange)
                    }

                    ForEach(serviceNowResults) { result in
                        Button {
                            selectServiceNowUser(result)
                        } label: {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(result.primaryLabel)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(InciTeamTheme.ink)
                                Text(result.secondaryLabel)
                                    .font(.caption)
                                    .foregroundStyle(InciTeamTheme.muted)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }

                    if !selectedServiceNowLabel.isEmpty {
                        Label("Linked ServiceNow user: \(selectedServiceNowLabel)", systemImage: "link")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.green)
                    }
                }
                .task(id: serviceNowSearch) {
                    await searchServiceNowUsers(for: serviceNowSearch)
                }
            }
            .navigationTitle(draft.editingId == nil ? "Add Member" : "Edit Member")
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

    private var joinedUserSelection: Binding<Int64?> {
        Binding(
            get: { draft.selectedJoinedUserId },
            set: { userId in
                draft.selectedJoinedUserId = userId
                guard let userId, let user = joinedUsers.first(where: { $0.id == userId }) else {
                    return
                }
                draft.firstName = user.firstName ?? ""
                draft.lastName = user.lastName ?? ""
                draft.email = user.workEmail ?? ""
                serviceNowSearch = user.workEmail ?? user.displayName
                clearServiceNowSelection()
            }
        )
    }

    private var emailBinding: Binding<String> {
        Binding(
            get: { draft.email },
            set: { email in
                draft.email = email
                serviceNowSearch = email
                clearServiceNowSelection()
            }
        )
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

    private var geoSelection: Binding<Int64?> {
        Binding(
            get: { draft.geoId },
            set: { draft.geoId = $0 }
        )
    }

    @MainActor
    private func searchServiceNowUsers(for rawQuery: String) async {
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
            let results = try await apiClient.searchServiceNowUsers(token: token, query: query)
            try Task.checkCancellation()

            if let exactMatch = autoSelectableServiceNowUser(from: results) {
                selectServiceNowUser(exactMatch, preserveSearch: true)
                isServiceNowLookupLoading = false
                return
            }

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

    private func selectServiceNowUser(_ result: ServiceNowLookupResult, preserveSearch: Bool = false) {
        let label = result.primaryLabel
        let nextSearch = preserveSearch ? serviceNowSearch : (result.email ?? label)

        draft.serviceNowSysId = result.sysId
        selectedServiceNowLabel = label
        selectedServiceNowSearch = nextSearch
        serviceNowSearch = nextSearch

        if let email = result.email, !email.isEmpty {
            draft.email = email
        }

        let nameParts = label
            .split(separator: " ")
            .map(String.init)
        if draft.firstName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
           let firstName = nameParts.first {
            draft.firstName = firstName
        }
        if draft.lastName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
           nameParts.count > 1 {
            draft.lastName = nameParts.dropFirst().joined(separator: " ")
        }

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

    private func autoSelectableServiceNowUser(from results: [ServiceNowLookupResult]) -> ServiceNowLookupResult? {
        let email = draft.email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard draft.serviceNowSysId.isEmpty,
              !email.isEmpty,
              results.count == 1,
              let result = results.first,
              result.email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == email else {
            return nil
        }
        return result
    }

    private func joinedUserLabel(_ user: JoinedTeamUserSummary) -> String {
        let alreadyMapped = existingMembers.contains { member in
            member.email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                == user.workEmail?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        }
        let suffix = alreadyMapped ? " • already mapped" : ""
        return "\(user.displayName)\(user.workEmail.map { " (\($0))" } ?? "")\(suffix)"
    }
}

private struct TeamMemberCard: View {
    let member: TeamMemberSummary
    let canManage: Bool
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                Text(initials)
                    .font(.headline.weight(.heavy))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
                    .background(FeatureTone.slate.color.gradient, in: RoundedRectangle(cornerRadius: 15, style: .continuous))

                VStack(alignment: .leading, spacing: 4) {
                    Text(member.fullName.isEmpty ? "Unnamed member" : member.fullName)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(InciTeamTheme.ink)
                    Text(member.geo?.name ?? "No geo assigned")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(InciTeamTheme.primary)
                }

                Spacer()
            }

            VStack(alignment: .leading, spacing: 8) {
                if let email = member.email, !email.isEmpty {
                    Label(email, systemImage: "envelope.fill")
                }
                if let phone = member.phone, !phone.isEmpty {
                    Label(phone, systemImage: "phone.fill")
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

    private var initials: String {
        let initials = [member.firstName.first, member.lastName.first]
            .compactMap { $0 }
            .map(String.init)
            .joined()
        return initials.isEmpty ? "TM" : initials.uppercased()
    }
}

private struct TeamMemberEmptyCard: View {
    let canManageTeam: Bool
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.largeTitle)
                .foregroundStyle(InciTeamTheme.primary)
            Text("No team members yet")
                .font(.headline.weight(.bold))
                .foregroundStyle(InciTeamTheme.ink)
            Text("Team members connect InciTeam users to ServiceNow assignee records.")
                .font(.subheadline)
                .foregroundStyle(InciTeamTheme.muted)
                .multilineTextAlignment(.center)
            if canManageTeam {
                Button("Add Member", systemImage: "plus", action: onAdd)
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(InciTeamTheme.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct TeamMemberNoticeCard: View {
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

private struct TeamMemberErrorCard: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Could not load people", systemImage: "exclamationmark.triangle.fill")
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
        TeamMembersView()
    }
    .environment(SessionStore.previewSignedIn)
}
