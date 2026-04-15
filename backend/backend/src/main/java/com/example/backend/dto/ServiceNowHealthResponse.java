package com.example.backend.dto;

import java.time.Instant;

public class ServiceNowHealthResponse {
    private Instant checkedAt;
    private boolean healthy;
    private String status;
    private String message;
    private String instanceUrl;
    private Instant lastPollAt;
    private String lastPollStatus;
    private String lastPollMessage;

    public ServiceNowHealthResponse() {}

    public ServiceNowHealthResponse(
            Instant checkedAt,
            boolean healthy,
            String status,
            String message,
            String instanceUrl,
            Instant lastPollAt,
            String lastPollStatus,
            String lastPollMessage) {
        this.checkedAt = checkedAt;
        this.healthy = healthy;
        this.status = status;
        this.message = message;
        this.instanceUrl = instanceUrl;
        this.lastPollAt = lastPollAt;
        this.lastPollStatus = lastPollStatus;
        this.lastPollMessage = lastPollMessage;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
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

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public Instant getLastPollAt() {
        return lastPollAt;
    }

    public void setLastPollAt(Instant lastPollAt) {
        this.lastPollAt = lastPollAt;
    }

    public String getLastPollStatus() {
        return lastPollStatus;
    }

    public void setLastPollStatus(String lastPollStatus) {
        this.lastPollStatus = lastPollStatus;
    }

    public String getLastPollMessage() {
        return lastPollMessage;
    }

    public void setLastPollMessage(String lastPollMessage) {
        this.lastPollMessage = lastPollMessage;
    }
}
