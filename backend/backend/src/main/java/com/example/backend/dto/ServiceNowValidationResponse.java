package com.example.backend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ServiceNowValidationResponse {
    private Instant checkedAt;
    private boolean valid;
    private String message;
    private int configurationItemCount;
    private int validConfigurationItemCount;
    private int teamMemberCount;
    private int validTeamMemberCount;
    private List<ServiceNowValidationIssue> issues = new ArrayList<>();

    public ServiceNowValidationResponse() {}

    public ServiceNowValidationResponse(
            Instant checkedAt,
            boolean valid,
            String message,
            int configurationItemCount,
            int validConfigurationItemCount,
            int teamMemberCount,
            int validTeamMemberCount,
            List<ServiceNowValidationIssue> issues) {
        this.checkedAt = checkedAt;
        this.valid = valid;
        this.message = message;
        this.configurationItemCount = configurationItemCount;
        this.validConfigurationItemCount = validConfigurationItemCount;
        this.teamMemberCount = teamMemberCount;
        this.validTeamMemberCount = validTeamMemberCount;
        this.issues = issues;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getConfigurationItemCount() {
        return configurationItemCount;
    }

    public void setConfigurationItemCount(int configurationItemCount) {
        this.configurationItemCount = configurationItemCount;
    }

    public int getValidConfigurationItemCount() {
        return validConfigurationItemCount;
    }

    public void setValidConfigurationItemCount(int validConfigurationItemCount) {
        this.validConfigurationItemCount = validConfigurationItemCount;
    }

    public int getTeamMemberCount() {
        return teamMemberCount;
    }

    public void setTeamMemberCount(int teamMemberCount) {
        this.teamMemberCount = teamMemberCount;
    }

    public int getValidTeamMemberCount() {
        return validTeamMemberCount;
    }

    public void setValidTeamMemberCount(int validTeamMemberCount) {
        this.validTeamMemberCount = validTeamMemberCount;
    }

    public List<ServiceNowValidationIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ServiceNowValidationIssue> issues) {
        this.issues = issues;
    }
}
