package com.inciteam.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inciteam.app.data.AssignmentDiagnosticsResponse
import com.inciteam.app.data.AuthenticatedUser
import com.inciteam.app.data.AvailabilityEntrySummary
import com.inciteam.app.data.AvailabilityRecord
import com.inciteam.app.data.BreakRecordSummary
import com.inciteam.app.data.CiUserMappingSummary
import com.inciteam.app.data.ConfigurationItemSummary
import com.inciteam.app.data.CoverageSummaryResponse
import com.inciteam.app.data.GeoSummary
import com.inciteam.app.data.LeaveHandoffResponse
import com.inciteam.app.data.LeaveRecordSummary
import com.inciteam.app.data.JoinedTeamUserSummary
import com.inciteam.app.data.ServiceNowHealthResponse
import com.inciteam.app.data.ServiceNowLookupResult
import com.inciteam.app.data.ServiceNowRunLogSummary
import com.inciteam.app.data.ServiceNowValidationResponse
import com.inciteam.app.data.ShiftSummary
import com.inciteam.app.data.TeamMemberScheduleSummary
import com.inciteam.app.data.TeamMemberSummary
import com.inciteam.app.data.TeamSummary
import com.inciteam.app.data.UserSummary
import com.inciteam.app.network.InciTeamApiClient
import com.inciteam.app.session.SessionStore
import com.inciteam.app.ui.theme.InciTeamBackgroundBottom
import com.inciteam.app.ui.theme.InciTeamBackgroundTop
import com.inciteam.app.ui.theme.InciTeamBorder
import com.inciteam.app.ui.theme.InciTeamCard
import com.inciteam.app.ui.theme.InciTeamInk
import com.inciteam.app.ui.theme.InciTeamMuted
import com.inciteam.app.ui.theme.InciTeamPrimary
import com.inciteam.app.ui.theme.InciTeamPrimaryDeep
import com.inciteam.app.ui.theme.InciTeamRow
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FeatureScreen(
    feature: InciTeamFeature,
    token: String,
    user: AuthenticatedUser,
    sessionStore: SessionStore,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val apiClient = remember { InciTeamApiClient() }

    when (feature.id) {
        "roster" -> RosterScreen(feature, token, apiClient, onBack)
        "schedule" -> ScheduleScreen(feature, token, apiClient, onBack)
        "team-members" -> TeamMembersScreen(feature, token, user, apiClient, onBack)
        "leaves" -> AvailabilityEntriesScreen(feature, "leaves", token, user, apiClient, onBack)
        "breaks" -> AvailabilityEntriesScreen(feature, "breaks", token, user, apiClient, onBack)
        "configuration-items" -> ConfigurationItemsScreen(feature, token, user, apiClient, onBack)
        "ci-user-mapping" -> CiUserMappingScreen(feature, token, user, apiClient, onBack)
        "summary" -> SummaryScreen(feature, token, apiClient, onBack)
        "logs" -> LogsScreen(feature, token, apiClient, onBack)
        "diagnostics" -> DiagnosticsScreen(feature, token, apiClient, onBack)
        "account" -> AccountScreen(feature, token, user, apiClient, sessionStore, onBack)
        "user-access" -> UserAccessScreen(feature, token, user, apiClient, onBack)
        else -> SimpleMessageScreen(feature, onBack, "This screen is ready to be implemented.")
    }
}

@Composable
private fun RosterScreen(
    feature: InciTeamFeature,
    token: String,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var viewMode by remember { mutableStateOf(RosterWindow.Week) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    val dates = remember(startDate, viewMode) {
        (0 until viewMode.dayCount).map { startDate.plusDays(it.toLong()).toString() }
    }
    var state by remember { mutableStateOf<RosterData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey, startDate, viewMode) {
        loading = true
        error = null
        try {
            val isoStart = startDate.toString()
            state = coroutineScope {
                val availability = async { apiClient.fetchAvailability(token, isoStart, viewMode.dayCount) }
                val leaves = async { apiClient.fetchLeaves(token, isoStart, viewMode.dayCount) }
                val breaks = async { apiClient.fetchBreaks(token, isoStart, viewMode.dayCount) }
                RosterData(
                    availability = availability.await(),
                    leaves = leaves.await(),
                    breaks = breaks.await()
                )
            }
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    FeatureScaffold(feature, onBack, actions = {
        RefreshButton { refreshKey++ }
    }) {
        StatusContent(loading = loading, error = error, empty = state?.availability.isNullOrEmpty()) {
            val data = state ?: return@StatusContent
            val grouped = data.availability.groupBy { "${it.geoName} / ${it.shiftName}" }
            item {
                DataCard("Roster Window") {
                    SegmentedTextRow(
                        options = listOf("Day", "Week"),
                        selected = viewMode.label,
                        onSelect = { viewMode = if (it == "Day") RosterWindow.Day else RosterWindow.Week }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { startDate = startDate.minusDays(viewMode.dayCount.toLong()) }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous")
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                rosterWindowTitle(startDate, dates, viewMode),
                                color = InciTeamInk,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Text("${data.availability.size} scheduled records", color = InciTeamMuted)
                        }
                        IconButton(onClick = { startDate = startDate.plusDays(viewMode.dayCount.toLong()) }) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next")
                        }
                    }
                }
            }
            grouped.forEach { (geoShift, records) ->
                item {
                    DataCard(title = geoShift) {
                        dates.forEach { date ->
                            val dayRecords = records.filter { it.date == date }
                            if (dayRecords.isNotEmpty()) {
                                Text(displayDate(date), color = InciTeamMuted, fontWeight = FontWeight.ExtraBold)
                                RosterDayGrid(
                                    records = dayRecords,
                                    leaves = data.leaves,
                                    breaks = data.breaks,
                                    date = date
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            item {
                ImpactList("On Leave", data.leaves.map {
                    "${it.fullName} | ${displayDateTime(it.startTs)} - ${displayDateTime(it.endTs)}${it.reason?.let { reason -> " | $reason" }.orEmpty()}"
                })
            }
            item {
                ImpactList("On Break", data.breaks.map {
                    "${it.fullName} | ${displayDateTime(it.startTs)} - ${displayDateTime(it.endTs)}${it.reason?.let { reason -> " | $reason" }.orEmpty()}"
                })
            }
        }
    }
}

@Composable
private fun RosterDayGrid(
    records: List<AvailabilityRecord>,
    leaves: List<LeaveRecordSummary>,
    breaks: List<BreakRecordSummary>,
    date: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InciTeamRow.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        records.chunked(2).forEach { rowRecords ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowRecords.forEach { record ->
                    val onLeave = leaves.any { it.matchesRosterRecord(record, date) }
                    val onBreak = breaks.any { it.matchesRosterRecord(record, date) }
                    AvailabilityChip(
                        name = record.fullName,
                        status = when {
                            onLeave -> "Leave"
                            onBreak -> "Break"
                            else -> "Available"
                        },
                        color = when {
                            onLeave -> Color(0xFFFF3B45)
                            onBreak -> Color(0xFFE1A600)
                            else -> Color(0xFF31C85B)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowRecords.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryScreen(
    feature: InciTeamFeature,
    token: String,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf<SummaryData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            data = coroutineScope {
                val health = async { apiClient.fetchServiceNowHealth(token) }
                val validation = async { apiClient.fetchServiceNowValidation(token) }
                val logs = async { apiClient.fetchServiceNowLogs(token) }
                val coverage = async { apiClient.fetchCoverageSummary(token, 7) }
                val handoff = async { apiClient.fetchLeaveHandoff(token) }
                SummaryData(health.await(), validation.await(), logs.await(), coverage.await(), handoff.await())
            }
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    FeatureScaffold(feature, onBack, actions = {
        PollButton {
            scope.launch {
                message = try {
                    apiClient.pollServiceNowNow(token).message ?: "Poll completed."
                } catch (cause: Exception) {
                    cause.message ?: "Poll failed."
                }
                refreshKey++
            }
        }
    }) {
        StatusContent(loading = loading, error = error, empty = data == null) {
            val summary = data ?: return@StatusContent
            message?.let {
                item { InfoCard("Poll completed", it, Color(0xFF1F9D68)) }
            }
            item {
                MetricGrid(
                    listOf(
                        "Health" to (summary.health.status ?: if (summary.health.healthy) "Healthy" else "Issue"),
                        "Latest Logs" to summary.logs.size.toString(),
                        "Coverage Gaps" to summary.coverage.gapCount.toString(),
                        "CI Risks" to summary.coverage.ciRiskCount.toString()
                    )
                )
            }
            item {
                DataCard("ServiceNow Connection") {
                    DetailLine("Status", summary.health.status ?: "-")
                    DetailLine("Message", summary.health.message ?: "-")
                    DetailLine("Instance", summary.health.instanceUrl ?: "-")
                    DetailLine("Last poll", shortDateTime(summary.health.lastPollAt))
                }
            }
            item {
                DataCard("Setup Validation") {
                    DetailLine("Valid", if (summary.validation.valid) "Yes" else "No")
                    DetailLine("CIs", "${summary.validation.validConfigurationItemCount}/${summary.validation.configurationItemCount}")
                    DetailLine("Team members", "${summary.validation.validTeamMemberCount}/${summary.validation.teamMemberCount}")
                    summary.validation.issues.take(5).forEach {
                        DetailLine(it.type ?: "Issue", it.message ?: it.localName ?: "-")
                    }
                }
            }
            item {
                ImpactList(
                    title = "Coverage Issues",
                    values = summary.coverage.issues.take(8).map {
                        listOfNotNull(it.severity, it.date, it.geo, it.shift, it.configurationItem, it.message)
                            .joinToString(" | ")
                    }
                )
            }
            item {
                ImpactList(
                    title = "Leave Handoff",
                    values = summary.handoff.items.take(8).map {
                        "${it.teamMemberName ?: "Member"} | ${it.incidents.size} active incidents"
                    }
                )
            }
        }
    }
}

@Composable
private fun LogsScreen(
    feature: InciTeamFeature,
    token: String,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var logs by remember { mutableStateOf<List<ServiceNowRunLogSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var resultFilter by remember { mutableStateOf("ALL") }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val filteredLogs = remember(logs, searchText, statusFilter, resultFilter) {
        logs.filter { it.matchesFilters(searchText, statusFilter, resultFilter) }
    }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            logs = apiClient.fetchServiceNowLogs(token)
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    FeatureScaffold(feature, onBack, actions = {
        PollButton {
            scope.launch {
                message = try {
                    apiClient.pollServiceNowNow(token).message ?: "Poll completed."
                } catch (cause: Exception) {
                    cause.message ?: "Poll failed."
                }
                refreshKey++
            }
        }
        Spacer(Modifier.width(8.dp))
        RefreshButton { refreshKey++ }
    }) {
        StatusContent(loading = loading, error = error, empty = logs.isEmpty()) {
            message?.let { item { InfoCard("Poll completed", it, Color(0xFF1F9D68)) } }
            item {
                MetricGrid(
                    listOf(
                        "Entries" to logs.size.toString(),
                        "Healthy" to logs.count { it.status == "OK" }.toString(),
                        "Errors" to logs.count { it.status == "ERROR" }.toString(),
                        "Results" to logs.flatMap { it.assignmentResults }.size.toString()
                    )
                )
            }
            item {
                DataCard("Filters") {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search incident, CI, caller, message...") },
                        colors = formTextFieldColors(),
                        singleLine = true
                    )
                    SegmentedTextRow(
                        options = listOf("ALL", "OK", "ERROR"),
                        selected = statusFilter,
                        onSelect = { statusFilter = it }
                    )
                    SegmentedTextRow(
                        options = listOf("ALL", "SUCCESS", "FAILED", "SKIPPED"),
                        selected = resultFilter,
                        onSelect = { resultFilter = it }
                    )
                }
            }
            if (filteredLogs.isEmpty()) {
                item { InfoCard("No matching logs", "No logs match the current filters.", InciTeamPrimary) }
            }
            items(filteredLogs) { log ->
                DataCard(title = log.type ?: "Log") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayDateTime(log.timestamp), color = InciTeamMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (!log.message.isNullOrBlank()) {
                                Text(log.message, color = InciTeamMuted)
                            }
                        }
                        StatusChip(
                            text = log.status ?: "-",
                            color = if (log.status == "OK") Color(0xFF1F9D68) else Color(0xFFC74444)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        StatusChip("${log.incidentCount} incidents", InciTeamPrimary)
                        StatusChip("${log.assignmentResults.size} results", Color(0xFFD48806))
                    }
                    if (log.assignmentSelections.isNotEmpty()) {
                        LogSection("Selections") {
                            log.assignmentSelections.forEach {
                                Text("${it.incidentNumber ?: "-"} -> ${it.assigneeName ?: "-"}", color = InciTeamInk, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (log.assignmentResults.isNotEmpty()) {
                        LogSection("Assignment Results") {
                            log.assignmentResults.forEach {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(it.incidentNumber ?: "-", color = InciTeamInk, fontWeight = FontWeight.Bold)
                                        Text(it.message ?: "-", color = InciTeamMuted, fontSize = 12.sp)
                                    }
                                    StatusChip(
                                        text = it.status ?: "-",
                                        color = when (it.status) {
                                            "SUCCESS" -> Color(0xFF1F9D68)
                                            "FAILED" -> Color(0xFFC74444)
                                            else -> Color(0xFFD48806)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (log.incidents.isNotEmpty()) {
                        LogSection("Incidents") {
                            log.incidents.sortedBy { it.createdOn ?: "" }.forEach {
                                Text("${it.number ?: "-"} | ${it.priority ?: "-"}", color = InciTeamInk, fontWeight = FontWeight.Bold)
                                Text(it.shortDescription ?: it.configurationItem ?: "-", color = InciTeamMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    feature: InciTeamFeature,
    token: String,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var diagnostics by remember { mutableStateOf<AssignmentDiagnosticsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            diagnostics = apiClient.fetchAssignmentDiagnostics(token)
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    FeatureScaffold(feature, onBack, actions = {
        RefreshButton { refreshKey++ }
    }) {
        StatusContent(loading = loading, error = error, empty = diagnostics == null) {
            val data = diagnostics ?: return@StatusContent
            item {
                MetricGrid(
                    listOf(
                        "Incidents" to data.incidentCount.toString(),
                        "Assignable" to data.assignableCount.toString(),
                        "Skipped" to data.skippedCount.toString()
                    )
                )
            }
            items(data.incidents) { incident ->
                DataCard(title = incident.incidentNumber ?: "Incident") {
                    DetailLine("Status", incident.status ?: "-")
                    DetailLine("CI", incident.configurationItem ?: "-")
                    DetailLine("Priority", incident.priority ?: "-")
                    DetailLine("Reason", incident.reason ?: "-")
                    incident.suggestion?.let {
                        DetailLine("Suggested", listOfNotNull(it.assigneeName, it.geo, it.shift).joinToString(" | "))
                    }
                    incident.candidateChecks.take(5).forEach {
                        DetailLine(
                            it.teamMemberName ?: "Candidate",
                            listOfNotNull(
                                if (it.selected) "selected" else null,
                                if (it.eligible) "eligible" else "not eligible",
                                it.reason
                            ).joinToString(" | ")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamMembersScreen(
    feature: InciTeamFeature,
    token: String,
    user: AuthenticatedUser,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var members by remember { mutableStateOf<List<TeamMemberSummary>>(emptyList()) }
    var geos by remember { mutableStateOf<List<GeoSummary>>(emptyList()) }
    var joinedUsers by remember { mutableStateOf<List<JoinedTeamUserSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<TeamMemberSummary?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            coroutineScope {
                val memberData = async { apiClient.fetchTeamMembers(token) }
                val geoData = async { apiClient.fetchGeos(token) }
                val joinedUserData = async {
                    if (user.canManageCurrentTeam) apiClient.fetchJoinedTeamUsers(token) else emptyList()
                }
                members = memberData.await().sortedBy { it.fullName }
                geos = geoData.await().sortedBy { it.name }
                joinedUsers = joinedUserData.await().sortedBy { it.displayName }
            }
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    if (showCreate || editing != null) {
        TeamMemberDialog(
            member = editing,
            geos = geos,
            joinedUsers = joinedUsers,
            existingMembers = members,
            token = token,
            apiClient = apiClient,
            onDismiss = { showCreate = false; editing = null },
            onSave = { draft ->
                scope.launch {
                    try {
                        if (editing == null) {
                            apiClient.createTeamMember(token, draft.firstName, draft.lastName, draft.email, draft.phone, draft.sysId, draft.geoId)
                        } else {
                            apiClient.updateTeamMember(editing!!.id, token, draft.firstName, draft.lastName, draft.email, draft.phone, draft.sysId, draft.geoId)
                        }
                        message = if (editing == null) "Team member added." else "Team member updated."
                        showCreate = false
                        editing = null
                        refreshKey++
                    } catch (cause: Exception) {
                        message = cause.message
                    }
                }
            }
        )
    }

    FeatureScaffold(feature, onBack, actions = {
        RefreshButton { refreshKey++ }
        if (user.canManageCurrentTeam) {
            Spacer(Modifier.width(8.dp))
            SmallActionButton("Add", Icons.Outlined.Add) { showCreate = true }
        }
    }) {
        StatusContent(loading = loading, error = error, empty = members.isEmpty()) {
            message?.let { item { InfoCard("Team Members", it, InciTeamPrimary) } }
            if (!user.canManageCurrentTeam) {
                item { InfoCard("Read-only", "Team managers and admins can add, update, or remove members.", Color(0xFFD48806)) }
            }
            items(members) { member ->
                DataCard(title = "") {
                    EntityHeader(
                        icon = Icons.Outlined.Groups,
                        title = member.fullName.ifBlank { "Unnamed member" },
                        subtitle = member.geo?.name ?: "No geo assigned",
                        badge = member.email?.takeIf { it.isNotBlank() }?.let { "Linked" },
                        tone = FeatureTone.Slate.color
                    )
                    member.email?.takeIf { it.isNotBlank() }?.let {
                        IconDetailLine(Icons.Outlined.Email, it)
                    }
                    if (user.canManageCurrentTeam) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = member }) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Edit")
                            }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        val response = apiClient.deleteTeamMember(member.id, token)
                                        message = response.message ?: "Team member deleted."
                                        refreshKey++
                                    } catch (cause: Exception) {
                                        message = cause.message
                                    }
                                }
                            }) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleScreen(
    feature: InciTeamFeature,
    token: String,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var schedules by remember { mutableStateOf<List<TeamMemberScheduleSummary>>(emptyList()) }
    var members by remember { mutableStateOf<List<TeamMemberSummary>>(emptyList()) }
    var geos by remember { mutableStateOf<List<GeoSummary>>(emptyList()) }
    var shifts by remember { mutableStateOf<List<ShiftSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<TeamMemberScheduleSummary?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            coroutineScope {
                val scheduleData = async { apiClient.fetchSchedules(token) }
                val memberData = async { apiClient.fetchTeamMembers(token) }
                val geoData = async { apiClient.fetchGeos(token) }
                val shiftData = async { apiClient.fetchShifts(token) }
                schedules = scheduleData.await()
                members = memberData.await()
                geos = geoData.await()
                shifts = shiftData.await()
            }
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    if (showCreate || editing != null) {
        ScheduleDialog(
            schedule = editing,
            members = members,
            geos = geos,
            shifts = shifts,
            onDismiss = { showCreate = false; editing = null },
            onSave = { draft ->
                scope.launch {
                    try {
                        if (editing == null) {
                            apiClient.createSchedule(token, draft.memberIds, draft.geoId, draft.shiftId, draft.startDate, draft.endDate, draft.coverageDays)
                        } else {
                            apiClient.updateSchedule(editing!!.id, token, draft.memberIds, draft.geoId, draft.shiftId, draft.startDate, draft.endDate, draft.coverageDays)
                        }
                        message = if (editing == null) "Schedule created." else "Schedule updated."
                        showCreate = false
                        editing = null
                        refreshKey++
                    } catch (cause: Exception) {
                        message = cause.message
                    }
                }
            }
        )
    }

    FeatureScaffold(feature, onBack, actions = {
        RefreshButton { refreshKey++ }
        Spacer(Modifier.width(8.dp))
        SmallActionButton("Add", Icons.Outlined.Add) { showCreate = true }
    }) {
        StatusContent(loading = loading, error = error, empty = schedules.isEmpty()) {
            message?.let { item { InfoCard("Schedule", it, InciTeamPrimary) } }
            items(schedules) { schedule ->
                DataCard(schedule.teamMember?.fullName ?: "Schedule ${schedule.id}") {
                    StatusChip(schedule.coverageDays?.prettyCoverageDays() ?: "Every day", InciTeamPrimary)
                    DetailLine("Geo / Shift", "${schedule.geo?.name ?: "-"} / ${schedule.shift?.name ?: "-"}")
                    DetailLine("Range", "${displayDate(schedule.startDate)} - ${displayDate(schedule.endDate)}")
                    DetailLine("Duration", durationLabel(schedule.startDate, schedule.endDate))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editing = schedule }) { Text("Edit") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    apiClient.deleteSchedule(schedule.id, token)
                                    message = "Schedule deleted."
                                    refreshKey++
                                } catch (cause: Exception) {
                                    message = cause.message
                                }
                            }
                        }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityEntriesScreen(
    feature: InciTeamFeature,
    kind: String,
    token: String,
    user: AuthenticatedUser,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var entries by remember { mutableStateOf<List<AvailabilityEntrySummary>>(emptyList()) }
    var members by remember { mutableStateOf<List<TeamMemberSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<AvailabilityEntrySummary?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            coroutineScope {
                val memberData = async { apiClient.fetchTeamMembers(token) }
                val entryData = async {
                    if (kind == "leaves") apiClient.fetchLeaveEntries(token) else apiClient.fetchBreakEntries(token)
                }
                members = memberData.await()
                entries = entryData.await()
            }
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    if (showCreate || editing != null) {
        AvailabilityDialog(
            entry = editing,
            members = members,
            title = if (kind == "leaves") "Leave" else "Break",
            onDismiss = { showCreate = false; editing = null },
            onSave = { draft ->
                scope.launch {
                    try {
                        if (editing == null) {
                            apiClient.createAvailabilityEntry(kind, token, draft.memberId, draft.startTs, draft.endTs, draft.reason)
                        } else {
                            apiClient.updateAvailabilityEntry(kind, editing!!.id, token, draft.memberId, draft.startTs, draft.endTs, draft.reason)
                        }
                        message = if (editing == null) "Entry created." else "Entry updated."
                        showCreate = false
                        editing = null
                        refreshKey++
                    } catch (cause: Exception) {
                        message = cause.message
                    }
                }
            }
        )
    }

    FeatureScaffold(feature, onBack, actions = {
        RefreshButton { refreshKey++ }
        if (user.canManageCurrentTeam || true) {
            Spacer(Modifier.width(8.dp))
            SmallActionButton("Add", Icons.Outlined.Add) { showCreate = true }
        }
    }) {
        StatusContent(loading = loading, error = error, empty = entries.isEmpty()) {
            message?.let { item { InfoCard(feature.title, it, InciTeamPrimary) } }
            items(entries) { entry ->
                DataCard(entry.teamMember?.fullName ?: "Entry ${entry.id}") {
                    DetailLine("Start", displayDateTime(entry.startTs))
                    DetailLine("End", displayDateTime(entry.endTs))
                    DetailLine("Reason", entry.reason ?: "-")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editing = entry }) { Text("Edit") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    apiClient.deleteAvailabilityEntry(kind, entry.id, token)
                                    message = "Entry deleted."
                                    refreshKey++
                                } catch (cause: Exception) {
                                    message = cause.message
                                }
                            }
                        }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigurationItemsScreen(
    feature: InciTeamFeature,
    token: String,
    user: AuthenticatedUser,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var items by remember { mutableStateOf<List<ConfigurationItemSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ConfigurationItemSummary?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            items = apiClient.fetchConfigurationItems(token)
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    if (showCreate || editing != null) {
        ConfigurationItemDialog(
            item = editing,
            token = token,
            apiClient = apiClient,
            onDismiss = { showCreate = false; editing = null },
            onSave = { draft ->
                scope.launch {
                    try {
                        if (editing == null) {
                            apiClient.createConfigurationItem(token, draft.name, draft.description, draft.sysId)
                        } else {
                            apiClient.updateConfigurationItem(editing!!.id, token, draft.name, draft.description, draft.sysId)
                        }
                        message = if (editing == null) "CI created." else "CI updated."
                        showCreate = false
                        editing = null
                        refreshKey++
                    } catch (cause: Exception) {
                        message = cause.message
                    }
                }
            }
        )
    }

    FeatureScaffold(feature, onBack, actions = {
        RefreshButton { refreshKey++ }
        if (user.canManageCurrentTeam) {
            Spacer(Modifier.width(8.dp))
            SmallActionButton("Add", Icons.Outlined.Add) { showCreate = true }
        }
    }) {
        StatusContent(loading = loading, error = error, empty = items.isEmpty()) {
            message?.let { item { InfoCard("Configuration Items", it, InciTeamPrimary) } }
            items(items) { item ->
                DataCard("") {
                    EntityHeader(
                        icon = Icons.Outlined.Cloud,
                        title = item.name,
                        subtitle = item.description?.takeIf { it.isNotBlank() } ?: "No description",
                        badge = if (item.serviceNowSysId.isNullOrBlank()) "Needs link" else "Linked",
                        tone = FeatureTone.Slate.color,
                        badgeColor = if (item.serviceNowSysId.isNullOrBlank()) Color(0xFFD48806) else Color(0xFF31C85B)
                    )
                    if (user.canManageCurrentTeam) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = item }) { Text("Edit") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        apiClient.deleteConfigurationItem(item.id, token)
                                        message = "CI deleted."
                                        refreshKey++
                                    } catch (cause: Exception) {
                                        message = cause.message
                                    }
                                }
                            }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CiUserMappingScreen(
    feature: InciTeamFeature,
    token: String,
    user: AuthenticatedUser,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var items by remember { mutableStateOf<List<ConfigurationItemSummary>>(emptyList()) }
    var members by remember { mutableStateOf<List<TeamMemberSummary>>(emptyList()) }
    var mappings by remember { mutableStateOf<List<CiUserMappingSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var mappingEditorInitialItem by remember { mutableStateOf<ConfigurationItemSummary?>(null) }
    var showMappingEditor by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            coroutineScope {
                val itemData = async { apiClient.fetchConfigurationItems(token) }
                val memberData = async { apiClient.fetchTeamMembers(token) }
                val mappingData = async { apiClient.fetchCiUserMappings(token) }
                items = itemData.await()
                members = memberData.await()
                mappings = mappingData.await()
            }
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    if (showMappingEditor) {
        MappingDialog(
            initialItem = mappingEditorInitialItem,
            items = items,
            members = members,
            mappings = mappings,
            onDismiss = {
                showMappingEditor = false
                mappingEditorInitialItem = null
            },
            onSave = { ciId, memberIds ->
                scope.launch {
                    try {
                        apiClient.replaceCiUserMappingsForCi(token, ciId, memberIds)
                        message = "Mapping updated."
                        showMappingEditor = false
                        mappingEditorInitialItem = null
                        refreshKey++
                    } catch (cause: Exception) {
                        message = cause.message
                    }
                }
            }
        )
    }

    FeatureScaffold(feature, onBack, actions = {
        if (user.canManageCurrentTeam) {
            SmallActionButton("Add", Icons.Outlined.Add) {
                mappingEditorInitialItem = null
                showMappingEditor = true
            }
        }
    }) {
        StatusContent(loading = loading, error = error, empty = items.isEmpty()) {
            message?.let { item { InfoCard("CI User Mapping", it, InciTeamPrimary) } }
            items(items) { item ->
                val owners = mappings.filter { it.configurationItem?.id == item.id }.sortedBy { it.sortOrder ?: 0 }
                DataCard(item.name) {
                    StatusChip("${owners.size} owners", InciTeamPrimary)
                    if (owners.isEmpty()) {
                        Text("No owners mapped.", color = InciTeamMuted)
                    } else {
                        owners.forEachIndexed { index, mapping ->
                            OwnerOrderRow(
                                order = index + 1,
                                name = mapping.teamMember?.fullName ?: "Unknown member",
                                geo = mapping.teamMember?.geo?.name
                            )
                        }
                    }
                    if (user.canManageCurrentTeam) {
                        OutlinedButton(onClick = {
                            mappingEditorInitialItem = item
                            showMappingEditor = true
                        }) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (owners.isEmpty()) "Configure" else "Update Order")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountScreen(
    feature: InciTeamFeature,
    token: String,
    user: AuthenticatedUser,
    apiClient: InciTeamApiClient,
    sessionStore: SessionStore,
    onBack: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete account?") },
            text = { Text("This removes your user access, team memberships, mobile device tokens, and linked team member records where applicable.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            apiClient.deleteCurrentAccount(token)
                            sessionStore.signOut()
                        } catch (cause: Exception) {
                            message = cause.message
                            confirmDelete = false
                        }
                    }
                }) { Text("Delete", color = Color(0xFFC74444)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    FeatureScaffold(feature, onBack) {
        item {
            DataCard("Profile") {
                DetailLine("Name", user.displayName)
                DetailLine("Username", user.username)
                DetailLine("Email", user.workEmail ?: "-")
                DetailLine("Role", user.role)
                DetailLine("Workspace", user.workspaceLine)
                message?.let { Text(it, color = Color(0xFFC74444), fontWeight = FontWeight.Bold) }
            }
        }
        item {
            DataCard("Account Deletion") {
                Text(
                    "Deleting your account removes your InciTeam access and linked mobile registration. Existing org/team records remain available to other admins.",
                    color = InciTeamMuted
                )
                Button(onClick = { confirmDelete = true }) {
                    Text("Delete Account")
                }
            }
        }
    }
}

@Composable
private fun UserAccessScreen(
    feature: InciTeamFeature,
    token: String,
    currentUser: AuthenticatedUser,
    apiClient: InciTeamApiClient,
    onBack: () -> Unit
) {
    var users by remember { mutableStateOf<List<UserSummary>>(emptyList()) }
    var teams by remember { mutableStateOf<List<TeamSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            coroutineScope {
                val userData = async { apiClient.fetchUsers(token) }
                val teamData = async { apiClient.fetchWorkspaceTeams(token) }
                users = userData.await()
                teams = teamData.await()
            }
        } catch (cause: Exception) {
            error = cause.message
        } finally {
            loading = false
        }
    }

    FeatureScaffold(feature, onBack, actions = {
        RefreshButton { refreshKey++ }
    }) {
        StatusContent(loading = loading, error = error, empty = users.isEmpty()) {
            message?.let { item { InfoCard("User Access", it, InciTeamPrimary) } }
            if (!currentUser.isGlobalAdmin) {
                item { InfoCard("Admin access", "Only organization admins can update global roles or delete accounts.", Color(0xFFD48806)) }
            }
            items(users) { appUser ->
                DataCard(appUser.displayName) {
                    DetailLine("Username", appUser.username)
                    DetailLine("Email", appUser.workEmail ?: "-")
                    DetailLine("Global role", appUser.role)
                    DetailLine("Current team", appUser.currentTeamName ?: "-")
                    if (currentUser.isGlobalAdmin) {
                        Text("ORG ROLE", color = InciTeamMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        apiClient.updateUserRole(appUser.id, token, "Admin")
                                        message = "Role updated."
                                        refreshKey++
                                    } catch (cause: Exception) {
                                        message = cause.message
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Make Admin") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        apiClient.updateUserRole(appUser.id, token, "User")
                                        message = "Role updated."
                                        refreshKey++
                                    } catch (cause: Exception) {
                                        message = cause.message
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Make User") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        val response = apiClient.deleteUserAccount(appUser.id, token)
                                        message = response.message ?: "Account deleted."
                                        refreshKey++
                                    } catch (cause: Exception) {
                                        message = cause.message
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
                        }
                    }
                    appUser.teamMemberships.forEach { membership ->
                        DetailLine("Team", "${membership.teamName} | ${membership.role}${if (membership.current) " | current" else ""}")
                        Text("TEAM ROLE", color = InciTeamMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MEMBER", "MANAGER", "TEAM_ADMIN").forEach { role ->
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        try {
                                            apiClient.updateUserTeamRole(appUser.id, membership.teamId, token, role)
                                            message = "Team role updated."
                                            refreshKey++
                                        } catch (cause: Exception) {
                                            message = cause.message
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text(role) }
                            }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        apiClient.removeUserFromTeam(appUser.id, membership.teamId, token)
                                        message = "Removed from team."
                                        refreshKey++
                                    } catch (cause: Exception) {
                                        message = cause.message
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Remove") }
                        }
                    }
                    teams.filter { team -> appUser.teamMemberships.none { it.teamId == team.id } }.take(3).forEach { team ->
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    apiClient.assignUserToTeam(appUser.id, team.id, token)
                                    message = "Assigned to ${team.teamName}."
                                    refreshKey++
                                } catch (cause: Exception) {
                                    message = cause.message
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Add to ${team.teamName}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleMessageScreen(feature: InciTeamFeature, onBack: () -> Unit, message: String) {
    FeatureScaffold(feature, onBack) {
        item { InfoCard(feature.title, message, InciTeamPrimary) }
    }
}

@Composable
private fun FeatureScaffold(
    feature: InciTeamFeature,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: LazyListScopeBuilder
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(InciTeamBackgroundTop, Color(0xFFEFF6FF), InciTeamBackgroundBottom)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = InciTeamCard),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = InciTeamInk)
                        }
                    }
                    Text(
                        text = feature.title,
                        modifier = Modifier.weight(1f),
                        color = InciTeamInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.width(112.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions()
                    }
                }
            }
            item {
                ScreenHero(feature)
            }
            content()
        }
    }
}

private typealias LazyListScopeBuilder = androidx.compose.foundation.lazy.LazyListScope.() -> Unit

private fun androidx.compose.foundation.lazy.LazyListScope.StatusContent(
    loading: Boolean,
    error: String?,
    empty: Boolean,
    content: LazyListScopeBuilder
) {
    when {
        loading -> item { InfoCard("Loading", "Fetching latest InciTeam data.", InciTeamPrimary) }
        error != null -> item { InfoCard("Error", error, Color(0xFFC74444)) }
        empty -> item { InfoCard("No records", "Nothing to show yet.", InciTeamMuted) }
        else -> content()
    }
}

@Composable
private fun RefreshButton(onClick: () -> Unit) {
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = InciTeamCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = InciTeamInk)
        }
    }
}

@Composable
private fun PollButton(onClick: () -> Unit) {
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = InciTeamCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Outlined.Cloud, contentDescription = "Poll ServiceNow", tint = InciTeamInk)
        }
    }
}

@Composable
private fun SmallActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Send,
    onClick: () -> Unit
) {
    if (label.equals("Add", ignoreCase = true)) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = InciTeamCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
        ) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = label, tint = InciTeamInk)
            }
        }
        return
    }

    Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = InciTeamCard),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(subtitle, color = InciTeamMuted, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun DataCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = InciTeamCard),
        border = BorderStroke(1.dp, InciTeamBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title.isNotBlank()) {
                Text(title, color = InciTeamPrimaryDeep, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            content()
        }
    }
}

@Composable
private fun ScreenHero(feature: InciTeamFeature) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = InciTeamCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        BoxWithConstraints {
            val compact = maxWidth < 360.dp
            Row(
                modifier = Modifier.padding(if (compact) 16.dp else 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp)
            ) {
                FeatureIconBox(feature, modifier = Modifier.size(if (compact) 52.dp else 58.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        featureHeroTitle(feature),
                        color = InciTeamPrimaryDeep,
                        fontSize = if (compact) 25.sp else 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = if (compact) 29.sp else 33.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        featureHeroSubtitle(feature),
                        color = InciTeamMuted,
                        fontSize = if (compact) 15.sp else 17.sp,
                        lineHeight = if (compact) 21.sp else 23.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureIconBox(feature: InciTeamFeature, modifier: Modifier = Modifier.size(58.dp)) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(feature.tone.color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = featureIcon(feature.id),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun EntityHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    tone: Color = InciTeamPrimary,
    badgeColor: Color = Color(0xFF31C85B)
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tone),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = InciTeamInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = InciTeamMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        if (!badge.isNullOrBlank()) {
            StatusChip(badge, badgeColor)
        }
    }
}

@Composable
private fun IconDetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = InciTeamMuted, modifier = Modifier.size(18.dp))
        Text(text, color = InciTeamMuted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = InciTeamMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(value.ifBlank { "-" }, color = InciTeamInk, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = color,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AvailabilityChip(
    name: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(top = 4.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            name,
            modifier = Modifier.weight(1f),
            color = InciTeamInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            status,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun OwnerOrderRow(order: Int, name: String, geo: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(InciTeamRow)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(InciTeamPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(order.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = InciTeamInk,
            fontWeight = FontWeight.ExtraBold
        )
        Text(geo ?: "-", color = InciTeamMuted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ImpactList(title: String, values: List<String>) {
    DataCard(title) {
        if (values.isEmpty()) {
            Text("None", color = InciTeamMuted)
        } else {
            values.forEach { Text(it, color = InciTeamInk, lineHeight = 21.sp) }
        }
    }
}

@Composable
private fun MetricGrid(values: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = InciTeamRow),
                        border = BorderStroke(1.dp, InciTeamBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(value, color = InciTeamPrimaryDeep, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text(label, color = InciTeamMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TeamMemberDialog(
    member: TeamMemberSummary?,
    geos: List<GeoSummary>,
    joinedUsers: List<JoinedTeamUserSummary>,
    existingMembers: List<TeamMemberSummary>,
    token: String,
    apiClient: InciTeamApiClient,
    onDismiss: () -> Unit,
    onSave: (TeamMemberDraft) -> Unit
) {
    var firstName by remember { mutableStateOf(member?.firstName.orEmpty()) }
    var lastName by remember { mutableStateOf(member?.lastName.orEmpty()) }
    var email by remember { mutableStateOf(member?.email.orEmpty()) }
    var phone by remember { mutableStateOf(member?.phone.orEmpty()) }
    var sysId by remember { mutableStateOf(member?.serviceNowSysId.orEmpty()) }
    var geoId by remember { mutableStateOf(member?.geo?.id ?: geos.firstOrNull()?.id) }
    var selectedJoinedUserId by remember { mutableStateOf<Long?>(null) }
    var searchText by remember {
        mutableStateOf(email.ifBlank { listOf(firstName, lastName).joinToString(" ").trim() })
    }
    var selectedServiceNowLabel by remember {
        mutableStateOf(if (sysId.isBlank()) "" else listOf(firstName, lastName).joinToString(" ").trim())
    }
    var selectedServiceNowSearch by remember { mutableStateOf(searchText) }
    var searchResults by remember { mutableStateOf<List<ServiceNowLookupResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchComplete by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showJoinedUserPicker by remember { mutableStateOf(false) }
    var showGeoPicker by remember { mutableStateOf(false) }
    val selectedJoinedUser = joinedUsers.firstOrNull { it.id == selectedJoinedUserId }
    val selectedGeo = geos.firstOrNull { it.id == geoId }

    LaunchedEffect(searchText, selectedServiceNowSearch, selectedServiceNowLabel) {
        val query = searchText.trim()
        if (selectedServiceNowLabel.isNotBlank() && query == selectedServiceNowSearch) {
            searchResults = emptyList()
            searchComplete = false
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        if (query.length < 2) {
            searchResults = emptyList()
            searchComplete = false
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        searching = true
        searchError = null
        searchComplete = false
        delay(300)
        try {
            val results = apiClient.searchServiceNowUsers(token, query)
            val exact = results.singleOrNull {
                email.isNotBlank() && it.email?.trim()?.equals(email.trim(), ignoreCase = true) == true && sysId.isBlank()
            }
            if (exact != null) {
                val next = exact.email ?: exact.primaryLabel
                sysId = exact.sysId
                selectedServiceNowLabel = exact.primaryLabel
                selectedServiceNowSearch = next
                searchText = next
                if (!exact.email.isNullOrBlank()) email = exact.email
                val parts = exact.primaryLabel.split(" ").filter { it.isNotBlank() }
                if (firstName.isBlank() && parts.isNotEmpty()) firstName = parts.first()
                if (lastName.isBlank() && parts.size > 1) lastName = parts.drop(1).joinToString(" ")
                searchResults = emptyList()
                searchComplete = false
            } else {
                searchResults = results
                searchComplete = true
            }
        } catch (cause: Exception) {
            searchResults = emptyList()
            searchComplete = true
            searchError = cause.message ?: "ServiceNow search failed."
        } finally {
            searching = false
        }
    }

    fun clearServiceNowSelection() {
        sysId = ""
        selectedServiceNowLabel = ""
        selectedServiceNowSearch = ""
        searchComplete = false
        searchError = null
    }

    fun selectServiceNowUser(result: ServiceNowLookupResult) {
        val label = result.primaryLabel
        val nextSearch = result.email ?: label
        sysId = result.sysId
        selectedServiceNowLabel = label
        selectedServiceNowSearch = nextSearch
        searchText = nextSearch
        result.email?.takeIf { it.isNotBlank() }?.let { email = it }
        val parts = label.split(" ").filter { it.isNotBlank() }
        if (firstName.isBlank() && parts.isNotEmpty()) firstName = parts.first()
        if (lastName.isBlank() && parts.size > 1) lastName = parts.drop(1).joinToString(" ")
        searchResults = emptyList()
        searchComplete = false
        searchError = null
    }

    if (showJoinedUserPicker) {
        SelectionDialog(
            title = "Joined InciTeam User",
            onDismiss = { showJoinedUserPicker = false }
        ) {
            SelectionCard(
                title = "Optional",
                subtitle = "Create a standalone roster member",
                selected = selectedJoinedUserId == null,
                onClick = {
                    selectedJoinedUserId = null
                    showJoinedUserPicker = false
                }
            )
            joinedUsers.sortedBy { it.displayName }.forEach { joinedUser ->
                val alreadyMapped = existingMembers.any {
                    it.email?.trim()?.equals(joinedUser.workEmail?.trim().orEmpty(), ignoreCase = true) == true
                }
                SelectionCard(
                    title = joinedUser.displayName,
                    subtitle = listOfNotNull(joinedUser.workEmail, if (alreadyMapped) "already mapped" else null).joinToString(" | "),
                    selected = selectedJoinedUserId == joinedUser.id,
                    onClick = {
                        selectedJoinedUserId = joinedUser.id
                        firstName = joinedUser.firstName.orEmpty()
                        lastName = joinedUser.lastName.orEmpty()
                        email = joinedUser.workEmail.orEmpty()
                        searchText = joinedUser.workEmail ?: joinedUser.displayName
                        clearServiceNowSelection()
                        showJoinedUserPicker = false
                    }
                )
            }
        }
    }

    if (showGeoPicker) {
        SelectionDialog(
            title = "Select Geo",
            onDismiss = { showGeoPicker = false }
        ) {
            geos.sortedBy { it.name }.forEach { geo ->
                SelectionCard(
                    title = geo.name,
                    subtitle = "Team member coverage location",
                    selected = geoId == geo.id,
                    onClick = {
                        geoId = geo.id
                        showGeoPicker = false
                    }
                )
            }
        }
    }

    FormSheet(
        title = if (member == null) "Add Member" else "Edit Member",
        onDismiss = onDismiss,
        saveEnabled = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && sysId.isNotBlank() && geoId != null,
        onSave = { onSave(TeamMemberDraft(firstName, lastName, email, phone.ifBlank { null }, sysId, geoId ?: 0L)) }
    ) {
        if (member == null && joinedUsers.isNotEmpty()) {
            FormSectionTitle("Joined InciTeam User")
            SheetSection {
                PickerRow(
                    label = "User",
                    value = selectedJoinedUser?.displayName ?: "Optional",
                    onClick = { showJoinedUserPicker = true }
                )
            }
        }

        FormSectionTitle("Identity")
        SheetSection {
            SheetTextField("First Name", firstName) { firstName = it }
            FormDivider()
            SheetTextField("Last Name", lastName) { lastName = it }
            FormDivider()
            SheetTextField("Email", email, keyboardType = KeyboardType.Email) {
                email = it
                searchText = it
                clearServiceNowSelection()
            }
            FormDivider()
            SheetTextField("Phone", phone, keyboardType = KeyboardType.Phone) { phone = it }
        }

        FormSectionTitle("Coverage")
        SheetSection {
            PickerRow(
                label = "Geo",
                value = selectedGeo?.name ?: "Select Geo",
                onClick = { showGeoPicker = true }
            )
        }

        FormSectionTitle("ServiceNow Link")
        SheetSection {
            ServiceNowSearchBlock(
                label = "",
                placeholder = "Search by email, name, or ServiceNow username",
                helper = "Select the matching ServiceNow user. InciTeam keeps the ServiceNow link behind the scenes.",
                searchText = searchText,
                onSearchTextChange = {
                    searchText = it
                    if (it != selectedServiceNowSearch) clearServiceNowSelection()
                },
                searching = searching,
                complete = searchComplete,
                error = searchError,
                emptyMessage = "No matching ServiceNow user found. Try the user's email or ServiceNow username.",
                results = searchResults,
                selectedLabel = selectedServiceNowLabel,
                selectedPrefix = "Linked ServiceNow user",
                detailFor = { it.secondaryLabel },
                onSelect = ::selectServiceNowUser
            )
        }
    }
}

@Composable
private fun ScheduleDialog(
    schedule: TeamMemberScheduleSummary?,
    members: List<TeamMemberSummary>,
    geos: List<GeoSummary>,
    shifts: List<ShiftSummary>,
    onDismiss: () -> Unit,
    onSave: (ScheduleDraft) -> Unit
) {
    var memberIds by remember {
        mutableStateOf(schedule?.teamMember?.id?.let { setOf(it) } ?: emptySet<Long>())
    }
    var geoId by remember { mutableStateOf(schedule?.geo?.id ?: geos.firstOrNull()?.id) }
    var shiftId by remember { mutableStateOf(schedule?.shift?.id ?: shifts.firstOrNull()?.id) }
    var startDate by remember { mutableStateOf(schedule?.startDate ?: LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(schedule?.endDate ?: LocalDate.now().plusDays(30).toString()) }
    var days by remember {
        mutableStateOf(
            schedule?.coverageDays?.split(",")?.map { it.trim().uppercase() }?.filter { it.isNotBlank() }?.toSet()
                ?: setOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        )
    }
    var showMemberPicker by remember { mutableStateOf(false) }
    var showGeoPicker by remember { mutableStateOf(false) }
    var showShiftPicker by remember { mutableStateOf(false) }
    val selectedGeo = geos.firstOrNull { it.id == geoId }
    val selectedShift = shifts.firstOrNull { it.id == shiftId }
    val selectedMembers = members.filter { memberIds.contains(it.id) }.sortedBy { it.fullName }
    val filteredMembers = if (geoId == null) members else members.filter { it.geo?.id == geoId }

    if (showGeoPicker) {
        SelectionDialog(
            title = "Select Geo",
            onDismiss = { showGeoPicker = false }
        ) {
            geos.sortedBy { it.name }.forEach { geo ->
                SelectionCard(
                    title = geo.name,
                    subtitle = "Coverage location",
                    selected = geoId == geo.id,
                    onClick = {
                        geoId = geo.id
                        memberIds = memberIds.intersect(members.filter { it.geo?.id == geo.id }.map { it.id }.toSet())
                        showGeoPicker = false
                    }
                )
            }
        }
    }

    if (showShiftPicker) {
        SelectionDialog(
            title = "Select Shift",
            onDismiss = { showShiftPicker = false }
        ) {
            shifts.sortedBy { it.name }.forEach { shift ->
                SelectionCard(
                    title = shift.name,
                    subtitle = listOfNotNull(shift.startTime, shift.endTime).joinToString(" - ").ifBlank { "Shift window" },
                    selected = shiftId == shift.id,
                    onClick = {
                        shiftId = shift.id
                        showShiftPicker = false
                    }
                )
            }
        }
    }

    if (showMemberPicker) {
        SelectionDialog(
            title = if (schedule == null) "Select Team Members" else "Select Team Member",
            onDismiss = { showMemberPicker = false }
        ) {
            if (geoId == null) {
                Text("Select a geo first.", color = InciTeamMuted, fontSize = 17.sp)
            } else if (filteredMembers.isEmpty()) {
                Text("No team members belong to the selected geo.", color = InciTeamMuted, fontSize = 17.sp)
            } else {
                filteredMembers.sortedBy { it.fullName }.forEach { member ->
                    SelectionCard(
                        title = member.fullName,
                        subtitle = member.email ?: member.geo?.name ?: "Team member",
                        selected = memberIds.contains(member.id),
                        onClick = {
                            memberIds = if (schedule == null) {
                                if (memberIds.contains(member.id)) memberIds - member.id else memberIds + member.id
                            } else {
                                setOf(member.id)
                            }
                        }
                    )
                }
            }
        }
    }

    FormSheet(
        title = if (schedule == null) "Add Schedule" else "Edit Schedule",
        onDismiss = onDismiss,
        saveEnabled = memberIds.isNotEmpty() && geoId != null && shiftId != null && startDate.isNotBlank() && endDate.isNotBlank() && days.isNotEmpty(),
        onSave = {
            onSave(
                ScheduleDraft(
                    memberIds = memberIds.toList(),
                    geoId = geoId ?: 0L,
                    shiftId = shiftId ?: 0L,
                    startDate = startDate,
                    endDate = endDate,
                    coverageDays = days.toList()
                )
            )
        }
    ) {
        FormSectionTitle("Coverage")
        SheetSection {
            PickerRow(
                label = "Geo",
                value = selectedGeo?.name ?: "Select Geo",
                onClick = { showGeoPicker = true }
            )
            FormDivider()
            PickerRow(
                label = "Shift",
                value = selectedShift?.name ?: "Select Shift",
                onClick = { showShiftPicker = true }
            )
        }

        FormSectionTitle(if (schedule == null) "Team Members" else "Team Member")
        SheetSection {
            PickerRow(
                label = "Members",
                value = when {
                    selectedMembers.isEmpty() -> "Select Member"
                    selectedMembers.size == 1 -> selectedMembers.first().fullName
                    else -> "${selectedMembers.size} selected"
                },
                onClick = { showMemberPicker = true }
            )
            if (selectedMembers.isNotEmpty()) {
                FormDivider()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedMembers.forEach { selected ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selected.fullName, color = InciTeamInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            Text(selected.geo?.name ?: "-", color = InciTeamMuted, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        FormSectionTitle("Dates")
        SheetSection {
            SheetTextField("Start date yyyy-mm-dd", startDate) { startDate = it }
            FormDivider()
            SheetTextField("End date yyyy-mm-dd", endDate) { endDate = it }
        }

        FormSectionTitle("Repeat")
        SheetSection {
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY").forEachIndexed { index, day ->
                SelectionCard(
                    title = day.lowercase().replaceFirstChar { it.uppercase() },
                    subtitle = "Include this day",
                    selected = days.contains(day),
                    onClick = { days = if (days.contains(day)) days - day else days + day }
                )
                if (index != 6) Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun AvailabilityDialog(
    entry: AvailabilityEntrySummary?,
    members: List<TeamMemberSummary>,
    title: String,
    onDismiss: () -> Unit,
    onSave: (AvailabilityDraft) -> Unit
) {
    var memberId by remember { mutableStateOf(entry?.teamMember?.id) }
    var startTs by remember { mutableStateOf(entry?.startTs ?: "${LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)}") }
    var endTs by remember {
        mutableStateOf(
            entry?.endTs ?: "${LocalDateTime.now().plusHours(if (title == "Leave") 24 else 1).truncatedTo(ChronoUnit.MINUTES)}"
        )
    }
    var reason by remember { mutableStateOf(entry?.reason.orEmpty()) }
    var showMemberPicker by remember { mutableStateOf(false) }
    val selectedMember = members.firstOrNull { it.id == memberId }

    if (showMemberPicker) {
        SelectionDialog(
            title = "Select Member",
            onDismiss = { showMemberPicker = false }
        ) {
            members.sortedBy { it.fullName }.forEach { member ->
                SelectionCard(
                    title = member.fullName,
                    subtitle = member.geo?.name ?: "No geo assigned",
                    selected = memberId == member.id,
                    onClick = {
                        memberId = member.id
                        showMemberPicker = false
                    }
                )
            }
        }
    }

    FormSheet(
        title = if (entry == null) "Add $title" else "Edit $title",
        onDismiss = onDismiss,
        saveEnabled = memberId != null && startTs.isNotBlank() && endTs.isNotBlank(),
        onSave = { onSave(AvailabilityDraft(memberId ?: 0L, startTs, endTs, reason.ifBlank { null })) }
    ) {
        FormSectionTitle("Team Member")
        SheetSection {
            PickerRow(
                label = "Member",
                value = selectedMember?.fullName ?: "Select Member",
                onClick = { showMemberPicker = true }
            )
        }

        FormSectionTitle("Window")
        SheetSection {
            SheetTextField("Start", startTs) { startTs = it }
            FormDivider()
            SheetTextField("End", endTs) { endTs = it }
        }

        FormSectionTitle("Reason")
        SheetSection {
            SheetTextField("Optional", reason, singleLine = false) { reason = it }
        }
    }
}

@Composable
private fun ConfigurationItemDialog(
    item: ConfigurationItemSummary?,
    token: String,
    apiClient: InciTeamApiClient,
    onDismiss: () -> Unit,
    onSave: (ConfigurationItemDraft) -> Unit
) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var description by remember { mutableStateOf(item?.description.orEmpty()) }
    var sysId by remember { mutableStateOf(item?.serviceNowSysId.orEmpty()) }
    var searchText by remember { mutableStateOf(name) }
    var selectedServiceNowLabel by remember { mutableStateOf(if (sysId.isBlank()) "" else name) }
    var selectedServiceNowSearch by remember { mutableStateOf(searchText) }
    var searchResults by remember { mutableStateOf<List<ServiceNowLookupResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchComplete by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(searchText, selectedServiceNowSearch, selectedServiceNowLabel) {
        val query = searchText.trim()
        if (selectedServiceNowLabel.isNotBlank() && query == selectedServiceNowSearch) {
            searchResults = emptyList()
            searchComplete = false
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        if (query.length < 2) {
            searchResults = emptyList()
            searchComplete = false
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        searching = true
        searchError = null
        searchComplete = false
        delay(300)
        try {
            searchResults = apiClient.searchServiceNowConfigurationItems(token, query)
            searchComplete = true
        } catch (cause: Exception) {
            searchResults = emptyList()
            searchComplete = true
            searchError = cause.message ?: "ServiceNow search failed."
        } finally {
            searching = false
        }
    }

    fun clearServiceNowSelection() {
        sysId = ""
        selectedServiceNowLabel = ""
        selectedServiceNowSearch = ""
        searchComplete = false
        searchError = null
    }

    fun selectServiceNowItem(result: ServiceNowLookupResult) {
        val label = result.primaryLabel
        sysId = result.sysId
        name = label
        description = listOfNotNull(result.detail, result.secondaryDetail)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" / ")
        selectedServiceNowLabel = label
        selectedServiceNowSearch = label
        searchText = label
        searchResults = emptyList()
        searchComplete = false
        searchError = null
    }

    FormSheet(
        title = if (item == null) "Add CI" else "Edit CI",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank() && sysId.isNotBlank(),
        onSave = { onSave(ConfigurationItemDraft(name, description.ifBlank { null }, sysId)) }
    ) {
        FormSectionTitle("ServiceNow Link")
        SheetSection {
            ServiceNowSearchBlock(
                label = "",
                placeholder = "Search by CI name, asset tag, or serial number",
                helper = "Select the matching ServiceNow record. InciTeam keeps the ServiceNow link behind the scenes.",
                searchText = searchText,
                onSearchTextChange = {
                    searchText = it
                    if (it != selectedServiceNowSearch) clearServiceNowSelection()
                },
                searching = searching,
                complete = searchComplete,
                error = searchError,
                emptyMessage = "No matching ServiceNow CI found. Try a fuller CI name from ServiceNow.",
                results = searchResults,
                selectedLabel = selectedServiceNowLabel,
                selectedPrefix = "Linked ServiceNow CI",
                detailFor = {
                    listOfNotNull(it.detail, it.secondaryDetail)
                        .map { value -> value.trim() }
                        .filter { value -> value.isNotEmpty() }
                        .joinToString(" / ")
                        .ifBlank { "Configuration item" }
                },
                onSelect = ::selectServiceNowItem
            )
        }

        FormSectionTitle("Details")
        SheetSection {
            SheetTextField("Name", name) {
                name = it
                if (it != selectedServiceNowSearch) clearServiceNowSelection()
            }
            FormDivider()
            SheetTextField("Description", description, singleLine = false) { description = it }
        }
    }
}

@Composable
private fun MappingDialog(
    initialItem: ConfigurationItemSummary?,
    items: List<ConfigurationItemSummary>,
    members: List<TeamMemberSummary>,
    mappings: List<CiUserMappingSummary>,
    onDismiss: () -> Unit,
    onSave: (Long, List<Long>) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<Long?>(initialItem?.id) }
    var memberIds by remember {
        mutableStateOf<List<Long>>(
            initialItem?.let { item ->
                mappings.filter { it.configurationItem?.id == item.id }
                    .sortedBy { it.sortOrder ?: 0 }
                    .mapNotNull { it.teamMember?.id }
            } ?: emptyList()
        )
    }
    var memberToAddId by remember { mutableStateOf<Long?>(null) }
    var showCiPicker by remember { mutableStateOf(false) }
    var showMemberPicker by remember { mutableStateOf(false) }
    val selectedItem = items.firstOrNull { it.id == selectedItemId }
    val availableMembers = members.filter { it.id !in memberIds }.sortedBy { it.fullName }

    if (showCiPicker) {
        SelectionDialog("Select CI", onDismiss = { showCiPicker = false }) {
            items.sortedBy { it.name }.forEach { item ->
                val ownerCount = mappings.count { it.configurationItem?.id == item.id }
                SelectionCard(
                    title = item.name,
                    subtitle = if (ownerCount == 0) "No owners configured" else "$ownerCount owners configured",
                    selected = selectedItemId == item.id,
                    onClick = {
                        selectedItemId = item.id
                        memberIds = mappings.filter { it.configurationItem?.id == item.id }
                            .sortedBy { it.sortOrder ?: 0 }
                            .mapNotNull { it.teamMember?.id }
                        memberToAddId = null
                        showCiPicker = false
                    }
                )
            }
        }
    }

    if (showMemberPicker) {
        SelectionDialog("Select Member", onDismiss = { showMemberPicker = false }) {
            availableMembers.forEach { member ->
                SelectionCard(
                    title = member.fullName,
                    subtitle = member.geo?.name ?: "No geo assigned",
                    selected = memberToAddId == member.id,
                    onClick = {
                        memberToAddId = member.id
                        showMemberPicker = false
                    }
                )
            }
        }
    }

    FormSheet(
        title = "CI Owner Order",
        onDismiss = onDismiss,
        saveEnabled = selectedItemId != null && memberIds.isNotEmpty(),
        onSave = { selectedItemId?.let { onSave(it, memberIds) } }
    ) {
        FormSectionTitle("Configuration Item")
        SheetSection {
            PickerRow("CI", selectedItem?.name ?: "Select CI") { showCiPicker = true }
        }

        FormSectionTitle("Add Owner")
        SheetSection {
            PickerRow(
                label = "Team Member",
                value = memberToAddId?.let { id -> members.firstOrNull { it.id == id }?.fullName } ?: "Select Member",
                onClick = { showMemberPicker = true }
            )
            FormDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = memberToAddId != null) {
                        memberToAddId?.let { id ->
                            if (id !in memberIds) {
                                memberIds = memberIds + id
                            }
                            memberToAddId = null
                        }
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = InciTeamPrimary)
                Text("Add to Order", color = InciTeamInk, fontSize = 20.sp)
            }
        }

        FormSectionTitle("Assignment Order")
        if (memberIds.isEmpty()) {
            SheetSection {
                Text(
                    "Add one or more owners. This order becomes the assignment order for the CI.",
                    color = InciTeamMuted,
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                )
            }
        } else {
            SheetSection {
                memberIds.forEachIndexed { index, memberId ->
                    val member = members.firstOrNull { it.id == memberId }
                    OwnerEditRow(
                        order = index + 1,
                        name = member?.fullName ?: "Unknown member",
                        geo = member?.geo?.name,
                        canMoveUp = index > 0,
                        canMoveDown = index < memberIds.lastIndex,
                        onMoveUp = {
                            memberIds = memberIds.toMutableList().also {
                                val value = it.removeAt(index)
                                it.add(index - 1, value)
                            }
                        },
                        onMoveDown = {
                            memberIds = memberIds.toMutableList().also {
                                val value = it.removeAt(index)
                                it.add(index + 1, value)
                            }
                        },
                        onRemove = { memberIds = memberIds - memberId }
                    )
                    if (index < memberIds.lastIndex) {
                        FormDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MappingPickerDialog(
    items: List<ConfigurationItemSummary>,
    mappings: List<CiUserMappingSummary>,
    onDismiss: () -> Unit,
    onSelect: (ConfigurationItemSummary) -> Unit
) {
    EditDialog(title = "Add CI User Mapping", onDismiss = onDismiss) {
        Text("Configuration Item", color = InciTeamMuted, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        if (items.isEmpty()) {
            Text("Add a configuration item before creating owner order.", color = InciTeamMuted)
        } else {
            items.sortedBy { it.name }.forEach { item ->
                val ownerCount = mappings.count { it.configurationItem?.id == item.id }
                SelectionCard(
                    title = item.name,
                    subtitle = if (ownerCount == 0) "No owners configured" else "$ownerCount owners configured",
                    selected = false,
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun FormSheet(
    title: String,
    onDismiss: () -> Unit,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F5FA))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetPillButton(text = "Cancel", enabled = true, onClick = onDismiss)
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SheetPillButton(text = "Save", enabled = saveEnabled, onClick = onSave)
                }
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SheetPillButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.96f else 0.72f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = if (enabled) Color.Black else Color(0xFF9EA0A7),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 2.dp),
        color = Color(0xFF909199),
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun SheetSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Black, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = if (value.startsWith("Select")) Color(0xFF8F9097) else InciTeamInk,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.15f),
            textAlign = TextAlign.End
        )
        Text("v", color = Color(0xFF8F9097), fontSize = 18.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun SheetTextField(
    placeholder: String,
    value: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = InciTeamInk,
            unfocusedTextColor = InciTeamInk,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = InciTeamPrimary
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3
    )
}

@Composable
private fun FormDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE7E7EC))
    )
}

@Composable
private fun SelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = InciTeamCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(title, color = InciTeamPrimaryDeep, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                content()
            }
        }
    }
}

@Composable
private fun OwnerEditRow(
    order: Int,
    name: String,
    geo: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(InciTeamPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(order.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = InciTeamInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(geo ?: "-", color = InciTeamMuted, fontSize = 13.sp)
        }
        IconButton(enabled = canMoveUp, onClick = onMoveUp) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Move up", tint = InciTeamPrimary)
        }
        IconButton(enabled = canMoveDown, onClick = onMoveDown) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Move down", tint = InciTeamPrimary)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Color(0xFFFF3B45))
        }
    }
}

@Composable
private fun EditDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun FormText(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        colors = formTextFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = false
    )
}

@Composable
private fun formTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = InciTeamInk,
    unfocusedTextColor = InciTeamInk,
    disabledTextColor = InciTeamMuted,
    focusedLabelColor = InciTeamPrimary,
    unfocusedLabelColor = InciTeamMuted,
    cursorColor = InciTeamPrimary,
    focusedBorderColor = InciTeamPrimary,
    unfocusedBorderColor = InciTeamBorder,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) InciTeamPrimary.copy(alpha = 0.10f) else InciTeamRow)
            .border(
                1.dp,
                if (selected) InciTeamPrimary.copy(alpha = 0.5f) else InciTeamBorder.copy(alpha = 0.7f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.Add,
            contentDescription = null,
            tint = if (selected) InciTeamPrimary else InciTeamMuted,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = InciTeamInk, fontWeight = FontWeight.ExtraBold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = InciTeamMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ServiceNowSearchBlock(
    label: String,
    placeholder: String,
    helper: String,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searching: Boolean,
    complete: Boolean,
    error: String?,
    emptyMessage: String,
    results: List<ServiceNowLookupResult>,
    selectedLabel: String,
    selectedPrefix: String,
    detailFor: (ServiceNowLookupResult) -> String,
    onSelect: (ServiceNowLookupResult) -> Unit
) {
    if (label.isNotBlank()) {
        Text(label, color = InciTeamMuted, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        colors = formTextFieldColors(),
        singleLine = true
    )
    Text(helper, color = InciTeamMuted, fontSize = 12.sp, lineHeight = 16.sp)
    when {
        searching -> IconDetailLine(Icons.Outlined.Search, "Searching ServiceNow...")
        !error.isNullOrBlank() -> IconDetailLine(Icons.Outlined.Warning, error)
        complete && selectedLabel.isBlank() && results.isEmpty() -> IconDetailLine(Icons.Outlined.Warning, emptyMessage)
    }
    results.forEach { result ->
        SelectionCard(
            title = result.primaryLabel,
            subtitle = detailFor(result),
            selected = false,
            onClick = { onSelect(result) }
        )
    }
    if (selectedLabel.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF31C85B).copy(alpha = 0.12f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF31C85B), modifier = Modifier.size(18.dp))
            Text("$selectedPrefix: $selectedLabel", color = Color(0xFF1F9D68), fontWeight = FontWeight.ExtraBold)
        }
    }
}

private enum class RosterWindow(val dayCount: Int) {
    Day(1),
    Week(7);

    val label: String
        get() = if (this == Day) "Day" else "Week"
}

@Composable
private fun SegmentedTextRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        options.forEach { option ->
            OutlinedButton(
                onClick = { onSelect(option) },
                border = BorderStroke(1.dp, if (selected == option) InciTeamPrimary else InciTeamBorder)
            ) {
                Text(option.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
private fun LogSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InciTeamRow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title.uppercase(), color = InciTeamMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        content()
    }
}

private fun buildAvailabilityFromSchedules(
    schedules: List<TeamMemberScheduleSummary>,
    dates: List<String>
): List<com.inciteam.app.data.AvailabilityRecord> {
    return schedules.flatMap { schedule ->
        val member = schedule.teamMember ?: return@flatMap emptyList()
        dates.filter { date ->
            schedule.includesDate(date)
        }.map { date ->
            com.inciteam.app.data.AvailabilityRecord(
                tmId = member.id,
                geoName = schedule.geo?.name ?: member.geo?.name ?: "Geo",
                shiftName = schedule.shift?.name ?: "Shift",
                date = date,
                fullName = member.fullName
            )
        }
    }
}

private fun TeamMemberScheduleSummary.includesDate(date: String): Boolean {
    if (startDate.isNotBlank() && date < startDate) {
        return false
    }
    if (endDate.isNotBlank() && date > endDate) {
        return false
    }
    val days = coverageDays?.uppercase().orEmpty()
    if (days.isBlank()) {
        return true
    }
    val dayName = LocalDate.parse(date).dayOfWeek.name
    return days.contains(dayName)
}

private fun AvailabilityEntrySummary.toLeaveRecord(): LeaveRecordSummary {
    return LeaveRecordSummary(
        tmId = teamMember?.id,
        fullName = teamMember?.fullName ?: "Team member",
        geoName = teamMember?.geo?.name,
        shiftName = null,
        startTs = startTs,
        endTs = endTs,
        reason = reason
    )
}

private fun AvailabilityEntrySummary.toBreakRecord(): BreakRecordSummary {
    return BreakRecordSummary(
        tmId = teamMember?.id,
        fullName = teamMember?.fullName ?: "Team member",
        geoName = teamMember?.geo?.name,
        shiftName = null,
        startTs = startTs,
        endTs = endTs,
        reason = reason
    )
}

private fun LeaveRecordSummary.matchesRosterRecord(
    record: com.inciteam.app.data.AvailabilityRecord,
    date: String
): Boolean {
    return ((tmId != null && tmId == record.tmId) || fullName == record.fullName) && date.inDateRange(startTs, endTs)
}

private fun BreakRecordSummary.matchesRosterRecord(
    record: com.inciteam.app.data.AvailabilityRecord,
    date: String
): Boolean {
    return ((tmId != null && tmId == record.tmId) || fullName == record.fullName) && date.inDateRange(startTs, endTs)
}

private fun ServiceNowRunLogSummary.matchesFilters(
    searchText: String,
    statusFilter: String,
    resultFilter: String
): Boolean {
    if (statusFilter != "ALL" && status != statusFilter) {
        return false
    }
    if (resultFilter != "ALL" && assignmentResults.none { it.status == resultFilter }) {
        return false
    }
    val query = searchText.trim().lowercase()
    if (query.isBlank()) {
        return true
    }
    val values = listOf(type, status, message, assignmentConfirmation) +
        incidents.flatMap {
            listOf(it.number, it.configurationItem, it.assignmentGroup, it.caller, it.shortDescription)
        } +
        assignmentResults.flatMap {
            listOf(it.incidentNumber, it.assigneeName, it.status, it.message)
        }
    return values.filterNotNull().any { it.lowercase().contains(query) }
}

private data class RosterData(
    val availability: List<com.inciteam.app.data.AvailabilityRecord>,
    val leaves: List<LeaveRecordSummary>,
    val breaks: List<BreakRecordSummary>
)

private data class SummaryData(
    val health: ServiceNowHealthResponse,
    val validation: ServiceNowValidationResponse,
    val logs: List<ServiceNowRunLogSummary>,
    val coverage: CoverageSummaryResponse,
    val handoff: LeaveHandoffResponse
)

private data class TeamMemberDraft(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val sysId: String,
    val geoId: Long
)

private data class ScheduleDraft(
    val memberIds: List<Long>,
    val geoId: Long,
    val shiftId: Long,
    val startDate: String,
    val endDate: String,
    val coverageDays: List<String>
)

private data class AvailabilityDraft(
    val memberId: Long,
    val startTs: String,
    val endTs: String,
    val reason: String?
)

private data class ConfigurationItemDraft(
    val name: String,
    val description: String?,
    val sysId: String
)

private fun featureHeroTitle(feature: InciTeamFeature): String {
    return when (feature.id) {
        "configuration-items" -> "Configuration Items"
        "summary" -> "Operations Summary"
        "logs" -> "ServiceNow Logs"
        else -> feature.title
    }
}

private fun featureHeroSubtitle(feature: InciTeamFeature): String {
    return when (feature.id) {
        "roster" -> "Schedule-aware availability for your current team."
        "schedule" -> "Shift coverage for your current team."
        "team-members" -> "People in your current team."
        "leaves" -> "Planned absences that affect routing."
        "breaks" -> "Active breaks that affect routing."
        "configuration-items" -> "Supported systems linked from ServiceNow."
        "ci-user-mapping" -> "Ordered ownership for ServiceNow routing."
        "summary" -> "Team health, coverage, and routing risk."
        "logs" -> "Poll history and assignment outcomes."
        "diagnostics" -> "Routing checks for open incidents."
        "account" -> "Profile, privacy, and account deletion."
        "user-access" -> "Roles and team access."
        else -> feature.subtitle
    }
}

private fun featureIcon(featureId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (featureId) {
        "roster" -> Icons.Outlined.Groups
        "schedule" -> Icons.Outlined.CalendarMonth
        "team-members" -> Icons.Outlined.Groups
        "leaves" -> Icons.Outlined.PersonOff
        "breaks" -> Icons.Outlined.Coffee
        "configuration-items" -> Icons.Outlined.Dns
        "ci-user-mapping" -> Icons.Outlined.Tune
        "summary" -> Icons.Outlined.CheckCircle
        "logs" -> Icons.Outlined.Send
        "diagnostics" -> Icons.Outlined.Search
        "account" -> Icons.Outlined.Warning
        "user-access" -> Icons.Outlined.Warning
        else -> Icons.Outlined.CheckCircle
    }
}

private fun rosterWindowTitle(startDate: LocalDate, dates: List<String>, viewMode: RosterWindow): String {
    return if (viewMode == RosterWindow.Week) {
        "${displayDate(startDate.toString())} - ${displayDate(dates.lastOrNull().orEmpty())}"
    } else {
        displayDate(startDate.toString())
    }
}

private fun displayDate(value: String?): String {
    if (value.isNullOrBlank()) {
        return "-"
    }
    return try {
        LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    } catch (_: Exception) {
        value
    }
}

private fun displayDateTime(value: String?): String {
    if (value.isNullOrBlank()) {
        return "-"
    }
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    return try {
        OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(formatter)
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(value.removeSuffix("Z")).format(formatter)
        } catch (_: Exception) {
            value.replace("T", " ").take(19)
        }
    }
}

private fun shortDateTime(value: String?): String = displayDateTime(value)

private fun durationLabel(startDate: String, endDate: String): String {
    return try {
        val days = ChronoUnit.DAYS.between(LocalDate.parse(startDate.take(10)), LocalDate.parse(endDate.take(10))) + 1
        "$days days"
    } catch (_: Exception) {
        "-"
    }
}

private fun String.prettyCoverageDays(): String {
    val days = split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
    val weekdays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
    val allDays = weekdays + listOf("SATURDAY", "SUNDAY")
    return when {
        days.isEmpty() -> "Every day"
        days.toSet() == weekdays.toSet() -> "Weekdays"
        days.toSet() == allDays.toSet() -> "Every day"
        else -> days.joinToString(", ") { it.lowercase().replaceFirstChar { char -> char.uppercase() }.take(3) }
    }
}

private fun String.csvLongs(): List<Long> {
    return split(",").mapNotNull { it.trim().toLongOrNull() }
}

private fun String.inDateRange(startTs: String, endTs: String): Boolean {
    val start = startTs.take(10)
    val end = endTs.take(10)
    return this >= start && this <= end
}
