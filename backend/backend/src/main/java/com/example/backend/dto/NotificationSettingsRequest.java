package com.example.backend.dto;

public class NotificationSettingsRequest {
    private boolean slackEnabled;
    private boolean emailEnabled;
    private String slackWebhookUrl;
    private String slackDestination;
    private String emailRecipients;
    private Boolean notifyAssignmentSuccess;
    private Boolean notifyAssignmentSkipped;
    private Boolean notifyUnsupportedCi;
    private Boolean notifyPollerFailure;

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

    public String getSlackWebhookUrl() {
        return slackWebhookUrl;
    }

    public void setSlackWebhookUrl(String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
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

    public Boolean getNotifyAssignmentSuccess() {
        return notifyAssignmentSuccess;
    }

    public void setNotifyAssignmentSuccess(Boolean notifyAssignmentSuccess) {
        this.notifyAssignmentSuccess = notifyAssignmentSuccess;
    }

    public Boolean getNotifyAssignmentSkipped() {
        return notifyAssignmentSkipped;
    }

    public void setNotifyAssignmentSkipped(Boolean notifyAssignmentSkipped) {
        this.notifyAssignmentSkipped = notifyAssignmentSkipped;
    }

    public Boolean getNotifyUnsupportedCi() {
        return notifyUnsupportedCi;
    }

    public void setNotifyUnsupportedCi(Boolean notifyUnsupportedCi) {
        this.notifyUnsupportedCi = notifyUnsupportedCi;
    }

    public Boolean getNotifyPollerFailure() {
        return notifyPollerFailure;
    }

    public void setNotifyPollerFailure(Boolean notifyPollerFailure) {
        this.notifyPollerFailure = notifyPollerFailure;
    }
}
