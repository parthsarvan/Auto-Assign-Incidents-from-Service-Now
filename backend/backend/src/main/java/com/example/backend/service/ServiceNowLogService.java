package com.example.backend.service;

import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowIncidentSummary;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.dto.ServiceNowRunLog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceNowLogService {
    private final List<ServiceNowRunLog> logs = new CopyOnWriteArrayList<>();
    private final int retention;
    private final IncidentAssignmentService assignmentService;

    public ServiceNowLogService(
            IncidentAssignmentService assignmentService,
            @Value("${servicenow.log.retention:100}") int retention) {
        this.assignmentService = assignmentService;
        this.retention = retention;
    }

    public void recordStartup() {
        ServiceNowRunLog log = new ServiceNowRunLog(Instant.now(), "STARTUP", "OK", "ServiceNow poller started.");
        addLog(log);
    }

    public void recordPollSuccess(List<ServiceNowIncident> incidents) {
        ServiceNowRunLog log = new ServiceNowRunLog(Instant.now(), "POLL", "OK", "Fetched incidents from ServiceNow.");
        List<ServiceNowIncidentSummary> summaries = incidents.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        log.setIncidents(summaries);
        log.setIncidentCount(summaries.size());
        addLog(log);
    }

    public void recordPollFailure(Exception ex) {
        ServiceNowRunLog log = new ServiceNowRunLog(Instant.now(), "POLL", "ERROR", ex.getMessage());
        log.setIncidentCount(0);
        addLog(log);
    }

    public List<ServiceNowRunLog> getLogs() {
        List<ServiceNowRunLog> snapshot = new ArrayList<>(logs);
        Collections.reverse(snapshot);
        return snapshot;
    }

    private ServiceNowIncidentSummary toSummary(ServiceNowIncident incident) {
        ServiceNowIncidentSummary summary = new ServiceNowIncidentSummary(
                incident.getNumber(),
                incident.getSys_created_on(),
                resolveDisplayValue(incident.getCmdb_ci()),
                incident.getPriority(),
                resolveDisplayValue(incident.getCaller_id()),
                incident.getShort_description());
        assignmentService.suggestAssignee(incident).ifPresent(suggestion -> applySuggestion(summary, suggestion));
        return summary;
    }

    private void applySuggestion(ServiceNowIncidentSummary summary, IncidentAssignmentSuggestion suggestion) {
        summary.setSuggestedAssignee(suggestion.getAssigneeName());
        summary.setSuggestedAssigneeEmail(suggestion.getAssigneeEmail());
        summary.setSuggestedGeo(suggestion.getGeo());
        summary.setSuggestedShift(suggestion.getShift());
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

    private void addLog(ServiceNowRunLog log) {
        logs.add(log);
        if (logs.size() > retention) {
            int overflow = logs.size() - retention;
            for (int i = 0; i < overflow; i++) {
                logs.remove(0);
            }
        }
    }
}
