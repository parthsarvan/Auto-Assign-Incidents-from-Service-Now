package com.example.backend.service;

import com.example.backend.dto.IncidentAssignmentDecision;
import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowAssignmentSelection;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowIncidentSummary;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.dto.ServiceNowRunLog;
import com.example.backend.entity.ServiceNowRunLogEntity;
import com.example.backend.entity.Team;
import com.example.backend.repository.ServiceNowRunLogRepository;
import com.example.backend.repository.TeamRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceNowLogService {
    private final int retention;
    private final IncidentAssignmentService assignmentService;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final ServiceNowRunLogRepository runLogRepository;
    private final TeamRepository teamRepository;
    private final ObjectMapper objectMapper;

    public ServiceNowLogService(
            IncidentAssignmentService assignmentService,
            CurrentWorkspaceService currentWorkspaceService,
            ServiceNowRunLogRepository runLogRepository,
            TeamRepository teamRepository,
            ObjectMapper objectMapper,
            @Value("${servicenow.log.retention:100}") int retention) {
        this.assignmentService = assignmentService;
        this.currentWorkspaceService = currentWorkspaceService;
        this.runLogRepository = runLogRepository;
        this.teamRepository = teamRepository;
        this.objectMapper = objectMapper;
        this.retention = retention;
    }

    public void recordStartup(Team team) {
        ServiceNowRunLog log = new ServiceNowRunLog(Instant.now(), "STARTUP", "OK", "ServiceNow poller started.");
        applyTeam(log, team);
        addLog(log);
    }

    public void recordPollSuccess(Team team, List<ServiceNowIncident> incidents, List<ServiceNowAssignmentResult> results) {
        ServiceNowRunLog log = new ServiceNowRunLog(Instant.now(), "POLL", "OK", "Fetched incidents from ServiceNow.");
        applyTeam(log, team);
        List<ServiceNowIncidentSummary> summaries = incidents.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        log.setIncidents(summaries);
        log.setAssignmentSelections(buildSelections(summaries));
        List<ServiceNowAssignmentResult> assignmentResults =
                results != null ? results : List.of();
        log.setAssignmentResults(assignmentResults);
        log.setAssignmentConfirmation(buildConfirmationMessage(assignmentResults));
        log.setIncidentCount(summaries.size());
        addLog(log);
    }

    public void recordPollFailure(Team team, Exception ex) {
        ServiceNowRunLog log = new ServiceNowRunLog(Instant.now(), "POLL", "ERROR", ex.getMessage());
        applyTeam(log, team);
        log.setIncidentCount(0);
        addLog(log);
    }

    public List<ServiceNowRunLog> getLogs() {
        Team currentTeam = currentWorkspaceService.getCurrentTeam();
        return runLogRepository.findTop100ByTeamOrderByTimestampDesc(currentTeam).stream()
                .map(this::toDto)
                .toList();
    }

    private ServiceNowIncidentSummary toSummary(ServiceNowIncident incident) {
        ServiceNowIncidentSummary summary = new ServiceNowIncidentSummary(
                incident.getNumber(),
                incident.getSys_created_on(),
                resolveDisplayValue(incident.getCmdb_ci()),
                incident.getPriority(),
                resolveDisplayValue(incident.getCaller_id()),
                incident.getShort_description());
        IncidentAssignmentDecision decision = assignmentService.determineAssignment(incident, false);
        if (decision.hasSuggestion()) {
            applySuggestion(summary, decision.getSuggestion());
        }
        return summary;
    }

    private List<ServiceNowAssignmentSelection> buildSelections(List<ServiceNowIncidentSummary> summaries) {
        return summaries.stream()
                .filter(summary -> summary.getSuggestedAssignee() != null && !summary.getSuggestedAssignee().isBlank())
                .map(summary -> new ServiceNowAssignmentSelection(
                        summary.getNumber(),
                        summary.getSuggestedAssignee(),
                        summary.getSuggestedAssigneeEmail(),
                        summary.getSuggestedGeo(),
                        summary.getSuggestedShift()))
                .collect(Collectors.toList());
    }

    private void applySuggestion(ServiceNowIncidentSummary summary, IncidentAssignmentSuggestion suggestion) {
        summary.setSuggestedAssignee(suggestion.getAssigneeName());
        summary.setSuggestedAssigneeEmail(suggestion.getAssigneeEmail());
        summary.setSuggestedGeo(suggestion.getGeo());
        summary.setSuggestedShift(suggestion.getShift());
    }

    private String buildConfirmationMessage(List<ServiceNowAssignmentResult> results) {
        if (results == null || results.isEmpty()) {
            return "Nothing to assign.";
        }
        long successCount = results.stream()
                .filter(result -> "SUCCESS".equals(result.getStatus()))
                .count();
        long failureCount = results.stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .count();
        long skippedCount = results.stream()
                .filter(result -> "SKIPPED".equals(result.getStatus()))
                .count();
        return String.format(
                "Assignments completed: %d success, %d failed, %d skipped.",
                successCount,
                failureCount,
                skippedCount);
    }

    private String resolveDisplayValue(ServiceNowReference reference) {
        if (reference == null) {
            return null;
        }
        if (reference.getDisplayValue() != null && !reference.getDisplayValue().isBlank()) {
            return reference.getDisplayValue();
        }
        return reference.getValue();
    }

    private void applyTeam(ServiceNowRunLog log, Team team) {
        if (team == null) {
            return;
        }
        log.setTeamId(team.getTeam_id());
        log.setTeamName(team.getName());
    }

    private void addLog(ServiceNowRunLog log) {
        Long teamId = log.getTeamId();
        if (teamId == null) {
            return;
        }
        ServiceNowRunLogEntity entity = new ServiceNowRunLogEntity();
        entity.setTimestamp(log.getTimestamp());
        Team team = teamRepository.getReferenceById(teamId);
        entity.setTeam(team);
        entity.setTeamName(log.getTeamName() != null ? log.getTeamName() : team.getName());
        entity.setType(log.getType());
        entity.setStatus(log.getStatus());
        entity.setMessage(log.getMessage());
        entity.setIncidentCount(log.getIncidentCount());
        entity.setIncidentsJson(writeJson(log.getIncidents()));
        entity.setAssignmentSelectionsJson(writeJson(log.getAssignmentSelections()));
        entity.setAssignmentResultsJson(writeJson(log.getAssignmentResults()));
        entity.setAssignmentConfirmation(log.getAssignmentConfirmation());
        runLogRepository.save(entity);

        List<ServiceNowRunLogEntity> teamLogs = runLogRepository.findTop100ByTeamOrderByTimestampDesc(team);
        if (teamLogs.size() > retention) {
            for (int i = retention; i < teamLogs.size(); i++) {
                runLogRepository.delete(teamLogs.get(i));
            }
        }
    }

    private ServiceNowRunLog toDto(ServiceNowRunLogEntity entity) {
        ServiceNowRunLog log = new ServiceNowRunLog(entity.getTimestamp(), entity.getType(), entity.getStatus(), entity.getMessage());
        if (entity.getTeam() != null) {
            log.setTeamId(entity.getTeam().getTeam_id());
            log.setTeamName(entity.getTeamName());
        }
        log.setIncidentCount(entity.getIncidentCount());
        log.setIncidents(readJson(entity.getIncidentsJson(), new TypeReference<List<ServiceNowIncidentSummary>>() {}));
        log.setAssignmentSelections(readJson(entity.getAssignmentSelectionsJson(), new TypeReference<List<ServiceNowAssignmentSelection>>() {}));
        log.setAssignmentResults(readJson(entity.getAssignmentResultsJson(), new TypeReference<List<ServiceNowAssignmentResult>>() {}));
        log.setAssignmentConfirmation(entity.getAssignmentConfirmation());
        return log;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to persist ServiceNow log payload.", ex);
        }
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            return emptyValue(typeReference);
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to read persisted ServiceNow log payload.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T emptyValue(TypeReference<T> typeReference) {
        return (T) new ArrayList<>();
    }
}
