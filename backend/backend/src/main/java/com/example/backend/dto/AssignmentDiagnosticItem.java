package com.example.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class AssignmentDiagnosticItem {
    private String incidentNumber;
    private String incidentSysId;
    private String caller;
    private String configurationItem;
    private String priority;
    private String createdOn;
    private String shortDescription;
    private String status;
    private String reason;
    private IncidentAssignmentSuggestion suggestion;
    private List<AssignmentCandidateCheck> candidateChecks = new ArrayList<>();

    public AssignmentDiagnosticItem() {}

    public AssignmentDiagnosticItem(
            String incidentNumber,
            String incidentSysId,
            String caller,
            String configurationItem,
            String priority,
            String createdOn,
            String shortDescription,
            String status,
            String reason,
            IncidentAssignmentSuggestion suggestion,
            List<AssignmentCandidateCheck> candidateChecks) {
        this.incidentNumber = incidentNumber;
        this.incidentSysId = incidentSysId;
        this.caller = caller;
        this.configurationItem = configurationItem;
        this.priority = priority;
        this.createdOn = createdOn;
        this.shortDescription = shortDescription;
        this.status = status;
        this.reason = reason;
        this.suggestion = suggestion;
        this.candidateChecks = candidateChecks;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getIncidentSysId() {
        return incidentSysId;
    }

    public void setIncidentSysId(String incidentSysId) {
        this.incidentSysId = incidentSysId;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getConfigurationItem() {
        return configurationItem;
    }

    public void setConfigurationItem(String configurationItem) {
        this.configurationItem = configurationItem;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public IncidentAssignmentSuggestion getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(IncidentAssignmentSuggestion suggestion) {
        this.suggestion = suggestion;
    }

    public List<AssignmentCandidateCheck> getCandidateChecks() {
        return candidateChecks;
    }

    public void setCandidateChecks(List<AssignmentCandidateCheck> candidateChecks) {
        this.candidateChecks = candidateChecks;
    }
}
