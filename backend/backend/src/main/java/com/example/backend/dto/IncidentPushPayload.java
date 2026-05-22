package com.example.backend.dto;

public class IncidentPushPayload {
    private final String incidentNumber;
    private final String configurationItem;
    private final String priority;
    private final String title;

    public IncidentPushPayload(String incidentNumber, String configurationItem, String priority, String title) {
        this.incidentNumber = incidentNumber;
        this.configurationItem = configurationItem;
        this.priority = priority;
        this.title = title;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public String getConfigurationItem() {
        return configurationItem;
    }

    public String getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }
}
