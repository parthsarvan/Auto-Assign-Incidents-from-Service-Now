package com.example.backend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ServiceNowRunLog {
    private Instant timestamp;
    private Long teamId;
    private String teamName;
    private String type;
    private String status;
    private String message;
    private int incidentCount;
    private List<ServiceNowIncidentSummary> incidents = new ArrayList<>();
    private List<ServiceNowAssignmentSelection> assignmentSelections = new ArrayList<>();
    private List<ServiceNowAssignmentResult> assignmentResults = new ArrayList<>();
    private String assignmentConfirmation;

    public ServiceNowRunLog() {}

    public ServiceNowRunLog(Instant timestamp, String type, String status, String message) {
        this.timestamp = timestamp;
        this.type = type;
        this.status = status;
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(int incidentCount) {
        this.incidentCount = incidentCount;
    }

    public List<ServiceNowIncidentSummary> getIncidents() {
        return incidents;
    }

    public void setIncidents(List<ServiceNowIncidentSummary> incidents) {
        this.incidents = incidents;
    }

    public List<ServiceNowAssignmentSelection> getAssignmentSelections() {
        return assignmentSelections;
    }

    public void setAssignmentSelections(List<ServiceNowAssignmentSelection> assignmentSelections) {
        this.assignmentSelections = assignmentSelections;
    }

    public List<ServiceNowAssignmentResult> getAssignmentResults() {
        return assignmentResults;
    }

    public void setAssignmentResults(List<ServiceNowAssignmentResult> assignmentResults) {
        this.assignmentResults = assignmentResults;
    }

    public String getAssignmentConfirmation() {
        return assignmentConfirmation;
    }

    public void setAssignmentConfirmation(String assignmentConfirmation) {
        this.assignmentConfirmation = assignmentConfirmation;
    }
}
