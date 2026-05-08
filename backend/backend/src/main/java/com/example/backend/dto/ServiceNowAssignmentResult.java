package com.example.backend.dto;

public class ServiceNowAssignmentResult {
    private String incidentNumber;
    private String assigneeName;
    private String assigneeEmail;
    private String geo;
    private String shift;
    private String status;
    private String message;

    public ServiceNowAssignmentResult() {}

    public ServiceNowAssignmentResult(String incidentNumber, String assigneeName, String status, String message) {
        this(incidentNumber, assigneeName, null, null, null, status, message);
    }

    public ServiceNowAssignmentResult(
            String incidentNumber,
            String assigneeName,
            String assigneeEmail,
            String geo,
            String shift,
            String status,
            String message) {
        this.incidentNumber = incidentNumber;
        this.assigneeName = assigneeName;
        this.assigneeEmail = assigneeEmail;
        this.geo = geo;
        this.shift = shift;
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

    public String getAssigneeEmail() {
        return assigneeEmail;
    }

    public void setAssigneeEmail(String assigneeEmail) {
        this.assigneeEmail = assigneeEmail;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
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
