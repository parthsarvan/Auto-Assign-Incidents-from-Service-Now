package com.inciteam.app.data

import org.json.JSONArray
import org.json.JSONObject

data class GeoSummary(val id: Long, val name: String)
data class ShiftSummary(val id: Long, val name: String, val startTime: String?, val endTime: String?)
data class GeoShiftMappingSummary(val id: Long, val geo: GeoSummary?, val shift: ShiftSummary?)

data class TeamMemberSummary(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
    val serviceNowSysId: String?,
    val geo: GeoSummary?
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { email ?: "Team member $id" }
}

data class JoinedTeamUserSummary(
    val id: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val workEmail: String?
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { username }
}

data class ServiceNowLookupResult(
    val sysId: String,
    val displayName: String?,
    val email: String?,
    val userName: String?,
    val detail: String?,
    val secondaryDetail: String?
) {
    val primaryLabel: String
        get() = displayName ?: email ?: userName ?: "ServiceNow record"

    val secondaryLabel: String
        get() = listOfNotNull(email, userName).filter { it.isNotBlank() }.joinToString(" / ")
            .ifBlank { detail ?: secondaryDetail ?: "ServiceNow record" }
}

data class TeamMemberScheduleSummary(
    val id: Long,
    val teamMember: TeamMemberSummary?,
    val geo: GeoSummary?,
    val shift: ShiftSummary?,
    val startDate: String,
    val endDate: String,
    val coverageDays: String?
)

data class AvailabilityRecord(
    val tmId: Long?,
    val geoName: String,
    val shiftName: String,
    val date: String,
    val fullName: String
)

data class LeaveRecordSummary(
    val tmId: Long?,
    val fullName: String,
    val geoName: String?,
    val shiftName: String?,
    val startTs: String,
    val endTs: String,
    val reason: String?
)

data class BreakRecordSummary(
    val tmId: Long?,
    val fullName: String,
    val geoName: String?,
    val shiftName: String?,
    val startTs: String,
    val endTs: String,
    val reason: String?
)

data class AvailabilityEntrySummary(
    val id: Long,
    val teamMember: TeamMemberSummary?,
    val startTs: String,
    val endTs: String,
    val reason: String?
)

data class ConfigurationItemSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val serviceNowSysId: String?
)

data class CiUserMappingSummary(
    val id: Long,
    val configurationItem: ConfigurationItemSummary?,
    val teamMember: TeamMemberSummary?,
    val sortOrder: Int?
)

data class CoverageSummaryResponse(
    val checkedAt: String?,
    val startDate: String?,
    val endDate: String?,
    val totalGeoShiftDays: Int,
    val coveredGeoShiftDays: Int,
    val gapCount: Int,
    val ciRiskCount: Int,
    val issues: List<CoverageIssueSummary>
)

data class CoverageIssueSummary(
    val type: String?,
    val severity: String?,
    val message: String?,
    val date: String?,
    val geo: String?,
    val shift: String?,
    val configurationItem: String?
)

data class ServiceNowHealthResponse(
    val checkedAt: String?,
    val healthy: Boolean,
    val status: String?,
    val message: String?,
    val instanceUrl: String?,
    val lastPollAt: String?,
    val lastPollStatus: String?,
    val lastPollMessage: String?
)

data class ServiceNowValidationResponse(
    val checkedAt: String?,
    val valid: Boolean,
    val message: String?,
    val configurationItemCount: Int,
    val validConfigurationItemCount: Int,
    val teamMemberCount: Int,
    val validTeamMemberCount: Int,
    val issues: List<ServiceNowValidationIssue>
)

data class ServiceNowValidationIssue(
    val type: String?,
    val localName: String?,
    val localSysId: String?,
    val message: String?
)

data class ServiceNowPollNowResponse(
    val polledAt: String?,
    val status: String?,
    val message: String?,
    val incidentCount: Int,
    val successCount: Int,
    val failedCount: Int,
    val skippedCount: Int
)

data class ServiceNowRunLogSummary(
    val timestamp: String?,
    val teamId: Long?,
    val teamName: String?,
    val type: String?,
    val status: String?,
    val message: String?,
    val incidentCount: Int,
    val incidents: List<ServiceNowIncidentSummary>,
    val assignmentSelections: List<ServiceNowAssignmentSelection>,
    val assignmentResults: List<ServiceNowAssignmentResult>,
    val assignmentConfirmation: String?
)

data class ServiceNowIncidentSummary(
    val number: String?,
    val createdOn: String?,
    val configurationItem: String?,
    val assignmentGroup: String?,
    val priority: String?,
    val caller: String?,
    val shortDescription: String?,
    val suggestedAssignee: String?,
    val suggestedAssigneeEmail: String?,
    val suggestedGeo: String?,
    val suggestedShift: String?
)

data class ServiceNowAssignmentSelection(
    val incidentNumber: String?,
    val assigneeName: String?,
    val assigneeEmail: String?,
    val geo: String?,
    val shift: String?
)

data class ServiceNowAssignmentResult(
    val incidentNumber: String?,
    val assigneeName: String?,
    val assigneeEmail: String?,
    val geo: String?,
    val shift: String?,
    val status: String?,
    val message: String?
)

data class LeaveHandoffResponse(
    val checkedAt: String?,
    val impactedMemberCount: Int,
    val activeIncidentCount: Int,
    val items: List<LeaveHandoffItem>
)

data class LeaveHandoffItem(
    val teamMemberName: String?,
    val email: String?,
    val leaveStart: String?,
    val leaveEnd: String?,
    val reason: String?,
    val incidents: List<LeaveHandoffIncident>
)

data class LeaveHandoffIncident(
    val number: String?,
    val priority: String?,
    val configurationItem: String?,
    val shortDescription: String?
)

data class AssignmentDiagnosticsResponse(
    val checkedAt: String?,
    val incidentCount: Int,
    val assignableCount: Int,
    val skippedCount: Int,
    val incidents: List<AssignmentDiagnosticItem>
)

data class AssignmentDiagnosticItem(
    val incidentNumber: String?,
    val incidentSysId: String?,
    val caller: String?,
    val configurationItem: String?,
    val priority: String?,
    val createdOn: String?,
    val shortDescription: String?,
    val status: String?,
    val reason: String?,
    val suggestion: IncidentAssignmentSuggestion?,
    val candidateChecks: List<AssignmentCandidateCheck>
)

data class IncidentAssignmentSuggestion(
    val assigneeName: String?,
    val assigneeEmail: String?,
    val assigneeSysId: String?,
    val assigneePhone: String?,
    val geo: String?,
    val shift: String?,
    val routingNote: String?,
    val routedTeamName: String?
)

data class AssignmentCandidateCheck(
    val teamMemberName: String?,
    val email: String?,
    val serviceNowUserSysId: String?,
    val sortOrder: Int?,
    val memberGeo: String?,
    val activeSchedules: String?,
    val matchStatus: String?,
    val onLeave: Boolean,
    val onBreak: Boolean,
    val eligible: Boolean,
    val selected: Boolean,
    val reason: String?
)

data class TeamSummary(
    val id: Long,
    val teamName: String,
    val description: String?,
    val joinCode: String?,
    val current: Boolean
)

data class UserSummary(
    val id: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val workEmail: String?,
    val role: String,
    val currentTeamId: Long?,
    val currentTeamName: String?,
    val teamMemberships: List<UserTeamMembershipSummary>
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { username }
}

data class UserTeamMembershipSummary(
    val teamId: Long,
    val teamName: String,
    val role: String,
    val current: Boolean
)

data class AccountDeletionResponse(
    val deletedUserId: Long?,
    val deletedUsername: String?,
    val userDeleted: Boolean,
    val teamMemberRecordsDeleted: Int,
    val teamMembershipsDeleted: Int,
    val organizationMembershipsDeleted: Int,
    val mobileDeviceTokensDeleted: Int,
    val message: String?
)

object ApiParsers {
    fun teamMember(json: JSONObject) = TeamMemberSummary(
        id = json.long("tm_id", "tmId", "id") ?: 0L,
        firstName = json.string("f_name", "fName", "firstName").orEmpty(),
        lastName = json.string("l_name", "lName", "lastName").orEmpty(),
        email = json.string("email"),
        phone = json.string("phone"),
        serviceNowSysId = json.string("sys_id", "sysId", "serviceNowUserSysId"),
        geo = json.obj("geo")?.let(::geo)
    )

    fun joinedUser(json: JSONObject) = JoinedTeamUserSummary(
        id = json.long("id", "userId", "u_id") ?: 0L,
        username = json.string("username").orEmpty(),
        firstName = json.string("firstName"),
        lastName = json.string("lastName"),
        workEmail = json.string("workEmail")
    )

    fun lookupResult(json: JSONObject) = ServiceNowLookupResult(
        sysId = json.string("sysId", "sys_id").orEmpty(),
        displayName = json.string("displayName", "display_name"),
        email = json.string("email"),
        userName = json.string("userName", "user_name"),
        detail = json.string("detail"),
        secondaryDetail = json.string("secondaryDetail", "secondary_detail")
    )

    fun geo(json: JSONObject) = GeoSummary(
        id = json.long("g_id", "gId", "id") ?: 0L,
        name = json.string("name") ?: "Geo ${json.long("g_id", "gId", "id") ?: 0L}"
    )

    fun shift(json: JSONObject) = ShiftSummary(
        id = json.long("s_id", "sId", "id") ?: 0L,
        name = json.string("name") ?: "Shift ${json.long("s_id", "sId", "id") ?: 0L}",
        startTime = json.string("startTime", "start_time"),
        endTime = json.string("endTime", "end_time")
    )

    fun geoShiftMapping(json: JSONObject) = GeoShiftMappingSummary(
        id = json.long("gsm_id", "gsmId", "id") ?: 0L,
        geo = json.obj("geo")?.let(::geo),
        shift = json.obj("shift")?.let(::shift)
    )

    fun schedule(json: JSONObject) = TeamMemberScheduleSummary(
        id = json.long("tms_id", "tmsId", "id") ?: 0L,
        teamMember = json.obj("teamMember")?.let(::teamMember),
        geo = json.obj("geo")?.let(::geo),
        shift = json.obj("shift")?.let(::shift),
        startDate = json.string("startDate", "start_date").orEmpty(),
        endDate = json.string("endDate", "end_date").orEmpty(),
        coverageDays = json.string("coverageDays", "coverage_days")
    )

    fun availability(json: JSONObject) = AvailabilityRecord(
        tmId = json.long("tmId"),
        geoName = json.string("geoName").orEmpty(),
        shiftName = json.string("shiftName").orEmpty(),
        date = json.string("date").orEmpty(),
        fullName = json.string("fullName").orEmpty()
    )

    fun leaveRecord(json: JSONObject) = LeaveRecordSummary(
        tmId = json.long("tmId"),
        fullName = json.string("fullName").orEmpty(),
        geoName = json.string("geoName"),
        shiftName = json.string("shiftName"),
        startTs = json.string("startTs").orEmpty(),
        endTs = json.string("endTs").orEmpty(),
        reason = json.string("reason")
    )

    fun breakRecord(json: JSONObject) = BreakRecordSummary(
        tmId = json.long("tmId"),
        fullName = json.string("fullName").orEmpty(),
        geoName = json.string("geoName"),
        shiftName = json.string("shiftName"),
        startTs = json.string("startTs").orEmpty(),
        endTs = json.string("endTs").orEmpty(),
        reason = json.string("reason")
    )

    fun availabilityEntry(json: JSONObject) = AvailabilityEntrySummary(
        id = json.long("leave_id", "break_id", "id") ?: 0L,
        teamMember = json.obj("teamMember")?.let(::teamMember),
        startTs = json.string("startTs", "start_ts").orEmpty(),
        endTs = json.string("endTs", "end_ts").orEmpty(),
        reason = json.string("reason")
    )

    fun configurationItem(json: JSONObject) = ConfigurationItemSummary(
        id = json.long("ci_id", "ciId", "id") ?: 0L,
        name = json.string("name") ?: "CI ${json.long("ci_id", "ciId", "id") ?: 0L}",
        description = json.string("description"),
        serviceNowSysId = json.string("serviceNowSysId", "service_now_sys_id")
    )

    fun ciMapping(json: JSONObject) = CiUserMappingSummary(
        id = json.long("mapping_id", "mappingId", "id") ?: 0L,
        configurationItem = json.obj("configurationItem")?.let(::configurationItem),
        teamMember = json.obj("teamMember")?.let(::teamMember),
        sortOrder = json.int("sortOrder", "sort_order")
    )

    fun coverage(json: JSONObject) = CoverageSummaryResponse(
        checkedAt = json.string("checkedAt"),
        startDate = json.string("startDate"),
        endDate = json.string("endDate"),
        totalGeoShiftDays = json.int("totalGeoShiftDays") ?: 0,
        coveredGeoShiftDays = json.int("coveredGeoShiftDays") ?: 0,
        gapCount = json.int("gapCount") ?: 0,
        ciRiskCount = json.int("ciRiskCount") ?: 0,
        issues = json.array("issues").mapObjects {
            CoverageIssueSummary(
                type = it.string("type"),
                severity = it.string("severity"),
                message = it.string("message"),
                date = it.string("date"),
                geo = it.string("geo"),
                shift = it.string("shift"),
                configurationItem = it.string("configurationItem")
            )
        }
    )

    fun health(json: JSONObject) = ServiceNowHealthResponse(
        checkedAt = json.string("checkedAt"),
        healthy = json.bool("healthy") ?: false,
        status = json.string("status"),
        message = json.string("message"),
        instanceUrl = json.string("instanceUrl"),
        lastPollAt = json.string("lastPollAt"),
        lastPollStatus = json.string("lastPollStatus"),
        lastPollMessage = json.string("lastPollMessage")
    )

    fun validation(json: JSONObject) = ServiceNowValidationResponse(
        checkedAt = json.string("checkedAt"),
        valid = json.bool("valid") ?: false,
        message = json.string("message"),
        configurationItemCount = json.int("configurationItemCount") ?: 0,
        validConfigurationItemCount = json.int("validConfigurationItemCount") ?: 0,
        teamMemberCount = json.int("teamMemberCount") ?: 0,
        validTeamMemberCount = json.int("validTeamMemberCount") ?: 0,
        issues = json.array("issues").mapObjects {
            ServiceNowValidationIssue(
                type = it.string("type"),
                localName = it.string("localName"),
                localSysId = it.string("localSysId"),
                message = it.string("message")
            )
        }
    )

    fun pollNow(json: JSONObject) = ServiceNowPollNowResponse(
        polledAt = json.string("polledAt"),
        status = json.string("status"),
        message = json.string("message"),
        incidentCount = json.int("incidentCount") ?: 0,
        successCount = json.int("successCount") ?: 0,
        failedCount = json.int("failedCount") ?: 0,
        skippedCount = json.int("skippedCount") ?: 0
    )

    fun log(json: JSONObject) = ServiceNowRunLogSummary(
        timestamp = json.string("timestamp"),
        teamId = json.long("teamId"),
        teamName = json.string("teamName"),
        type = json.string("type"),
        status = json.string("status"),
        message = json.string("message"),
        incidentCount = json.int("incidentCount") ?: 0,
        incidents = json.array("incidents").mapObjects(::incident),
        assignmentSelections = json.array("assignmentSelections").mapObjects(::selection),
        assignmentResults = json.array("assignmentResults").mapObjects(::result),
        assignmentConfirmation = json.string("assignmentConfirmation")
    )

    fun incident(json: JSONObject) = ServiceNowIncidentSummary(
        number = json.string("number"),
        createdOn = json.string("createdOn"),
        configurationItem = json.string("configurationItem"),
        assignmentGroup = json.string("assignmentGroup"),
        priority = json.string("priority"),
        caller = json.string("caller"),
        shortDescription = json.string("shortDescription"),
        suggestedAssignee = json.string("suggestedAssignee"),
        suggestedAssigneeEmail = json.string("suggestedAssigneeEmail"),
        suggestedGeo = json.string("suggestedGeo"),
        suggestedShift = json.string("suggestedShift")
    )

    fun selection(json: JSONObject) = ServiceNowAssignmentSelection(
        incidentNumber = json.string("incidentNumber"),
        assigneeName = json.string("assigneeName"),
        assigneeEmail = json.string("assigneeEmail"),
        geo = json.string("geo"),
        shift = json.string("shift")
    )

    fun result(json: JSONObject) = ServiceNowAssignmentResult(
        incidentNumber = json.string("incidentNumber"),
        assigneeName = json.string("assigneeName"),
        assigneeEmail = json.string("assigneeEmail"),
        geo = json.string("geo"),
        shift = json.string("shift"),
        status = json.string("status"),
        message = json.string("message")
    )

    fun handoff(json: JSONObject) = LeaveHandoffResponse(
        checkedAt = json.string("checkedAt"),
        impactedMemberCount = json.int("impactedMemberCount") ?: 0,
        activeIncidentCount = json.int("activeIncidentCount") ?: 0,
        items = json.array("items").mapObjects {
            LeaveHandoffItem(
                teamMemberName = it.string("teamMemberName"),
                email = it.string("email"),
                leaveStart = it.string("leaveStart"),
                leaveEnd = it.string("leaveEnd"),
                reason = it.string("reason"),
                incidents = it.array("incidents").mapObjects { incident ->
                    LeaveHandoffIncident(
                        number = incident.string("number"),
                        priority = incident.string("priority"),
                        configurationItem = incident.string("configurationItem"),
                        shortDescription = incident.string("shortDescription")
                    )
                }
            )
        }
    )

    fun diagnostics(json: JSONObject) = AssignmentDiagnosticsResponse(
        checkedAt = json.string("checkedAt"),
        incidentCount = json.int("incidentCount") ?: 0,
        assignableCount = json.int("assignableCount") ?: 0,
        skippedCount = json.int("skippedCount") ?: 0,
        incidents = json.array("incidents").mapObjects(::diagnosticItem)
    )

    fun diagnosticItem(json: JSONObject) = AssignmentDiagnosticItem(
        incidentNumber = json.string("incidentNumber"),
        incidentSysId = json.string("incidentSysId"),
        caller = json.string("caller"),
        configurationItem = json.string("configurationItem"),
        priority = json.string("priority"),
        createdOn = json.string("createdOn"),
        shortDescription = json.string("shortDescription"),
        status = json.string("status"),
        reason = json.string("reason"),
        suggestion = json.obj("suggestion")?.let(::suggestion),
        candidateChecks = json.array("candidateChecks").mapObjects(::candidateCheck)
    )

    fun suggestion(json: JSONObject) = IncidentAssignmentSuggestion(
        assigneeName = json.string("assigneeName"),
        assigneeEmail = json.string("assigneeEmail"),
        assigneeSysId = json.string("assigneeSysId"),
        assigneePhone = json.string("assigneePhone"),
        geo = json.string("geo"),
        shift = json.string("shift"),
        routingNote = json.string("routingNote"),
        routedTeamName = json.string("routedTeamName")
    )

    fun candidateCheck(json: JSONObject) = AssignmentCandidateCheck(
        teamMemberName = json.string("teamMemberName"),
        email = json.string("email"),
        serviceNowUserSysId = json.string("serviceNowUserSysId"),
        sortOrder = json.int("sortOrder"),
        memberGeo = json.string("memberGeo"),
        activeSchedules = json.string("activeSchedules"),
        matchStatus = json.string("matchStatus"),
        onLeave = json.bool("onLeave") ?: false,
        onBreak = json.bool("onBreak") ?: false,
        eligible = json.bool("eligible") ?: false,
        selected = json.bool("selected") ?: false,
        reason = json.string("reason")
    )

    fun team(json: JSONObject) = TeamSummary(
        id = json.long("teamId", "id") ?: 0L,
        teamName = json.string("teamName").orEmpty(),
        description = json.string("description"),
        joinCode = json.string("joinCode"),
        current = json.bool("current") ?: false
    )

    fun user(json: JSONObject) = UserSummary(
        id = json.long("id", "userId", "u_id") ?: 0L,
        username = json.string("username").orEmpty(),
        firstName = json.string("firstName"),
        lastName = json.string("lastName"),
        workEmail = json.string("workEmail"),
        role = json.string("role") ?: "User",
        currentTeamId = json.long("currentTeamId"),
        currentTeamName = json.string("currentTeamName"),
        teamMemberships = json.array("teamMemberships").mapObjects {
            UserTeamMembershipSummary(
                teamId = it.long("teamId") ?: 0L,
                teamName = it.string("teamName").orEmpty(),
                role = it.string("role") ?: "MEMBER",
                current = it.bool("current") ?: false
            )
        }
    )

    fun deletion(json: JSONObject) = AccountDeletionResponse(
        deletedUserId = json.long("deletedUserId"),
        deletedUsername = json.string("deletedUsername"),
        userDeleted = json.bool("userDeleted") ?: false,
        teamMemberRecordsDeleted = json.int("teamMemberRecordsDeleted") ?: 0,
        teamMembershipsDeleted = json.int("teamMembershipsDeleted") ?: 0,
        organizationMembershipsDeleted = json.int("organizationMembershipsDeleted") ?: 0,
        mobileDeviceTokensDeleted = json.int("mobileDeviceTokensDeleted") ?: 0,
        message = json.string("message")
    )
}

fun JSONArray.mapObjects(): List<JSONObject> {
    return (0 until length()).mapNotNull { index -> optJSONObject(index) }
}

fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    return mapObjects().map(transform)
}

fun JSONObject.string(vararg names: String): String? {
    for (name in names) {
        if (has(name) && !isNull(name)) {
            return optString(name).trim().takeIf { it.isNotEmpty() }
        }
    }
    return null
}

fun JSONObject.long(vararg names: String): Long? {
    for (name in names) {
        if (has(name) && !isNull(name)) {
            return optLong(name)
        }
    }
    return null
}

fun JSONObject.int(vararg names: String): Int? {
    for (name in names) {
        if (has(name) && !isNull(name)) {
            return optInt(name)
        }
    }
    return null
}

fun JSONObject.bool(vararg names: String): Boolean? {
    for (name in names) {
        if (has(name) && !isNull(name)) {
            return optBoolean(name)
        }
    }
    return null
}

fun JSONObject.obj(name: String): JSONObject? {
    return if (has(name) && !isNull(name)) optJSONObject(name) else null
}

fun JSONObject.array(name: String): JSONArray {
    return if (has(name) && !isNull(name)) optJSONArray(name) ?: JSONArray() else JSONArray()
}

fun jsonObject(vararg pairs: Pair<String, Any?>): JSONObject {
    val json = JSONObject()
    for ((key, value) in pairs) {
        when (value) {
            null -> json.put(key, JSONObject.NULL)
            is Iterable<*> -> json.put(key, JSONArray(value.toList()))
            else -> json.put(key, value)
        }
    }
    return json
}
