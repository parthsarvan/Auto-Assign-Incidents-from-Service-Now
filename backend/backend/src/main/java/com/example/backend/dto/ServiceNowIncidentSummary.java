package com.example.backend.dto;

public class ServiceNowIncidentSummary {
    private String number;
    private String createdOn;
    private String configurationItem;
    private String priority;
    private String caller;
    private String shortDescription;

    public ServiceNowIncidentSummary() {}

    public ServiceNowIncidentSummary(
            String number,
            String createdOn,
            String configurationItem,
            String priority,
            String caller,
            String shortDescription) {
        this.number = number;
        this.createdOn = createdOn;
        this.configurationItem = configurationItem;
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
}
