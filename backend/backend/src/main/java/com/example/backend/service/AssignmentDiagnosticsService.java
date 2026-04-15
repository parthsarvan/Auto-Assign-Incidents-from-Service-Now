package com.example.backend.service;

import com.example.backend.dto.AssignmentDiagnosticItem;
import com.example.backend.dto.AssignmentDiagnosticsResponse;
import com.example.backend.dto.IncidentAssignmentAnalysis;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowReference;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AssignmentDiagnosticsService {
    private final ServiceNowIncidentClient incidentClient;
    private final IncidentAssignmentService assignmentService;

    public AssignmentDiagnosticsService(
            ServiceNowIncidentClient incidentClient,
            IncidentAssignmentService assignmentService) {
        this.incidentClient = incidentClient;
        this.assignmentService = assignmentService;
    }

    public AssignmentDiagnosticsResponse runDiagnostics() {
        List<ServiceNowIncident> incidents = incidentClient.fetchUnassignedIncidents();
        List<AssignmentDiagnosticItem> items = incidents.stream()
                .map(this::toDiagnosticItem)
                .toList();
        int assignableCount = (int) items.stream()
                .filter(item -> "ASSIGNABLE".equals(item.getStatus()))
                .count();
        int skippedCount = items.size() - assignableCount;
        return new AssignmentDiagnosticsResponse(Instant.now(), items.size(), assignableCount, skippedCount, items);
    }

    private AssignmentDiagnosticItem toDiagnosticItem(ServiceNowIncident incident) {
        IncidentAssignmentAnalysis analysis = assignmentService.analyzeAssignment(incident, false);
        return new AssignmentDiagnosticItem(
                incident.getNumber(),
                incident.getSys_id(),
                resolveDisplayValue(incident.getCaller_id()),
                resolveDisplayValue(incident.getCmdb_ci()),
                incident.getPriority(),
                incident.getSys_created_on(),
                incident.getShort_description(),
                analysis.getDecision().hasSuggestion() ? "ASSIGNABLE" : "SKIPPED",
                analysis.getDecision().hasSuggestion() ? "Eligible assignee found." : analysis.getDecision().getReason(),
                analysis.getDecision().getSuggestion(),
                analysis.getCandidates());
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
}
