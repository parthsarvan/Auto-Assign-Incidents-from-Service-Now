package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "team_notification_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id"}))
public class TeamNotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "slack_enabled", nullable = false)
    private boolean slackEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "slack_webhook_url", length = 2048)
    private String slackWebhookUrl;

    @Column(name = "slack_destination", length = 255)
    private String slackDestination;

    @Column(name = "email_recipients", length = 4000)
    private String emailRecipients;

    @Column(name = "notify_assignment_success", nullable = false)
    private boolean notifyAssignmentSuccess = true;

    @Column(name = "notify_assignment_skipped", nullable = false)
    private boolean notifyAssignmentSkipped = true;

    @Column(name = "notify_unsupported_ci", nullable = false)
    private boolean notifyUnsupportedCi = true;

    @Column(name = "notify_poller_failure", nullable = false)
    private boolean notifyPollerFailure = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
