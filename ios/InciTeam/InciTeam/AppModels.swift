import Foundation

struct SignInRequest: Encodable {
    let username: String
    let password: String
}

struct SignInResponse: Decodable {
    let token: String
    let userId: Int64
    let username: String
    let firstName: String?
    let lastName: String?
    let workEmail: String?
    let role: String
    let workspace: WorkspaceSummary?

    enum CodingKeys: String, CodingKey {
        case token
        case userId = "u_id"
        case username
        case firstName
        case lastName
        case workEmail
        case role
        case workspace
    }

    init(
        token: String,
        userId: Int64,
        username: String,
        firstName: String? = nil,
        lastName: String? = nil,
        workEmail: String?,
        role: String,
        workspace: WorkspaceSummary?
    ) {
        self.token = token
        self.userId = userId
        self.username = username
        self.firstName = firstName
        self.lastName = lastName
        self.workEmail = workEmail
        self.role = role
        self.workspace = workspace
    }
}

struct WorkspaceSummary: Codable, Equatable {
    let organizationId: Int64?
    let organizationName: String?
    let teamId: Int64?
    let teamName: String?
    let teamRole: String?
    let teamTimezone: String?
}

struct AuthenticatedUser: Codable, Equatable {
    let id: Int64
    let username: String
    let firstName: String?
    let lastName: String?
    let workEmail: String?
    let role: String
    let workspace: WorkspaceSummary?

    init(response: SignInResponse) {
        self.id = response.userId
        self.username = response.username
        self.firstName = response.firstName
        self.lastName = response.lastName
        self.workEmail = response.workEmail
        self.role = response.role
        self.workspace = response.workspace
    }

    var canManageCurrentTeam: Bool {
        if role.caseInsensitiveCompare("Admin") == .orderedSame {
            return true
        }

        let teamRole = workspace?.teamRole?.uppercased()
        return teamRole == "TEAM_ADMIN" || teamRole == "MANAGER"
    }

    var isGlobalAdmin: Bool {
        role.caseInsensitiveCompare("Admin") == .orderedSame
    }
}

struct StoredSession: Codable, Equatable {
    let token: String
    let user: AuthenticatedUser

    var isExpired: Bool {
        guard let expirationDate = JWT.expirationDate(from: token) else {
            return false
        }
        return expirationDate <= Date().addingTimeInterval(30)
    }
}

struct TeamMemberSummary: Decodable, Identifiable {
    let id: Int64
    let firstName: String
    let lastName: String
    let email: String?
    let phone: String?
    let serviceNowSysId: String?
    let geo: GeoSummary?

    var fullName: String {
        "\(firstName) \(lastName)"
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["tm_id", "tmId", "id"])
        firstName = container.decodeStringIfPresent(forKeys: ["f_name", "fName", "firstName"]) ?? ""
        lastName = container.decodeStringIfPresent(forKeys: ["l_name", "lName", "lastName"]) ?? ""
        email = container.decodeStringIfPresent(forKeys: ["email"])
        phone = container.decodeStringIfPresent(forKeys: ["phone"])
        serviceNowSysId = container.decodeStringIfPresent(forKeys: ["sys_id", "sysId", "serviceNowUserSysId"])
        geo = container.decodeValueIfPresent(GeoSummary.self, forKeys: ["geo"])
    }
}

struct TeamMemberRequest: Encodable {
    let firstName: String
    let lastName: String
    let email: String
    let phone: String?
    let serviceNowSysId: String
    let geoId: Int64

    enum CodingKeys: String, CodingKey {
        case firstName = "f_name"
        case lastName = "l_name"
        case email
        case phone
        case serviceNowSysId = "sys_id"
        case geoId
    }
}

struct ServiceNowLookupResult: Decodable, Identifiable, Hashable {
    let sysId: String
    let displayName: String?
    let email: String?
    let userName: String?
    let detail: String?
    let secondaryDetail: String?

    var id: String { sysId }

    var primaryLabel: String {
        if let displayName = displayName?.trimmingCharacters(in: .whitespacesAndNewlines), !displayName.isEmpty {
            return displayName
        }
        if let email = email?.trimmingCharacters(in: .whitespacesAndNewlines), !email.isEmpty {
            return email
        }
        if let userName = userName?.trimmingCharacters(in: .whitespacesAndNewlines), !userName.isEmpty {
            return userName
        }
        return "ServiceNow user"
    }

    var secondaryLabel: String {
        let values = [email, userName]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return values.isEmpty ? (detail ?? secondaryDetail ?? "ServiceNow user") : values.joined(separator: " / ")
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        sysId = container.decodeStringIfPresent(forKeys: ["sysId", "sys_id"]) ?? ""
        displayName = container.decodeStringIfPresent(forKeys: ["displayName", "display_name"])
        email = container.decodeStringIfPresent(forKeys: ["email"])
        userName = container.decodeStringIfPresent(forKeys: ["userName", "user_name"])
        detail = container.decodeStringIfPresent(forKeys: ["detail"])
        secondaryDetail = container.decodeStringIfPresent(forKeys: ["secondaryDetail", "secondary_detail"])
    }
}

struct JoinedTeamUserSummary: Decodable, Identifiable {
    let id: Int64
    let username: String
    let firstName: String?
    let lastName: String?
    let workEmail: String?

    var displayName: String {
        let name = [firstName, lastName]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        return name.isEmpty ? username : name
    }
}

struct GeoSummary: Decodable, Identifiable, Hashable {
    let id: Int64
    let name: String

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["g_id", "gId", "id"])
        name = container.decodeStringIfPresent(forKeys: ["name"]) ?? "Geo \(id)"
    }
}

struct ShiftSummary: Decodable, Identifiable, Hashable {
    let id: Int64
    let name: String
    let startTime: String?
    let endTime: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["s_id", "sId", "id"])
        name = container.decodeStringIfPresent(forKeys: ["name"]) ?? "Shift \(id)"
        startTime = container.decodeStringIfPresent(forKeys: ["startTime", "start_time"])
        endTime = container.decodeStringIfPresent(forKeys: ["endTime", "end_time"])
    }
}

struct GeoShiftMappingSummary: Decodable, Identifiable {
    let id: Int64
    let geo: GeoSummary?
    let shift: ShiftSummary?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["gsm_id", "gsmId", "id"])
        geo = container.decodeValueIfPresent(GeoSummary.self, forKeys: ["geo"])
        shift = container.decodeValueIfPresent(ShiftSummary.self, forKeys: ["shift"])
    }
}

struct TeamMemberScheduleSummary: Decodable, Identifiable {
    let id: Int64
    let teamMember: TeamMemberSummary?
    let geo: GeoSummary?
    let shift: ShiftSummary?
    let startDate: String
    let endDate: String
    let coverageDays: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["tms_id", "tmsId", "id"])
        teamMember = container.decodeValueIfPresent(TeamMemberSummary.self, forKeys: ["teamMember"])
        geo = container.decodeValueIfPresent(GeoSummary.self, forKeys: ["geo"])
        shift = container.decodeValueIfPresent(ShiftSummary.self, forKeys: ["shift"])
        startDate = container.decodeStringIfPresent(forKeys: ["startDate", "start_date"]) ?? ""
        endDate = container.decodeStringIfPresent(forKeys: ["endDate", "end_date"]) ?? ""
        coverageDays = container.decodeStringIfPresent(forKeys: ["coverageDays", "coverage_days"])
    }
}

struct TeamMemberScheduleRequest: Encodable {
    let teamMemberId: Int64?
    let teamMemberIds: [Int64]?
    let geoId: Int64
    let shiftId: Int64
    let startDate: String
    let endDate: String
    let coverageDays: [String]
}

struct AvailabilityRecord: Decodable, Identifiable {
    let id = UUID()
    let tmId: Int64?
    let geoName: String
    let shiftName: String
    let date: String
    let fullName: String

    enum CodingKeys: String, CodingKey {
        case tmId
        case geoName
        case shiftName
        case date
        case fullName
    }
}

struct LeaveRecordSummary: Decodable, Identifiable {
    let id = UUID()
    let tmId: Int64?
    let fullName: String
    let geoName: String?
    let shiftName: String?
    let startTs: String
    let endTs: String
    let reason: String?

    enum CodingKeys: String, CodingKey {
        case tmId
        case fullName
        case geoName
        case shiftName
        case startTs
        case endTs
        case reason
    }
}

struct BreakRecordSummary: Decodable, Identifiable {
    let id = UUID()
    let tmId: Int64?
    let fullName: String
    let geoName: String?
    let shiftName: String?
    let startTs: String
    let endTs: String
    let reason: String?

    enum CodingKeys: String, CodingKey {
        case tmId
        case fullName
        case geoName
        case shiftName
        case startTs
        case endTs
        case reason
    }
}

struct AvailabilityEntrySummary: Decodable, Identifiable {
    let id: Int64
    let teamMember: TeamMemberSummary?
    let startTs: String
    let endTs: String
    let reason: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["leave_id", "break_id", "id"])
        teamMember = container.decodeValueIfPresent(TeamMemberSummary.self, forKeys: ["teamMember"])
        startTs = container.decodeStringIfPresent(forKeys: ["startTs", "start_ts"]) ?? ""
        endTs = container.decodeStringIfPresent(forKeys: ["endTs", "end_ts"]) ?? ""
        reason = container.decodeStringIfPresent(forKeys: ["reason"])
    }
}

struct AvailabilityEntryRequest: Encodable {
    let teamMemberId: Int64
    let startTs: String
    let endTs: String
    let reason: String?
}

struct ConfigurationItemSummary: Decodable, Identifiable, Hashable {
    let id: Int64
    let name: String
    let description: String?
    let serviceNowSysId: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["ci_id", "ciId", "id"])
        name = container.decodeStringIfPresent(forKeys: ["name"]) ?? "CI \(id)"
        description = container.decodeStringIfPresent(forKeys: ["description"])
        serviceNowSysId = container.decodeStringIfPresent(forKeys: ["serviceNowSysId", "service_now_sys_id"])
    }
}

struct ConfigurationItemRequest: Encodable {
    let name: String
    let description: String?
    let serviceNowSysId: String
}

struct CiUserMappingSummary: Decodable, Identifiable {
    let id: Int64
    let configurationItem: ConfigurationItemSummary?
    let teamMember: TeamMemberSummary?
    let sortOrder: Int?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        id = try container.decodeInt64(forKeys: ["mapping_id", "mappingId", "id"])
        configurationItem = container.decodeValueIfPresent(ConfigurationItemSummary.self, forKeys: ["configurationItem"])
        teamMember = container.decodeValueIfPresent(TeamMemberSummary.self, forKeys: ["teamMember"])
        sortOrder = container.decodeIntIfPresent(forKeys: ["sortOrder", "sort_order"])
    }
}

struct CiUserMappingBulkRequest: Encodable {
    let configurationItemId: Int64
    let teamMemberIds: [Int64]
}

struct CoverageSummaryResponse: Decodable {
    let checkedAt: String?
    let startDate: String?
    let endDate: String?
    let totalGeoShiftDays: Int
    let coveredGeoShiftDays: Int
    let gapCount: Int
    let ciRiskCount: Int
    let issues: [CoverageIssueSummary]
}

struct CoverageIssueSummary: Decodable, Identifiable {
    let id = UUID()
    let type: String?
    let severity: String?
    let message: String?
    let date: String?
    let geo: String?
    let shift: String?
    let configurationItem: String?

    enum CodingKeys: String, CodingKey {
        case type
        case severity
        case message
        case date
        case geo
        case shift
        case configurationItem
    }
}

struct ServiceNowHealthResponse: Decodable {
    let checkedAt: String?
    let healthy: Bool
    let status: String?
    let message: String?
    let instanceUrl: String?
    let lastPollAt: String?
    let lastPollStatus: String?
    let lastPollMessage: String?
}

struct ServiceNowValidationResponse: Decodable {
    let checkedAt: String?
    let valid: Bool
    let message: String?
    let configurationItemCount: Int
    let validConfigurationItemCount: Int
    let teamMemberCount: Int
    let validTeamMemberCount: Int
    let issues: [ServiceNowValidationIssue]
}

struct ServiceNowValidationIssue: Decodable, Identifiable {
    let id = UUID()
    let type: String?
    let localName: String?
    let localSysId: String?
    let message: String?

    enum CodingKeys: String, CodingKey {
        case type
        case localName
        case localSysId
        case message
    }
}

struct ServiceNowPollNowResponse: Decodable {
    let polledAt: String?
    let status: String?
    let message: String?
    let incidentCount: Int
    let successCount: Int
    let failedCount: Int
    let skippedCount: Int
}

struct ServiceNowRunLogSummary: Decodable, Identifiable {
    let id = UUID()
    let timestamp: String?
    let teamId: Int64?
    let teamName: String?
    let type: String?
    let status: String?
    let message: String?
    let incidentCount: Int
    let incidents: [ServiceNowIncidentSummary]
    let assignmentSelections: [ServiceNowAssignmentSelection]
    let assignmentResults: [ServiceNowAssignmentResult]
    let assignmentConfirmation: String?

    enum CodingKeys: String, CodingKey {
        case timestamp
        case teamId
        case teamName
        case type
        case status
        case message
        case incidentCount
        case incidents
        case assignmentSelections
        case assignmentResults
        case assignmentConfirmation
    }
}

struct ServiceNowIncidentSummary: Decodable, Identifiable {
    let id = UUID()
    let number: String?
    let createdOn: String?
    let configurationItem: String?
    let assignmentGroup: String?
    let priority: String?
    let caller: String?
    let shortDescription: String?
    let suggestedAssignee: String?
    let suggestedAssigneeEmail: String?
    let suggestedGeo: String?
    let suggestedShift: String?

    enum CodingKeys: String, CodingKey {
        case number
        case createdOn
        case configurationItem
        case assignmentGroup
        case priority
        case caller
        case shortDescription
        case suggestedAssignee
        case suggestedAssigneeEmail
        case suggestedGeo
        case suggestedShift
    }
}

struct ServiceNowAssignmentSelection: Decodable, Identifiable {
    let id = UUID()
    let incidentNumber: String?
    let assigneeName: String?
    let assigneeEmail: String?
    let geo: String?
    let shift: String?

    enum CodingKeys: String, CodingKey {
        case incidentNumber
        case assigneeName
        case assigneeEmail
        case geo
        case shift
    }
}

struct ServiceNowAssignmentResult: Decodable, Identifiable {
    let id = UUID()
    let incidentNumber: String?
    let assigneeName: String?
    let assigneeEmail: String?
    let geo: String?
    let shift: String?
    let status: String?
    let message: String?

    enum CodingKeys: String, CodingKey {
        case incidentNumber
        case assigneeName
        case assigneeEmail
        case geo
        case shift
        case status
        case message
    }
}

struct LeaveHandoffResponse: Decodable {
    let checkedAt: String?
    let impactedMemberCount: Int
    let activeIncidentCount: Int
    let items: [LeaveHandoffItem]
}

struct LeaveHandoffItem: Decodable, Identifiable {
    let id = UUID()
    let teamMemberName: String?
    let email: String?
    let leaveStart: String?
    let leaveEnd: String?
    let reason: String?
    let incidents: [LeaveHandoffIncident]

    enum CodingKeys: String, CodingKey {
        case teamMemberName
        case email
        case leaveStart
        case leaveEnd
        case reason
        case incidents
    }
}

struct LeaveHandoffIncident: Decodable, Identifiable {
    let id = UUID()
    let number: String?
    let priority: String?
    let configurationItem: String?
    let shortDescription: String?

    enum CodingKeys: String, CodingKey {
        case number
        case priority
        case configurationItem
        case shortDescription
    }
}

struct AssignmentDiagnosticsResponse: Decodable {
    let checkedAt: String?
    let incidentCount: Int
    let assignableCount: Int
    let skippedCount: Int
    let incidents: [AssignmentDiagnosticItem]
}

struct AssignmentDiagnosticItem: Decodable, Identifiable {
    let id = UUID()
    let incidentNumber: String?
    let incidentSysId: String?
    let caller: String?
    let configurationItem: String?
    let priority: String?
    let createdOn: String?
    let shortDescription: String?
    let status: String?
    let reason: String?
    let suggestion: IncidentAssignmentSuggestion?
    let candidateChecks: [AssignmentCandidateCheck]

    enum CodingKeys: String, CodingKey {
        case incidentNumber
        case incidentSysId
        case caller
        case configurationItem
        case priority
        case createdOn
        case shortDescription
        case status
        case reason
        case suggestion
        case candidateChecks
    }
}

struct IncidentAssignmentSuggestion: Decodable {
    let assigneeName: String?
    let assigneeEmail: String?
    let assigneeSysId: String?
    let assigneePhone: String?
    let geo: String?
    let shift: String?
    let routingNote: String?
    let routedTeamName: String?
}

struct AssignmentCandidateCheck: Decodable, Identifiable {
    let id = UUID()
    let teamMemberName: String?
    let email: String?
    let serviceNowUserSysId: String?
    let sortOrder: Int?
    let memberGeo: String?
    let activeSchedules: String?
    let matchStatus: String?
    let onLeave: Bool
    let onBreak: Bool
    let eligible: Bool
    let selected: Bool
    let reason: String?

    enum CodingKeys: String, CodingKey {
        case teamMemberName
        case email
        case serviceNowUserSysId
        case sortOrder
        case memberGeo
        case activeSchedules
        case matchStatus
        case onLeave
        case onBreak
        case eligible
        case selected
        case reason
    }
}

struct TeamSummary: Decodable, Identifiable, Hashable {
    let id: Int64
    let teamName: String
    let description: String?
    let joinCode: String?
    let current: Bool

    enum CodingKeys: String, CodingKey {
        case id = "teamId"
        case teamName
        case description
        case joinCode
        case current
    }
}

struct UserSummary: Decodable, Identifiable {
    let id: Int64
    let username: String
    let firstName: String?
    let lastName: String?
    let workEmail: String?
    let role: String
    let currentTeamId: Int64?
    let currentTeamName: String?
    let teamMemberships: [UserTeamMembershipSummary]

    var displayName: String {
        let name = [firstName, lastName]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        return name.isEmpty ? username : name
    }
}

struct UserTeamMembershipSummary: Decodable, Identifiable {
    let teamId: Int64
    let teamName: String
    let role: String
    let current: Bool

    var id: Int64 { teamId }
}

struct UserRoleUpdateRequest: Encodable {
    let role: String
}

struct TeamMembershipUpdateRequest: Encodable {
    let teamId: Int64
}

struct TeamMembershipRoleUpdateRequest: Encodable {
    let role: String
}

struct MobileDeviceTokenRequest: Encodable {
    let deviceToken: String
    let platform: String
    let environment: String
}

struct AccountDeletionResponse: Decodable {
    let deletedUserId: Int64?
    let deletedUsername: String?
    let userDeleted: Bool
    let teamMemberRecordsDeleted: Int
    let teamMembershipsDeleted: Int
    let organizationMembershipsDeleted: Int
    let mobileDeviceTokensDeleted: Int
    let message: String?
}

struct IncidentNotificationDetail: Identifiable, Equatable {
    let id = UUID()
    let incidentNumber: String
    let title: String
    let priority: String

    init?(userInfo: [AnyHashable: Any]) {
        guard let incidentNumber = IncidentNotificationDetail.stringValue(for: "incidentNumber", in: userInfo),
              !incidentNumber.isEmpty else {
            return nil
        }

        self.incidentNumber = incidentNumber
        self.title = IncidentNotificationDetail.stringValue(for: "title", in: userInfo) ?? "Incident assigned to you"
        self.priority = IncidentNotificationDetail.stringValue(for: "priority", in: userInfo) ?? "Not provided"
    }

    private static func stringValue(for key: String, in userInfo: [AnyHashable: Any]) -> String? {
        guard let value = userInfo[key] else {
            return nil
        }
        if let string = value as? String {
            return string.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return "\(value)".trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

private struct DynamicCodingKey: CodingKey {
    let stringValue: String
    let intValue: Int?

    init?(stringValue: String) {
        self.stringValue = stringValue
        self.intValue = nil
    }

    init?(intValue: Int) {
        self.stringValue = "\(intValue)"
        self.intValue = intValue
    }
}

private extension KeyedDecodingContainer where Key == DynamicCodingKey {
    func decodeStringIfPresent(forKeys keys: [String]) -> String? {
        for key in keys {
            guard let codingKey = DynamicCodingKey(stringValue: key) else {
                continue
            }
            if let value = try? decodeIfPresent(String.self, forKey: codingKey) {
                return value
            }
        }
        return nil
    }

    func decodeValueIfPresent<T: Decodable>(_ type: T.Type, forKeys keys: [String]) -> T? {
        for key in keys {
            guard let codingKey = DynamicCodingKey(stringValue: key) else {
                continue
            }
            if let value = try? decodeIfPresent(type, forKey: codingKey) {
                return value
            }
        }
        return nil
    }

    func decodeIntIfPresent(forKeys keys: [String]) -> Int? {
        for key in keys {
            guard let codingKey = DynamicCodingKey(stringValue: key) else {
                continue
            }
            if let value = try? decodeIfPresent(Int.self, forKey: codingKey) {
                return value
            }
            if let value = try? decodeIfPresent(Int64.self, forKey: codingKey) {
                return Int(value)
            }
        }
        return nil
    }

    func decodeInt64(forKeys keys: [String]) throws -> Int64 {
        for key in keys {
            guard let codingKey = DynamicCodingKey(stringValue: key) else {
                continue
            }
            if let value = try? decode(Int64.self, forKey: codingKey) {
                return value
            }
            if let value = try? decode(Int.self, forKey: codingKey) {
                return Int64(value)
            }
        }
        throw DecodingError.keyNotFound(
            DynamicCodingKey(stringValue: keys.first ?? "id")!,
            DecodingError.Context(codingPath: codingPath, debugDescription: "Missing team member id.")
        )
    }
}
