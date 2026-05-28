package com.example.backend.service;

import com.example.backend.dto.NotificationSettingsRequest;
import com.example.backend.dto.NotificationSettingsResponse;
import com.example.backend.dto.NotificationTestEmailResponse;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamNotificationSettings;
import com.example.backend.repository.TeamNotificationSettingsRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationSettingsService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final TeamNotificationSettingsRepository notificationSettingsRepository;
    private final EmailNotificationSender emailNotificationSender;

    public NotificationSettingsService(
            CurrentWorkspaceService currentWorkspaceService,
            TeamNotificationSettingsRepository notificationSettingsRepository,
            EmailNotificationSender emailNotificationSender) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.emailNotificationSender = emailNotificationSender;
    }

    public NotificationSettingsResponse getSettings() {
        Team team = currentWorkspaceService.getCurrentTeam();
        return notificationSettingsRepository.findByTeam(team)
                .map(this::toResponse)
                .orElseGet(() -> new NotificationSettingsResponse(
                        false,
                        false,
                        false,
                        emailNotificationSender.isProviderConfigured(),
                        emailNotificationSender.isSandboxMode(),
                        null,
                        null,
                        true,
                        true,
                        true,
                        true));
    }

    public NotificationSettingsResponse updateSettings(NotificationSettingsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification settings are required.");
        }

        Team team = currentWorkspaceService.getCurrentTeam();
        Instant now = Instant.now();
        TeamNotificationSettings settings = notificationSettingsRepository.findByTeam(team)
                .orElseGet(() -> {
                    TeamNotificationSettings created = new TeamNotificationSettings();
                    created.setTeam(team);
                    created.setCreatedAt(now);
                    return created;
                });

        settings.setSlackEnabled(request.isSlackEnabled());
        if (request.isSlackEnabled()) {
            String webhookUrl = trimToNull(request.getSlackWebhookUrl());
            if (webhookUrl != null) {
                settings.setSlackWebhookUrl(webhookUrl);
            } else if (!StringUtils.hasText(settings.getSlackWebhookUrl())) {
                throw new IllegalArgumentException("Slack webhook URL is required when Slack notifications are enabled.");
            }
            settings.setSlackDestination(trimToNull(request.getSlackDestination()));
        } else {
            settings.setSlackWebhookUrl(null);
            settings.setSlackDestination(null);
        }

        settings.setEmailEnabled(request.isEmailEnabled());
        if (request.isEmailEnabled()) {
            String recipients = trimToNull(request.getEmailRecipients());
            if (recipients == null) {
                throw new IllegalArgumentException("Email recipients are required when email notifications are enabled.");
            }
            if (emailNotificationSender.isProviderConfigured()) {
                emailNotificationSender.validateSandboxRecipients(emailNotificationSender.parseRecipients(recipients));
            }
            settings.setEmailRecipients(recipients);
        } else {
            settings.setEmailRecipients(null);
        }

        boolean notifyAssignmentSuccess = defaultTrue(request.getNotifyAssignmentSuccess());
        boolean notifyAssignmentSkipped = defaultTrue(request.getNotifyAssignmentSkipped());
        boolean notifyUnsupportedCi = defaultTrue(request.getNotifyUnsupportedCi());
        boolean notifyPollerFailure = defaultTrue(request.getNotifyPollerFailure());
        if ((request.isSlackEnabled() || request.isEmailEnabled())
                && !notifyAssignmentSuccess
                && !notifyAssignmentSkipped
                && !notifyUnsupportedCi
                && !notifyPollerFailure) {
            throw new IllegalArgumentException("Select at least one notification scenario.");
        }
        settings.setNotifyAssignmentSuccess(notifyAssignmentSuccess);
        settings.setNotifyAssignmentSkipped(notifyAssignmentSkipped);
        settings.setNotifyUnsupportedCi(notifyUnsupportedCi);
        settings.setNotifyPollerFailure(notifyPollerFailure);

        settings.setUpdatedAt(now);
        return toResponse(notificationSettingsRepository.save(settings));
    }

    public long countConfiguredForTeam(Team team) {
        return notificationSettingsRepository.findByTeam(team)
                .filter(this::isConfigured)
                .map(settings -> 1L)
                .orElse(0L);
    }

    public NotificationTestEmailResponse sendTestEmail() {
        Team team = currentWorkspaceService.getCurrentTeam();
        TeamNotificationSettings settings = notificationSettingsRepository.findByTeam(team)
                .orElseThrow(() -> new IllegalArgumentException("Save notification settings before sending a test email."));
        if (!settings.isEmailEnabled()) {
            throw new IllegalArgumentException("Enable email notifications before sending a test email.");
        }

        var recipients = emailNotificationSender.parseRecipients(settings.getEmailRecipients());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Email recipients are required before sending a test email.");
        }
        if (!emailNotificationSender.isProviderConfigured()) {
            throw new IllegalArgumentException(
                    "Email delivery is not configured. Set INCITEAM_EMAIL_ENABLED=true and SES SMTP values before sending a test email.");
        }

        String subject = "[InciTeam] Test notification for " + team.getName();
        String body = """
                This is a test email from InciTeam.

                Team: %s

                Email delivery is configured for this team.
                """.formatted(team.getName());
        EmailNotificationSender.EmailSendResult result =
                emailNotificationSender.sendTextEmail(recipients, subject, body);
        return new NotificationTestEmailResponse(result.sent(), result.message(), result.recipients());
    }

    private boolean isConfigured(TeamNotificationSettings settings) {
        boolean hasChannel = (settings.isSlackEnabled() && StringUtils.hasText(settings.getSlackWebhookUrl()))
                || (settings.isEmailEnabled() && StringUtils.hasText(settings.getEmailRecipients()));
        return hasChannel
                && (settings.isNotifyAssignmentSuccess()
                || settings.isNotifyAssignmentSkipped()
                || settings.isNotifyUnsupportedCi()
                || settings.isNotifyPollerFailure());
    }

    private NotificationSettingsResponse toResponse(TeamNotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.isSlackEnabled(),
                settings.isEmailEnabled(),
                StringUtils.hasText(settings.getSlackWebhookUrl()),
                emailNotificationSender.isProviderConfigured(),
                emailNotificationSender.isSandboxMode(),
                settings.getSlackDestination(),
                settings.getEmailRecipients(),
                settings.isNotifyAssignmentSuccess(),
                settings.isNotifyAssignmentSkipped(),
                settings.isNotifyUnsupportedCi(),
                settings.isNotifyPollerFailure());
    }

    private boolean defaultTrue(Boolean value) {
        return value == null || value;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
