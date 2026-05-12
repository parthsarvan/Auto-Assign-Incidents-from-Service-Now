package com.example.backend.dto;

public class LeaveHandoffIncident {
    private String number;
    private String createdOn;
    private String priority;
    private String configurationItem;
    private String assignmentGroup;
    private String shortDescription;

    public LeaveHandoffIncident() {}

    public LeaveHandoffIncident(
            String number,
            String createdOn,
            String priority,
            String configurationItem,
            String assignmentGroup,
            String shortDescription) {
        this.number = number;
        this.createdOn = createdOn;
        this.priority = priority;
        this.configurationItem = configurationItem;
        this.assignmentGroup = assignmentGroup;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
}
