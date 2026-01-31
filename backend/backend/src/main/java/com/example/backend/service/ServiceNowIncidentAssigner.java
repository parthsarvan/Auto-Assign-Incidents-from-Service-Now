package com.example.backend.service;

import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowIncident;
import java.util.List;
import java.util.Optional;
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
    private final SmsService smsService;
    private final boolean assignmentEnabled;

    public ServiceNowIncidentAssigner(
            ServiceNowIncidentClient incidentClient,
            IncidentAssignmentService assignmentService,
            SmsService smsService,
            @Value("${servicenow.assignment.enabled:false}") boolean assignmentEnabled) {
        this.incidentClient = incidentClient;
        this.assignmentService = assignmentService;
        this.smsService = smsService;
        this.assignmentEnabled = assignmentEnabled;
    }

    public List<ServiceNowAssignmentResult> assignIncidents(List<ServiceNowIncident> incidents) {
        if (!assignmentEnabled) {
            logger.info("ServiceNow assignment disabled; skipping incident updates.");
            return List.of();
        }
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }
        return incidents.stream()
                .map(this::assignIncident)
                .collect(Collectors.toList());
    }

    private ServiceNowAssignmentResult assignIncident(ServiceNowIncident incident) {
        Optional<IncidentAssignmentSuggestion> suggestion = assignmentService.suggestAssignee(incident);
        if (suggestion.isEmpty()) {
            return new ServiceNowAssignmentResult(
                    incident.getNumber(), null, "SKIPPED", "No assignment suggestion available.");
        }
        IncidentAssignmentSuggestion selected = suggestion.get();
        if (selected.getAssigneeSysId() == null || selected.getAssigneeSysId().isBlank()) {
            logger.warn("Missing ServiceNow assignee sys_id for incident {}; skipping assignment.", incident.getSys_id());
            return new ServiceNowAssignmentResult(
                    incident.getNumber(),
                    selected.getAssigneeName(),
                    "FAILED",
                    "Missing ServiceNow assignee sys_id.");
        }
        boolean success = incidentClient.assignIncidentBySysId(incident.getSys_id(), selected.getAssigneeSysId());
        if (success) {
            sendAssignmentSms(selected, incident);
        }
        return new ServiceNowAssignmentResult(
                incident.getNumber(),
                selected.getAssigneeName(),
                success ? "SUCCESS" : "FAILED",
                success ? "Assigned successfully." : "ServiceNow assignment failed.");
    }

    private void sendAssignmentSms(IncidentAssignmentSuggestion suggestion, ServiceNowIncident incident) {
        String phone = suggestion.getAssigneePhone();
        if (phone == null || phone.isBlank()) {
            return;
        }
        String message = String.format(
                "ServiceNow incident assigned: %s | CI: %s | Priority: %s | %s",
                incident.getNumber(),
                incident.getCmdb_ci() != null ? incident.getCmdb_ci().getDisplayValue() : "N/A",
                incident.getPriority() != null ? incident.getPriority() : "N/A",
                incident.getShort_description() != null ? incident.getShort_description() : "");
        smsService.sendSms(phone, message);
    }
}
