package com.inciteam.app.network

import com.inciteam.app.data.AccountDeletionResponse
import com.inciteam.app.data.ApiParsers
import com.inciteam.app.data.AssignmentDiagnosticsResponse
import com.inciteam.app.data.AvailabilityEntrySummary
import com.inciteam.app.data.AvailabilityRecord
import com.inciteam.app.data.BreakRecordSummary
import com.inciteam.app.data.CiUserMappingSummary
import com.inciteam.app.data.ConfigurationItemSummary
import com.inciteam.app.data.CoverageSummaryResponse
import com.inciteam.app.data.GeoShiftMappingSummary
import com.inciteam.app.data.GeoSummary
import com.inciteam.app.data.JoinedTeamUserSummary
import com.inciteam.app.data.LeaveHandoffResponse
import com.inciteam.app.data.LeaveRecordSummary
import com.inciteam.app.data.ServiceNowHealthResponse
import com.inciteam.app.data.ServiceNowLookupResult
import com.inciteam.app.data.ServiceNowPollNowResponse
import com.inciteam.app.data.ServiceNowRunLogSummary
import com.inciteam.app.data.ServiceNowValidationResponse
import com.inciteam.app.data.ShiftSummary
import com.inciteam.app.data.SignInResponse
import com.inciteam.app.data.TeamMemberScheduleSummary
import com.inciteam.app.data.TeamMemberSummary
import com.inciteam.app.data.TeamSummary
import com.inciteam.app.data.UserSummary
import com.inciteam.app.data.WorkspaceSummary
import com.inciteam.app.data.jsonObject
import com.inciteam.app.data.mapObjects
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class InciTeamApiClient(
    private val baseUrl: String = "https://www.inciteam.com/api"
) {
    suspend fun signIn(username: String, password: String): SignInResponse = withContext(Dispatchers.IO) {
        val connection = URL("$baseUrl/auth/login").openConnection() as HttpURLConnection
        val requestBody = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toByteArray(Charsets.UTF_8)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { output ->
                output.write(requestBody)
            }

            val statusCode = connection.responseCode
            val responseText = connection.readResponseText(statusCode)
            if (statusCode !in 200..299) {
                throw InciTeamApiException(
                    message = responseText.ifBlank { "Sign in failed." },
                    statusCode = statusCode
                )
            }

            parseSignInResponse(responseText)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun fetchTeamMembers(token: String): List<TeamMemberSummary> =
        getArray("team-members", token).mapObjects(ApiParsers::teamMember)

    suspend fun fetchJoinedTeamUsers(token: String): List<JoinedTeamUserSummary> =
        getArray("team-members/joined-users", token).mapObjects(ApiParsers::joinedUser)

    suspend fun createTeamMember(
        token: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String?,
        serviceNowSysId: String,
        geoId: Long
    ): TeamMemberSummary = sendObject(
        path = "team-members",
        method = "POST",
        token = token,
        body = jsonObject(
            "f_name" to firstName,
            "l_name" to lastName,
            "email" to email,
            "phone" to phone,
            "sys_id" to serviceNowSysId,
            "geoId" to geoId
        ),
        parser = ApiParsers::teamMember
    )

    suspend fun updateTeamMember(
        id: Long,
        token: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String?,
        serviceNowSysId: String,
        geoId: Long
    ): TeamMemberSummary = sendObject(
        path = "team-members/$id",
        method = "PUT",
        token = token,
        body = jsonObject(
            "f_name" to firstName,
            "l_name" to lastName,
            "email" to email,
            "phone" to phone,
            "sys_id" to serviceNowSysId,
            "geoId" to geoId
        ),
        parser = ApiParsers::teamMember
    )

    suspend fun deleteTeamMember(id: Long, token: String): AccountDeletionResponse =
        deleteReturning("team-members/$id", token, ApiParsers::deletion)

    suspend fun searchServiceNowUsers(token: String, query: String): List<ServiceNowLookupResult> =
        getArray("servicenow/lookup/users", token, "query" to query).mapObjects(ApiParsers::lookupResult)

    suspend fun fetchGeos(token: String): List<GeoSummary> =
        getArray("geos", token).mapObjects(ApiParsers::geo)

    suspend fun fetchShifts(token: String): List<ShiftSummary> =
        getArray("shifts", token).mapObjects(ApiParsers::shift)

    suspend fun fetchGeoShiftMappings(token: String): List<GeoShiftMappingSummary> =
        getArray("geo-shift-mappings", token).mapObjects(ApiParsers::geoShiftMapping)

    suspend fun fetchSchedules(token: String): List<TeamMemberScheduleSummary> =
        getArray("team-member-schedules", token).mapObjects(ApiParsers::schedule)

    suspend fun createSchedule(
        token: String,
        teamMemberIds: List<Long>,
        geoId: Long,
        shiftId: Long,
        startDate: String,
        endDate: String,
        coverageDays: List<String>
    ) {
        sendIgnoringResponse(
            "team-member-schedules",
            "POST",
            token,
            jsonObject(
                "teamMemberId" to teamMemberIds.firstOrNull(),
                "teamMemberIds" to teamMemberIds,
                "geoId" to geoId,
                "shiftId" to shiftId,
                "startDate" to startDate,
                "endDate" to endDate,
                "coverageDays" to coverageDays
            )
        )
    }

    suspend fun updateSchedule(
        id: Long,
        token: String,
        teamMemberIds: List<Long>,
        geoId: Long,
        shiftId: Long,
        startDate: String,
        endDate: String,
        coverageDays: List<String>
    ) {
        sendIgnoringResponse(
            "team-member-schedules/$id",
            "PUT",
            token,
            jsonObject(
                "teamMemberId" to teamMemberIds.firstOrNull(),
                "teamMemberIds" to teamMemberIds,
                "geoId" to geoId,
                "shiftId" to shiftId,
                "startDate" to startDate,
                "endDate" to endDate,
                "coverageDays" to coverageDays
            )
        )
    }

    suspend fun deleteSchedule(id: Long, token: String) {
        delete("team-member-schedules/$id", token)
    }

    suspend fun fetchAvailability(token: String, startDate: String, days: Int): List<AvailabilityRecord> =
        getArray("schedule/next", token, "startDate" to startDate, "days" to days.toString())
            .mapObjects(ApiParsers::availability)

    suspend fun fetchLeaves(token: String, startDate: String, days: Int): List<LeaveRecordSummary> =
        getArray("leave/next", token, "startDate" to startDate, "days" to days.toString())
            .mapObjects(ApiParsers::leaveRecord)

    suspend fun fetchBreaks(token: String, startDate: String, days: Int): List<BreakRecordSummary> =
        getArray("break/next", token, "startDate" to startDate, "days" to days.toString())
            .mapObjects(ApiParsers::breakRecord)

    suspend fun fetchLeaveEntries(token: String): List<AvailabilityEntrySummary> =
        getArray("leaves", token).mapObjects(ApiParsers::availabilityEntry)

    suspend fun fetchBreakEntries(token: String): List<AvailabilityEntrySummary> =
        getArray("breaks", token).mapObjects(ApiParsers::availabilityEntry)

    suspend fun createAvailabilityEntry(
        kind: String,
        token: String,
        teamMemberId: Long,
        startTs: String,
        endTs: String,
        reason: String?
    ): AvailabilityEntrySummary = sendObject(
        path = kind,
        method = "POST",
        token = token,
        body = jsonObject(
            "teamMemberId" to teamMemberId,
            "startTs" to startTs,
            "endTs" to endTs,
            "reason" to reason
        ),
        parser = ApiParsers::availabilityEntry
    )

    suspend fun updateAvailabilityEntry(
        kind: String,
        id: Long,
        token: String,
        teamMemberId: Long,
        startTs: String,
        endTs: String,
        reason: String?
    ): AvailabilityEntrySummary = sendObject(
        path = "$kind/$id",
        method = "PUT",
        token = token,
        body = jsonObject(
            "teamMemberId" to teamMemberId,
            "startTs" to startTs,
            "endTs" to endTs,
            "reason" to reason
        ),
        parser = ApiParsers::availabilityEntry
    )

    suspend fun deleteAvailabilityEntry(kind: String, id: Long, token: String) {
        delete("$kind/$id", token)
    }

    suspend fun fetchConfigurationItems(token: String): List<ConfigurationItemSummary> =
        getArray("configuration-items", token).mapObjects(ApiParsers::configurationItem)

    suspend fun createConfigurationItem(
        token: String,
        name: String,
        description: String?,
        serviceNowSysId: String
    ): ConfigurationItemSummary = sendObject(
        path = "configuration-items",
        method = "POST",
        token = token,
        body = jsonObject(
            "name" to name,
            "description" to description,
            "serviceNowSysId" to serviceNowSysId
        ),
        parser = ApiParsers::configurationItem
    )

    suspend fun updateConfigurationItem(
        id: Long,
        token: String,
        name: String,
        description: String?,
        serviceNowSysId: String
    ): ConfigurationItemSummary = sendObject(
        path = "configuration-items/$id",
        method = "PUT",
        token = token,
        body = jsonObject(
            "name" to name,
            "description" to description,
            "serviceNowSysId" to serviceNowSysId
        ),
        parser = ApiParsers::configurationItem
    )

    suspend fun deleteConfigurationItem(id: Long, token: String) {
        delete("configuration-items/$id", token)
    }

    suspend fun searchServiceNowConfigurationItems(token: String, query: String): List<ServiceNowLookupResult> =
        getArray("servicenow/lookup/configuration-items", token, "query" to query).mapObjects(ApiParsers::lookupResult)

    suspend fun fetchCiUserMappings(token: String): List<CiUserMappingSummary> =
        getArray("ci-user-mappings", token).mapObjects(ApiParsers::ciMapping)

    suspend fun replaceCiUserMappingsForCi(
        token: String,
        configurationItemId: Long,
        teamMemberIds: List<Long>
    ): List<CiUserMappingSummary> {
        return sendArray(
            path = "ci-user-mappings/bulk",
            method = "POST",
            token = token,
            body = jsonObject(
                "configurationItemId" to configurationItemId,
                "teamMemberIds" to teamMemberIds
            )
        ).mapObjects(ApiParsers::ciMapping)
    }

    suspend fun fetchCoverageSummary(token: String, days: Int = 7): CoverageSummaryResponse =
        getObject("coverage/summary", token, "days" to days.toString(), parser = ApiParsers::coverage)

    suspend fun fetchServiceNowHealth(token: String): ServiceNowHealthResponse =
        getObject("servicenow/health", token, parser = ApiParsers::health)

    suspend fun fetchServiceNowValidation(token: String): ServiceNowValidationResponse =
        getObject("servicenow/validation", token, parser = ApiParsers::validation)

    suspend fun pollServiceNowNow(token: String): ServiceNowPollNowResponse =
        sendObject("servicenow/poll-now", "POST", token, null, ApiParsers::pollNow)

    suspend fun fetchLeaveHandoff(token: String): LeaveHandoffResponse =
        getObject("servicenow/leave-handoff", token, parser = ApiParsers::handoff)

    suspend fun fetchServiceNowLogs(token: String): List<ServiceNowRunLogSummary> =
        getArray("logs/servicenow", token).mapObjects(ApiParsers::log)

    suspend fun fetchAssignmentDiagnostics(token: String): AssignmentDiagnosticsResponse =
        getObject("servicenow/assignment-diagnostics", token, parser = ApiParsers::diagnostics)

    suspend fun fetchUsers(token: String): List<UserSummary> =
        getArray("users", token).mapObjects(ApiParsers::user)

    suspend fun fetchWorkspaceTeams(token: String): List<TeamSummary> =
        getArray("workspace/teams", token).mapObjects(ApiParsers::team)

    suspend fun updateUserRole(id: Long, token: String, role: String): UserSummary =
        sendObject("users/$id/role", "PUT", token, jsonObject("role" to role), ApiParsers::user)

    suspend fun assignUserToTeam(userId: Long, teamId: Long, token: String): UserSummary =
        sendObject("users/$userId/teams", "POST", token, jsonObject("teamId" to teamId), ApiParsers::user)

    suspend fun removeUserFromTeam(userId: Long, teamId: Long, token: String): UserSummary =
        deleteReturning("users/$userId/teams/$teamId", token, ApiParsers::user)

    suspend fun updateUserTeamRole(userId: Long, teamId: Long, token: String, role: String): UserSummary =
        sendObject("users/$userId/teams/$teamId/role", "PUT", token, jsonObject("role" to role), ApiParsers::user)

    suspend fun deleteCurrentAccount(token: String): AccountDeletionResponse =
        deleteReturning("account", token, ApiParsers::deletion)

    suspend fun deleteUserAccount(userId: Long, token: String): AccountDeletionResponse =
        deleteReturning("users/$userId", token, ApiParsers::deletion)

    suspend fun registerMobileDeviceToken(token: String, deviceToken: String, environment: String) {
        sendIgnoringResponse(
            path = "mobile/device-token",
            method = "POST",
            token = token,
            body = jsonObject(
                "deviceToken" to deviceToken,
                "platform" to "android",
                "environment" to environment
            )
        )
    }

    suspend fun unregisterMobileDeviceToken(token: String, deviceToken: String, environment: String) {
        sendIgnoringResponse(
            path = "mobile/device-token/unregister",
            method = "POST",
            token = token,
            body = jsonObject(
                "deviceToken" to deviceToken,
                "platform" to "android",
                "environment" to environment
            )
        )
    }

    private fun parseSignInResponse(responseText: String): SignInResponse {
        val json = JSONObject(responseText)
        val workspaceJson = json.optJSONObject("workspace")
        return SignInResponse(
            token = json.getString("token"),
            userId = json.optLongForKeys("u_id", "userId", "id") ?: 0L,
            username = json.optStringOrNull("username") ?: "",
            firstName = json.optStringOrNull("firstName"),
            lastName = json.optStringOrNull("lastName"),
            workEmail = json.optStringOrNull("workEmail"),
            role = json.optStringOrNull("role") ?: "User",
            workspace = workspaceJson?.let {
                WorkspaceSummary(
                    organizationId = it.optLongForKeys("organizationId", "organization_id"),
                    organizationName = it.optStringOrNull("organizationName"),
                    teamId = it.optLongForKeys("teamId", "team_id"),
                    teamName = it.optStringOrNull("teamName"),
                    teamRole = it.optStringOrNull("teamRole"),
                    teamTimezone = it.optStringOrNull("teamTimezone")
                )
            }
        )
    }

    private suspend fun getArray(
        path: String,
        token: String,
        vararg query: Pair<String, String>
    ) = JSONArray(request(path = path, method = "GET", token = token, query = query.toList()))

    private suspend fun <T> getObject(
        path: String,
        token: String,
        vararg query: Pair<String, String>,
        parser: (JSONObject) -> T
    ): T = parser(JSONObject(request(path = path, method = "GET", token = token, query = query.toList())))

    private suspend fun <T> sendObject(
        path: String,
        method: String,
        token: String,
        body: JSONObject?,
        parser: (JSONObject) -> T
    ): T = parser(JSONObject(request(path = path, method = method, token = token, body = body)))

    private suspend fun sendArray(
        path: String,
        method: String,
        token: String,
        body: JSONObject
    ) = JSONArray(request(path = path, method = method, token = token, body = body))

    private suspend fun sendIgnoringResponse(
        path: String,
        method: String,
        token: String,
        body: JSONObject
    ) {
        request(path = path, method = method, token = token, body = body)
    }

    private suspend fun delete(path: String, token: String) {
        request(path = path, method = "DELETE", token = token)
    }

    private suspend fun <T> deleteReturning(
        path: String,
        token: String,
        parser: (JSONObject) -> T
    ): T = parser(JSONObject(request(path = path, method = "DELETE", token = token)))

    private suspend fun request(
        path: String,
        method: String,
        token: String? = null,
        body: JSONObject? = null,
        query: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val queryString = query
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "?", separator = "&") { (key, value) ->
                "${URLEncoder.encode(key, Charsets.UTF_8.name())}=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
            }
            .orEmpty()
        var url = URL("$baseUrl/$path$queryString")
        var requestMethod = method
        var requestBody = body
        val bearerToken = token.normalizedBearerToken()

        repeat(MAX_REDIRECTS + 1) {
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.requestMethod = requestMethod
                connection.connectTimeout = 15_000
                connection.readTimeout = 25_000
                connection.setRequestProperty("Accept", "application/json")
                bearerToken?.let {
                    connection.setRequestProperty("Authorization", "Bearer $it")
                }
                if (requestBody != null) {
                    val encodedBody = requestBody.toString().toByteArray(Charsets.UTF_8)
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { output ->
                        output.write(encodedBody)
                    }
                }

                val statusCode = connection.responseCode
                if (statusCode.isRedirect()) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) {
                        throw InciTeamApiException(
                            message = "The server redirected the request without a destination.",
                            statusCode = statusCode
                        )
                    }
                    url = URL(url, location)
                    if (statusCode == HttpURLConnection.HTTP_SEE_OTHER) {
                        requestMethod = "GET"
                        requestBody = null
                    }
                    return@repeat
                }

                val responseText = connection.readResponseText(statusCode)
                if (statusCode !in 200..299) {
                    throw InciTeamApiException(
                        message = responseText.ifBlank { "The server returned an error." },
                        statusCode = statusCode
                    )
                }
                return@withContext responseText.ifBlank { "{}" }
            } finally {
                connection.disconnect()
            }
        }

        throw InciTeamApiException(
            message = "The server redirected the request too many times.",
            statusCode = 0
        )
    }

    private companion object {
        const val MAX_REDIRECTS = 3
    }
}

class InciTeamApiException(
    override val message: String,
    val statusCode: Int
) : Exception("$message ($statusCode)")

private fun HttpURLConnection.readResponseText(statusCode: Int): String {
    val stream = if (statusCode in 200..299) inputStream else errorStream
    return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        .trim()
        .removeSurrounding("\"")
}

private fun Int.isRedirect(): Boolean =
    this == HttpURLConnection.HTTP_MOVED_PERM ||
        this == HttpURLConnection.HTTP_MOVED_TEMP ||
        this == HttpURLConnection.HTTP_SEE_OTHER ||
        this == 307 ||
        this == 308

private fun String?.normalizedBearerToken(): String? {
    val normalized = this?.trim().orEmpty()
    if (normalized.isEmpty()) {
        return null
    }
    return if (normalized.startsWith("Bearer ", ignoreCase = true)) {
        normalized.drop(7).trim().takeIf { it.isNotEmpty() }
    } else {
        normalized
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optString(name).trim().takeIf { it.isNotEmpty() }
}

private fun JSONObject.optLongForKeys(vararg names: String): Long? {
    for (name in names) {
        if (has(name) && !isNull(name)) {
            return optLong(name)
        }
    }
    return null
}
