package com.example.backend.service;

import com.example.backend.dto.IncidentAssignmentDecision;
import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowIncident;
import java.util.List;
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
        IncidentAssignmentDecision decision = assignmentService.determineAssignment(incident);
        if (!decision.hasSuggestion()) {
            return new ServiceNowAssignmentResult(
                    incident.getNumber(), null, null, null, null, "SKIPPED", decision.getReason());
        }
        IncidentAssignmentSuggestion selected = decision.getSuggestion();
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
        return new ServiceNowAssignmentResult(
                incident.getNumber(),
                selected.getAssigneeName(),
                selected.getAssigneeEmail(),
                selected.getGeo(),
                selected.getShift(),
                success ? "SUCCESS" : "FAILED",
                success ? "Assigned successfully." : "ServiceNow assignment failed.");
    }
}
