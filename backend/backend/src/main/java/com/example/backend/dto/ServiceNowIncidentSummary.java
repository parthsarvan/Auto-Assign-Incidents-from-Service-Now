package com.example.backend.dto;

public class ServiceNowIncidentSummary {
    private String number;
    private String createdOn;
    private String configurationItem;
    private String assignmentGroup;
    private String priority;
    private String caller;
    private String shortDescription;
    private String suggestedAssignee;
    private String suggestedAssigneeEmail;
    private String suggestedGeo;
    private String suggestedShift;

    public ServiceNowIncidentSummary() {}

    public ServiceNowIncidentSummary(
            String number,
            String createdOn,
            String configurationItem,
            String assignmentGroup,
            String priority,
            String caller,
            String shortDescription) {
        this.number = number;
        this.createdOn = createdOn;
        this.configurationItem = configurationItem;
        this.assignmentGroup = assignmentGroup;
        this.priority = priority;
        this.caller = caller;
        this.shortDescription = shortDescription;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getConfigurationItem() {
        return configurationItem;
    }

    public void setConfigurationItem(String configurationItem) {
        this.configurationItem = configurationItem;
    }

    public String getAssignmentGroup() {
        return assignmentGroup;
    }

    public void setAssignmentGroup(String assignmentGroup) {
        this.assignmentGroup = assignmentGroup;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getSuggestedAssignee() {
        return suggestedAssignee;
    }

    public void setSuggestedAssignee(String suggestedAssignee) {
        this.suggestedAssignee = suggestedAssignee;
    }

    public String getSuggestedAssigneeEmail() {
        return suggestedAssigneeEmail;
    }

    public void setSuggestedAssigneeEmail(String suggestedAssigneeEmail) {
        this.suggestedAssigneeEmail = suggestedAssigneeEmail;
    }

    public String getSuggestedGeo() {
        return suggestedGeo;
    }

    public void setSuggestedGeo(String suggestedGeo) {
        this.suggestedGeo = suggestedGeo;
    }

    public String getSuggestedShift() {
        return suggestedShift;
    }

    public void setSuggestedShift(String suggestedShift) {
        this.suggestedShift = suggestedShift;
    }
}
