package com.example.backend.dto;

public class ServiceNowAssignmentResult {
    private String incidentNumber;
    private String assigneeName;
    private String status;
    private String message;

    public ServiceNowAssignmentResult() {}

    public ServiceNowAssignmentResult(String incidentNumber, String assigneeName, String status, String message) {
        this.incidentNumber = incidentNumber;
        this.assigneeName = assigneeName;
        this.status = status;
        this.message = message;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
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
}
