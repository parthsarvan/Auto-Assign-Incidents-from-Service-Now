package com.example.backend.dto;

public class ServiceNowAssignmentSelection {
    private String incidentNumber;
    private String assigneeName;
    private String assigneeEmail;
    private String geo;
    private String shift;

    public ServiceNowAssignmentSelection() {}

    public ServiceNowAssignmentSelection(
            String incidentNumber, String assigneeName, String assigneeEmail, String geo, String shift) {
        this.incidentNumber = incidentNumber;
        this.assigneeName = assigneeName;
        this.assigneeEmail = assigneeEmail;
        this.geo = geo;
        this.shift = shift;
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
}
