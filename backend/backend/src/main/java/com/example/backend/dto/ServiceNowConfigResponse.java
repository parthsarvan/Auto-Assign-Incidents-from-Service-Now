package com.example.backend.dto;

import java.time.Instant;

public class ServiceNowConfigResponse {
    private boolean configured;
    private String instanceUrl;
    private String username;
    private Instant connectedAt;

    public ServiceNowConfigResponse() {}

    public ServiceNowConfigResponse(boolean configured, String instanceUrl, String username, Instant connectedAt) {
        this.configured = configured;
        this.instanceUrl = instanceUrl;
        this.username = username;
        this.connectedAt = connectedAt;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }
}
