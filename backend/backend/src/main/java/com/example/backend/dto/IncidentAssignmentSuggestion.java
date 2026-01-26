package com.example.backend.dto;

public class IncidentAssignmentSuggestion {
    private String assigneeName;
    private String assigneeEmail;
    private String geo;
    private String shift;

    public IncidentAssignmentSuggestion() {}

    public IncidentAssignmentSuggestion(String assigneeName, String assigneeEmail, String geo, String shift) {
        this.assigneeName = assigneeName;
        this.assigneeEmail = assigneeEmail;
        this.geo = geo;
        this.shift = shift;
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
