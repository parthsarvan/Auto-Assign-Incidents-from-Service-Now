package com.example.backend.dto;

import java.time.LocalDate;

public class CoverageIssue {
    private String type;
    private String severity;
    private String message;
    private LocalDate date;
    private String geo;
    private String shift;
    private String configurationItem;

    public CoverageIssue() {}

    public CoverageIssue(
            String type,
            String severity,
            String message,
            LocalDate date,
            String geo,
            String shift,
            String configurationItem) {
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.date = date;
        this.geo = geo;
        this.shift = shift;
        this.configurationItem = configurationItem;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public String getConfigurationItem() {
        return configurationItem;
    }

    public void setConfigurationItem(String configurationItem) {
        this.configurationItem = configurationItem;
    }
}
