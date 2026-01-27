package com.example.backend.service;

import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowIncident;
import java.util.List;
import java.util.Optional;
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

    public int assignIncidents(List<ServiceNowIncident> incidents) {
        if (!assignmentEnabled) {
            logger.info("ServiceNow assignment disabled; skipping incident updates.");
            return 0;
        }
        if (incidents == null || incidents.isEmpty()) {
            return 0;
        }
        int assignedCount = 0;
        for (ServiceNowIncident incident : incidents) {
            if (assignIncident(incident)) {
                assignedCount++;
            }
        }
        return assignedCount;
    }

    private boolean assignIncident(ServiceNowIncident incident) {
        Optional<IncidentAssignmentSuggestion> suggestion = assignmentService.suggestAssignee(incident);
        if (suggestion.isEmpty()) {
            return false;
        }
        IncidentAssignmentSuggestion selected = suggestion.get();
        if (selected.getAssigneeSysId() == null || selected.getAssigneeSysId().isBlank()) {
            logger.warn("Missing ServiceNow assignee sys_id for incident {}; skipping assignment.", incident.getSys_id());
            return false;
        }
        return incidentClient.assignIncidentBySysId(incident.getSys_id(), selected.getAssigneeSysId());
    }
}
