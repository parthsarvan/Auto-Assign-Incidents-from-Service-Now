package com.example.backend.dto;

public class NotificationSettingsResponse {
    private boolean slackEnabled;
    private boolean emailEnabled;
    private boolean slackWebhookConfigured;
    private boolean emailProviderConfigured;
    private String slackDestination;
    private String emailRecipients;
    private boolean notifyAssignmentSuccess;
    private boolean notifyAssignmentSkipped;
    private boolean notifyUnsupportedCi;
    private boolean notifyPollerFailure;

    public NotificationSettingsResponse() {
    }

    public NotificationSettingsResponse(
            boolean slackEnabled,
            boolean emailEnabled,
            boolean slackWebhookConfigured,
            boolean emailProviderConfigured,
            String slackDestination,
            String emailRecipients,
            boolean notifyAssignmentSuccess,
            boolean notifyAssignmentSkipped,
            boolean notifyUnsupportedCi,
            boolean notifyPollerFailure) {
        this.slackEnabled = slackEnabled;
        this.emailEnabled = emailEnabled;
        this.slackWebhookConfigured = slackWebhookConfigured;
        this.emailProviderConfigured = emailProviderConfigured;
        this.slackDestination = slackDestination;
        this.emailRecipients = emailRecipients;
        this.notifyAssignmentSuccess = notifyAssignmentSuccess;
        this.notifyAssignmentSkipped = notifyAssignmentSkipped;
        this.notifyUnsupportedCi = notifyUnsupportedCi;
        this.notifyPollerFailure = notifyPollerFailure;
    }

    public boolean isSlackEnabled() {
        return slackEnabled;
    }

    public void setSlackEnabled(boolean slackEnabled) {
        this.slackEnabled = slackEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isSlackWebhookConfigured() {
        return slackWebhookConfigured;
    }

    public void setSlackWebhookConfigured(boolean slackWebhookConfigured) {
        this.slackWebhookConfigured = slackWebhookConfigured;
    }

    public boolean isEmailProviderConfigured() {
        return emailProviderConfigured;
    }

    public void setEmailProviderConfigured(boolean emailProviderConfigured) {
        this.emailProviderConfigured = emailProviderConfigured;
    }

    public String getSlackDestination() {
        return slackDestination;
    }

    public void setSlackDestination(String slackDestination) {
        this.slackDestination = slackDestination;
    }

    public String getEmailRecipients() {
        return emailRecipients;
    }

    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }

    public boolean isNotifyAssignmentSuccess() {
        return notifyAssignmentSuccess;
    }

    public void setNotifyAssignmentSuccess(boolean notifyAssignmentSuccess) {
        this.notifyAssignmentSuccess = notifyAssignmentSuccess;
    }

    public boolean isNotifyAssignmentSkipped() {
        return notifyAssignmentSkipped;
    }

    public void setNotifyAssignmentSkipped(boolean notifyAssignmentSkipped) {
        this.notifyAssignmentSkipped = notifyAssignmentSkipped;
    }

    public boolean isNotifyUnsupportedCi() {
        return notifyUnsupportedCi;
    }

    public void setNotifyUnsupportedCi(boolean notifyUnsupportedCi) {
        this.notifyUnsupportedCi = notifyUnsupportedCi;
    }

    public boolean isNotifyPollerFailure() {
        return notifyPollerFailure;
    }

    public void setNotifyPollerFailure(boolean notifyPollerFailure) {
        this.notifyPollerFailure = notifyPollerFailure;
    }
}
