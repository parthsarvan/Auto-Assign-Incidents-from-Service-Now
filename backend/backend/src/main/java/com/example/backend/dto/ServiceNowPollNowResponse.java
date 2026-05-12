package com.example.backend.dto;

import java.time.Instant;

public class ServiceNowPollNowResponse {
    private Instant polledAt;
    private String status;
    private String message;
    private int incidentCount;
    private long successCount;
    private long failedCount;
    private long skippedCount;

    public ServiceNowPollNowResponse() {}

    public ServiceNowPollNowResponse(
            Instant polledAt,
            String status,
            String message,
            int incidentCount,
            long successCount,
            long failedCount,
            long skippedCount) {
        this.polledAt = polledAt;
        this.status = status;
        this.message = message;
        this.incidentCount = incidentCount;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.skippedCount = skippedCount;
    }

    public Instant getPolledAt() {
        return polledAt;
    }

    public void setPolledAt(Instant polledAt) {
        this.polledAt = polledAt;
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

    public int getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(int incidentCount) {
        this.incidentCount = incidentCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public long getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(long skippedCount) {
        this.skippedCount = skippedCount;
    }
}
