import Foundation

struct InciTeamAPIClient {
    private let baseURL: URL
    private let urlSession: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    init(baseURL: URL = InciTeamAPIClient.productionBaseURL(), urlSession: URLSession = .shared) {
        self.baseURL = baseURL
        self.urlSession = urlSession
    }

    func signIn(username: String, password: String) async throws -> SignInResponse {
        var request = URLRequest(url: baseURL.appendingPathComponent("auth").appendingPathComponent("login"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try encoder.encode(SignInRequest(username: username, password: password))

        let (data, response) = try await urlSession.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw InciTeamAPIError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            throw InciTeamAPIError.server(
                statusCode: httpResponse.statusCode,
                message: errorMessage(from: data)
            )
        }

        do {
            return try decoder.decode(SignInResponse.self, from: data)
        } catch {
            throw InciTeamAPIError.decodingFailed
        }
    }

    func fetchTeamMembers(token: String) async throws -> [TeamMemberSummary] {
        try await get("team-members", token: token)
    }

    func fetchJoinedTeamUsers(token: String) async throws -> [JoinedTeamUserSummary] {
        try await get("team-members/joined-users", token: token)
    }

    func createTeamMember(token: String, request payload: TeamMemberRequest) async throws -> TeamMemberSummary {
        try await send("team-members", method: "POST", token: token, body: payload)
    }

    func updateTeamMember(id: Int64, token: String, request payload: TeamMemberRequest) async throws -> TeamMemberSummary {
        try await send("team-members/\(id)", method: "PUT", token: token, body: payload)
    }

    func deleteTeamMember(id: Int64, token: String) async throws -> AccountDeletionResponse {
        try await deleteReturning("team-members/\(id)", token: token)
    }

    func searchServiceNowUsers(token: String, query: String) async throws -> [ServiceNowLookupResult] {
        try await get("servicenow/lookup/users", token: token, queryItems: [
            URLQueryItem(name: "query", value: query)
        ])
    }

    func fetchGeos(token: String) async throws -> [GeoSummary] {
        try await get("geos", token: token)
    }

    func fetchShifts(token: String) async throws -> [ShiftSummary] {
        try await get("shifts", token: token)
    }

    func fetchGeoShiftMappings(token: String) async throws -> [GeoShiftMappingSummary] {
        try await get("geo-shift-mappings", token: token)
    }

    func fetchSchedules(token: String) async throws -> [TeamMemberScheduleSummary] {
        try await get("team-member-schedules", token: token)
    }

    func fetchAvailability(token: String, startDate: String, days: Int) async throws -> [AvailabilityRecord] {
        try await get("schedule/next", token: token, queryItems: [
            URLQueryItem(name: "startDate", value: startDate),
            URLQueryItem(name: "days", value: "\(days)")
        ])
    }

    func fetchLeaves(token: String, startDate: String, days: Int) async throws -> [LeaveRecordSummary] {
        try await get("leave/next", token: token, queryItems: [
            URLQueryItem(name: "startDate", value: startDate),
            URLQueryItem(name: "days", value: "\(days)")
        ])
    }

    func fetchBreaks(token: String, startDate: String, days: Int) async throws -> [BreakRecordSummary] {
        try await get("break/next", token: token, queryItems: [
            URLQueryItem(name: "startDate", value: startDate),
            URLQueryItem(name: "days", value: "\(days)")
        ])
    }

    func fetchLeaveEntries(token: String) async throws -> [AvailabilityEntrySummary] {
        try await get("leaves", token: token)
    }

    func createLeave(token: String, request payload: AvailabilityEntryRequest) async throws -> AvailabilityEntrySummary {
        try await send("leaves", method: "POST", token: token, body: payload)
    }

    func updateLeave(id: Int64, token: String, request payload: AvailabilityEntryRequest) async throws -> AvailabilityEntrySummary {
        try await send("leaves/\(id)", method: "PUT", token: token, body: payload)
    }

    func deleteLeave(id: Int64, token: String) async throws {
        try await delete("leaves/\(id)", token: token)
    }

    func fetchBreakEntries(token: String) async throws -> [AvailabilityEntrySummary] {
        try await get("breaks", token: token)
    }

    func createBreak(token: String, request payload: AvailabilityEntryRequest) async throws -> AvailabilityEntrySummary {
        try await send("breaks", method: "POST", token: token, body: payload)
    }

    func updateBreak(id: Int64, token: String, request payload: AvailabilityEntryRequest) async throws -> AvailabilityEntrySummary {
        try await send("breaks/\(id)", method: "PUT", token: token, body: payload)
    }

    func deleteBreak(id: Int64, token: String) async throws {
        try await delete("breaks/\(id)", token: token)
    }

    func fetchConfigurationItems(token: String) async throws -> [ConfigurationItemSummary] {
        try await get("configuration-items", token: token)
    }

    func createConfigurationItem(token: String, request payload: ConfigurationItemRequest) async throws -> ConfigurationItemSummary {
        try await send("configuration-items", method: "POST", token: token, body: payload)
    }

    func updateConfigurationItem(id: Int64, token: String, request payload: ConfigurationItemRequest) async throws -> ConfigurationItemSummary {
        try await send("configuration-items/\(id)", method: "PUT", token: token, body: payload)
    }

    func deleteConfigurationItem(id: Int64, token: String) async throws {
        try await delete("configuration-items/\(id)", token: token)
    }

    func searchServiceNowConfigurationItems(token: String, query: String) async throws -> [ServiceNowLookupResult] {
        try await get("servicenow/lookup/configuration-items", token: token, queryItems: [
            URLQueryItem(name: "query", value: query)
        ])
    }

    func fetchCiUserMappings(token: String) async throws -> [CiUserMappingSummary] {
        try await get("ci-user-mappings", token: token)
    }

    func replaceCiUserMappingsForCi(token: String, request payload: CiUserMappingBulkRequest) async throws -> [CiUserMappingSummary] {
        try await send("ci-user-mappings/bulk", method: "POST", token: token, body: payload)
    }

    func fetchCoverageSummary(token: String, startDate: String? = nil, days: Int = 7) async throws -> CoverageSummaryResponse {
        var queryItems = [URLQueryItem(name: "days", value: "\(days)")]
        if let startDate {
            queryItems.append(URLQueryItem(name: "startDate", value: startDate))
        }
        return try await get("coverage/summary", token: token, queryItems: queryItems)
    }

    func fetchServiceNowHealth(token: String) async throws -> ServiceNowHealthResponse {
        try await get("servicenow/health", token: token)
    }

    func fetchServiceNowValidation(token: String) async throws -> ServiceNowValidationResponse {
        try await get("servicenow/validation", token: token)
    }

    func pollServiceNowNow(token: String) async throws -> ServiceNowPollNowResponse {
        try await sendWithoutBody("servicenow/poll-now", method: "POST", token: token)
    }

    func fetchLeaveHandoff(token: String) async throws -> LeaveHandoffResponse {
        try await get("servicenow/leave-handoff", token: token)
    }

    func fetchServiceNowLogs(token: String) async throws -> [ServiceNowRunLogSummary] {
        try await get("logs/servicenow", token: token)
    }

    func fetchAssignmentDiagnostics(token: String) async throws -> AssignmentDiagnosticsResponse {
        try await get("servicenow/assignment-diagnostics", token: token)
    }

    func fetchUsers(token: String) async throws -> [UserSummary] {
        try await get("users", token: token)
    }

    func fetchWorkspaceTeams(token: String) async throws -> [TeamSummary] {
        try await get("workspace/teams", token: token)
    }

    func updateUserRole(id: Int64, token: String, role: String) async throws -> UserSummary {
        try await send("users/\(id)/role", method: "PUT", token: token, body: UserRoleUpdateRequest(role: role))
    }

    func assignUserToTeam(userId: Int64, teamId: Int64, token: String) async throws -> UserSummary {
        try await send("users/\(userId)/teams", method: "POST", token: token, body: TeamMembershipUpdateRequest(teamId: teamId))
    }

    func removeUserFromTeam(userId: Int64, teamId: Int64, token: String) async throws -> UserSummary {
        try await deleteReturning("users/\(userId)/teams/\(teamId)", token: token)
    }

    func deleteCurrentAccount(token: String) async throws -> AccountDeletionResponse {
        try await deleteReturning("account", token: token)
    }

    func deleteUserAccount(userId: Int64, token: String) async throws -> AccountDeletionResponse {
        try await deleteReturning("users/\(userId)", token: token)
    }

    func updateUserTeamRole(userId: Int64, teamId: Int64, token: String, role: String) async throws -> UserSummary {
        try await send("users/\(userId)/teams/\(teamId)/role", method: "PUT", token: token, body: TeamMembershipRoleUpdateRequest(role: role))
    }

    func registerMobileDeviceToken(_ deviceToken: String, authToken: String, environment: String) async throws {
        try await sendIgnoringResponse(
            "mobile/device-token",
            method: "POST",
            token: authToken,
            body: MobileDeviceTokenRequest(
                deviceToken: deviceToken,
                platform: "ios",
                environment: environment
            )
        )
    }

    func unregisterMobileDeviceToken(_ deviceToken: String, authToken: String, environment: String) async throws {
        try await sendIgnoringResponse(
            "mobile/device-token/unregister",
            method: "POST",
            token: authToken,
            body: MobileDeviceTokenRequest(
                deviceToken: deviceToken,
                platform: "ios",
                environment: environment
            )
        )
    }

    func createSchedule(token: String, request payload: TeamMemberScheduleRequest) async throws {
        try await sendIgnoringResponse("team-member-schedules", method: "POST", token: token, body: payload)
    }

    func updateSchedule(id: Int64, token: String, request payload: TeamMemberScheduleRequest) async throws {
        try await sendIgnoringResponse("team-member-schedules/\(id)", method: "PUT", token: token, body: payload)
    }

    func deleteSchedule(id: Int64, token: String) async throws {
        try await delete("team-member-schedules/\(id)", token: token)
    }

    private func errorMessage(from data: Data) -> String {
        if let decodedMessage = try? decoder.decode(String.self, from: data) {
            return decodedMessage
        }
        if let plainMessage = String(data: data, encoding: .utf8), !plainMessage.isEmpty {
            return plainMessage
        }
        return "The server returned an error."
    }

    private func get<T: Decodable>(
        _ path: String,
        token: String,
        queryItems: [URLQueryItem] = []
    ) async throws -> T {
        var request = URLRequest(url: url(for: path, queryItems: queryItems))
        request.httpMethod = "GET"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await urlSession.data(for: request)
        try validate(data: data, response: response, token: token)

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw InciTeamAPIError.decodingFailed
        }
    }

    private func send<T: Decodable, Body: Encodable>(
        _ path: String,
        method: String,
        token: String,
        body: Body
    ) async throws -> T {
        var request = URLRequest(url: url(for: path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.httpBody = try encoder.encode(body)

        let (data, response) = try await urlSession.data(for: request)
        try validate(data: data, response: response, token: token)

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw InciTeamAPIError.decodingFailed
        }
    }

    private func sendIgnoringResponse<Body: Encodable>(
        _ path: String,
        method: String,
        token: String,
        body: Body
    ) async throws {
        var request = URLRequest(url: url(for: path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.httpBody = try encoder.encode(body)

        let (data, response) = try await urlSession.data(for: request)
        try validate(data: data, response: response, token: token)
    }

    private func sendWithoutBody<T: Decodable>(
        _ path: String,
        method: String,
        token: String
    ) async throws -> T {
        var request = URLRequest(url: url(for: path))
        request.httpMethod = method
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await urlSession.data(for: request)
        try validate(data: data, response: response, token: token)

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw InciTeamAPIError.decodingFailed
        }
    }

    private func delete(_ path: String, token: String) async throws {
        var request = URLRequest(url: url(for: path))
        request.httpMethod = "DELETE"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await urlSession.data(for: request)
        try validate(data: data, response: response, token: token)
    }

    private func deleteReturning<T: Decodable>(_ path: String, token: String) async throws -> T {
        var request = URLRequest(url: url(for: path))
        request.httpMethod = "DELETE"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await urlSession.data(for: request)
        try validate(data: data, response: response, token: token)

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw InciTeamAPIError.decodingFailed
        }
    }

    private func validate(data: Data, response: URLResponse, token: String? = nil) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw InciTeamAPIError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            postAuthenticationExpiredIfNeeded(statusCode: httpResponse.statusCode, token: token)
            throw InciTeamAPIError.server(
                statusCode: httpResponse.statusCode,
                message: errorMessage(from: data)
            )
        }
    }

    private func postAuthenticationExpiredIfNeeded(statusCode: Int, token: String?) {
        guard (statusCode == 401 || statusCode == 403),
              let token,
              let expirationDate = JWT.expirationDate(from: token),
              expirationDate <= Date() else {
            return
        }
        NotificationCenter.default.post(name: .inciTeamAuthenticationExpired, object: nil)
    }

    private func url(for path: String, queryItems: [URLQueryItem] = []) -> URL {
        var url = baseURL
        for component in path.split(separator: "/") {
            url.appendPathComponent(String(component))
        }

        guard !queryItems.isEmpty else {
            return url
        }

        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        components?.queryItems = queryItems
        return components?.url ?? url
    }

    private static func productionBaseURL() -> URL {
        guard let url = URL(string: "https://www.inciteam.com/api") else {
            preconditionFailure("Invalid InciTeam production API URL.")
        }
        return url
    }
}

extension Notification.Name {
    static let inciTeamAuthenticationExpired = Notification.Name("inciTeamAuthenticationExpired")
}

private struct EmptyResponse: Decodable {}

enum InciTeamAPIError: LocalizedError {
    case invalidResponse
    case server(statusCode: Int, message: String)
    case decodingFailed

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "The server response was not valid."
        case let .server(statusCode, message):
            return "\(message) (\(statusCode))"
        case .decodingFailed:
            return "The sign-in response could not be read."
        }
    }
}
