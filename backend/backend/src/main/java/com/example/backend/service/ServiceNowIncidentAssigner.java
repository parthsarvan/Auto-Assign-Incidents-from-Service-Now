package com.example.backend.service;

import com.example.backend.dto.IncidentAssignmentDecision;
import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowIncident;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceNowIncidentAssigner {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentAssigner.class);

    private final ServiceNowIncidentClient incidentClient;
    private final IncidentAssignmentService assignmentService;
    private final boolean assignmentEnabled;

    public ServiceNowIncidentAssigner(
            ServiceNowIncidentClient incidentClient,
            IncidentAssignmentService assignmentService,
            @Value("${servicenow.assignment.enabled:false}") boolean assignmentEnabled) {
        this.incidentClient = incidentClient;
        this.assignmentService = assignmentService;
        this.assignmentEnabled = assignmentEnabled;
    }

    public List<ServiceNowAssignmentResult> assignIncidents(List<ServiceNowIncident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }
        if (!assignmentEnabled) {
            logger.info("ServiceNow assignment disabled; skipping incident updates.");
            return incidents.stream()
                    .map(incident -> new ServiceNowAssignmentResult(
                            incident.getNumber(),
                            null,
                            null,
                            null,
                            null,
                            "SKIPPED",
                            "Assignment is disabled in configuration."))
                    .collect(Collectors.toList());
        }
        return incidents.stream()
                .map(this::assignIncident)
                .collect(Collectors.toList());
    }

    private ServiceNowAssignmentResult assignIncident(ServiceNowIncident incident) {
        int maxAttempts = Math.max(
                1,
                (int) assignmentService.analyzeAssignment(incident, false).getCandidates().stream()
                        .filter(candidate -> candidate.isEligible())
                        .count());
        List<ServiceNowIncident> blockingIncidents = Collections.emptyList();
        Set<String> blockingIncidentNumbers = new LinkedHashSet<>();
        IncidentAssignmentDecision decision = null;
        IncidentAssignmentSuggestion selected = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            decision = assignmentService.determineAssignment(incident);
            if (!decision.hasSuggestion()) {
                break;
            }
            selected = decision.getSuggestion();
            blockingIncidents = incidentClient.fetchActiveCriticalIncidentsAssignedTo(selected.getAssigneeSysId());
            if (blockingIncidents.isEmpty()) {
                break;
            }
            blockingIncidents.stream()
                    .map(ServiceNowIncident::getNumber)
                    .filter(number -> number != null && !number.isBlank())
                    .forEach(blockingIncidentNumbers::add);
            logger.info(
                    "Skipping selected assignee {} for incident {} because they own active critical incident(s): {}",
                    selected.getAssigneeName(),
                    incident.getNumber(),
                    blockingIncidents.stream().map(ServiceNowIncident::getNumber).collect(Collectors.joining(", ")));
            selected = null;
        }
        if (!decision.hasSuggestion()) {
            return new ServiceNowAssignmentResult(
                    incident.getNumber(), null, null, null, null, "SKIPPED", decision.getReason());
        }
        if (selected == null) {
            return new ServiceNowAssignmentResult(
                    incident.getNumber(),
                    null,
                    null,
                    null,
                    null,
                    "SKIPPED",
                    "All eligible mapped team members are currently handling active P0/P1C incidents: "
                            + String.join(", ", blockingIncidentNumbers)
                            + ".");
        }
        if (selected.getAssigneeSysId() == null || selected.getAssigneeSysId().isBlank()) {
            logger.warn("Missing ServiceNow assignee sys_id for incident {}; skipping assignment.", incident.getSys_id());
            return new ServiceNowAssignmentResult(
                    incident.getNumber(),
                    selected.getAssigneeName(),
                    selected.getAssigneeEmail(),
                    selected.getGeo(),
                    selected.getShift(),
                    "FAILED",
                    "Missing ServiceNow assignee sys_id.");
        }
        boolean success = incidentClient.assignIncidentBySysId(incident.getSys_id(), selected.getAssigneeSysId());
        if (success && selected.getRoutingNote() != null && !selected.getRoutingNote().isBlank()) {
            incidentClient.addWorkNote(incident.getSys_id(), selected.getRoutingNote());
        }
        return new ServiceNowAssignmentResult(
                incident.getNumber(),
                selected.getAssigneeName(),
                selected.getAssigneeEmail(),
                selected.getGeo(),
                selected.getShift(),
                success ? "SUCCESS" : "FAILED",
                success ? buildSuccessMessage(selected) : "ServiceNow assignment failed.");
    }

    private String buildSuccessMessage(IncidentAssignmentSuggestion selected) {
        if (selected.getRoutedTeamName() != null && !selected.getRoutedTeamName().isBlank()) {
            return "Assigned successfully using unsupported-CI handling for " + selected.getRoutedTeamName() + ".";
        }
        return "Assigned successfully.";
    }
}
