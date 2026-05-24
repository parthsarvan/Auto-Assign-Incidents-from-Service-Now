package com.example.backend.service;

import com.example.backend.dto.IncidentPushPayload;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamNotificationSettings;
import com.example.backend.entity.User;
import com.example.backend.repository.MobileDeviceTokenRepository;
import com.example.backend.repository.TeamNotificationSettingsRepository;
import com.example.backend.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IncidentAssignmentNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(IncidentAssignmentNotificationService.class);

    private final UserRepository userRepository;
    private final MobileDeviceTokenRepository tokenRepository;
    private final ApplePushNotificationService pushNotificationService;
    private final TeamNotificationSettingsRepository notificationSettingsRepository;
    private final EmailNotificationSender emailNotificationSender;

    public IncidentAssignmentNotificationService(
            UserRepository userRepository,
            MobileDeviceTokenRepository tokenRepository,
            ApplePushNotificationService pushNotificationService,
            TeamNotificationSettingsRepository notificationSettingsRepository,
            EmailNotificationSender emailNotificationSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.pushNotificationService = pushNotificationService;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.emailNotificationSender = emailNotificationSender;
    }

    public void notifyAssignmentResults(
            Team team,
            List<ServiceNowIncident> incidents,
            List<ServiceNowAssignmentResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        Map<String, ServiceNowIncident> incidentsByNumber = (incidents != null ? incidents : List.<ServiceNowIncident>of()).stream()
                .filter(incident -> incident.getNumber() != null && !incident.getNumber().isBlank())
                .collect(Collectors.toMap(
                        incident -> incident.getNumber().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left));

        notifyPushAssignmentResults(incidentsByNumber, results);
        notifyEmailAssignmentResults(team, incidentsByNumber, results);
    }

    public void notifyPollFailure(Team team, Exception failure) {
        try {
            TeamNotificationSettings settings = findEmailSettings(team);
            if (settings == null || !settings.isNotifyPollerFailure()) {
                return;
            }
            String subject = "[InciTeam] ServiceNow poller failed for " + team.getName();
            String body = """
                    InciTeam could not complete a ServiceNow poll.

                    Team: %s
                    Error: %s

                    Check the ServiceNow connection, credentials, assignment groups, and backend logs.
                    """.formatted(team.getName(), failure != null ? failure.getMessage() : "Unknown error");
            sendEmail(settings, subject, body);
        } catch (Exception ex) {
            logger.warn("Failed to send poller failure notification for team {}: {}", team.getName(), ex.getMessage());
        }
    }

    private void notifyPushAssignmentResults(
            Map<String, ServiceNowIncident> incidentsByNumber,
            List<ServiceNowAssignmentResult> results) {
        for (ServiceNowAssignmentResult result : results) {
            if (!"SUCCESS".equals(result.getStatus())) {
                continue;
            }
            String assigneeEmail = normalizeEmail(result.getAssigneeEmail());
            if (assigneeEmail.isBlank()) {
                continue;
            }
            try {
                User user = userRepository.findByNormalizedWorkEmail(assigneeEmail).orElse(null);
                if (user == null) {
                    logger.debug("No InciTeam user matched assignment email {}; skipping push.", assigneeEmail);
                    continue;
                }
                ServiceNowIncident incident = incidentsByNumber.get(
                        Objects.toString(result.getIncidentNumber(), "").toLowerCase(Locale.ROOT));
                IncidentPushPayload payload = buildPayload(result, incident);
                tokenRepository.findAllByUserAndActiveTrue(user)
                        .forEach(token -> pushNotificationService.sendIncidentAssigned(token, payload));
            } catch (Exception ex) {
                logger.warn(
                        "Failed to dispatch assignment notification for incident {}: {}",
                        result.getIncidentNumber(),
                        ex.getMessage());
            }
        }
    }

    private void notifyEmailAssignmentResults(
            Team team,
            Map<String, ServiceNowIncident> incidentsByNumber,
            List<ServiceNowAssignmentResult> results) {
        try {
            TeamNotificationSettings settings = findEmailSettings(team);
            if (settings == null) {
                return;
            }

            for (ServiceNowAssignmentResult result : results) {
                NotificationEventType eventType = classifyResult(result);
                if (eventType == null || !isEventEnabled(settings, eventType)) {
                    continue;
                }
                ServiceNowIncident incident = incidentsByNumber.get(
                        Objects.toString(result.getIncidentNumber(), "").toLowerCase(Locale.ROOT));
                sendEmail(settings, buildSubject(team, eventType, result), buildBody(team, eventType, result, incident));
            }
        } catch (Exception ex) {
            logger.warn("Failed to send assignment email notification for team {}: {}", team.getName(), ex.getMessage());
        }
    }

    private TeamNotificationSettings findEmailSettings(Team team) {
        if (team == null || !emailNotificationSender.isProviderConfigured()) {
            return null;
        }
        return notificationSettingsRepository.findByTeam(team)
                .filter(TeamNotificationSettings::isEmailEnabled)
                .filter(settings -> emailNotificationSender.parseRecipients(settings.getEmailRecipients()).size() > 0)
                .orElse(null);
    }

    private void sendEmail(TeamNotificationSettings settings, String subject, String body) {
        emailNotificationSender.sendTextEmail(
                emailNotificationSender.parseRecipients(settings.getEmailRecipients()),
                subject,
                body);
    }

    private NotificationEventType classifyResult(ServiceNowAssignmentResult result) {
        if (result == null) {
            return null;
        }
        if (isUnsupportedCiResult(result)) {
            return NotificationEventType.UNSUPPORTED_CI;
        }
        if ("SUCCESS".equals(result.getStatus())) {
            return NotificationEventType.ASSIGNMENT_SUCCESS;
        }
        if ("SKIPPED".equals(result.getStatus()) && isAvailabilitySkipped(result.getMessage())) {
            return NotificationEventType.ASSIGNMENT_SKIPPED;
        }
        return null;
    }

    private boolean isEventEnabled(TeamNotificationSettings settings, NotificationEventType eventType) {
        return switch (eventType) {
            case ASSIGNMENT_SUCCESS -> settings.isNotifyAssignmentSuccess();
            case ASSIGNMENT_SKIPPED -> settings.isNotifyAssignmentSkipped();
            case UNSUPPORTED_CI -> settings.isNotifyUnsupportedCi();
        };
    }

    private String buildSubject(Team team, NotificationEventType eventType, ServiceNowAssignmentResult result) {
        String incidentNumber = Objects.toString(result.getIncidentNumber(), "incident");
        return switch (eventType) {
            case ASSIGNMENT_SUCCESS -> "[InciTeam] Assignment completed: " + incidentNumber;
            case ASSIGNMENT_SKIPPED -> "[InciTeam] Assignment skipped: " + incidentNumber;
            case UNSUPPORTED_CI -> "[InciTeam] Unsupported CI incident: " + incidentNumber;
        };
    }

    private String buildBody(
            Team team,
            NotificationEventType eventType,
            ServiceNowAssignmentResult result,
            ServiceNowIncident incident) {
        String eventLabel = switch (eventType) {
            case ASSIGNMENT_SUCCESS -> "Assignment success";
            case ASSIGNMENT_SKIPPED -> "Assignment skipped because mapped team members are busy or unavailable";
            case UNSUPPORTED_CI -> "Unsupported CI incident";
        };
        String assignee = result.getAssigneeName() != null && !result.getAssigneeName().isBlank()
                ? result.getAssigneeName()
                : "-";
        String assigneeEmail = result.getAssigneeEmail() != null && !result.getAssigneeEmail().isBlank()
                ? result.getAssigneeEmail()
                : "-";
        return """
                InciTeam notification: %s

                Team: %s
                Incident: %s
                Status: %s
                Message: %s
                Assignee: %s
                Assignee email: %s
                Geo / shift: %s / %s
                CI: %s
                Priority: %s
                Short description: %s
                """.formatted(
                eventLabel,
                team.getName(),
                Objects.toString(result.getIncidentNumber(), "-"),
                Objects.toString(result.getStatus(), "-"),
                Objects.toString(result.getMessage(), "-"),
                assignee,
                assigneeEmail,
                Objects.toString(result.getGeo(), "-"),
                Objects.toString(result.getShift(), "-"),
                incident != null ? resolveDisplayValue(incident.getCmdb_ci()) : "-",
                incident != null ? Objects.toString(incident.getPriority(), "-") : "-",
                incident != null ? Objects.toString(incident.getShort_description(), "-") : "-");
    }

    private boolean isUnsupportedCiResult(ServiceNowAssignmentResult result) {
        String message = normalizeMessage(result.getMessage());
        return message.contains("ci not configured")
                || message.contains("unsupported-ci")
                || message.contains("unsupported ci")
                || message.contains("fallback triage owner")
                || message.contains("owning team was found");
    }

    private boolean isAvailabilitySkipped(String message) {
        String normalized = normalizeMessage(message);
        return normalized.contains("no eligible mapped team member is available")
                || normalized.contains("all eligible mapped team members are currently handling");
    }

    private String normalizeMessage(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private IncidentPushPayload buildPayload(ServiceNowAssignmentResult result, ServiceNowIncident incident) {
        return new IncidentPushPayload(
                result.getIncidentNumber(),
                incident != null ? resolveDisplayValue(incident.getCmdb_ci()) : null,
                incident != null ? incident.getPriority() : null,
                incident != null ? incident.getShort_description() : null);
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveDisplayValue(ServiceNowReference reference) {
        if (reference == null) {
            return null;
        }
        if (reference.getDisplayValue() != null && !reference.getDisplayValue().isBlank()) {
            return reference.getDisplayValue();
        }
        return reference.getValue();
    }

    private enum NotificationEventType {
        ASSIGNMENT_SUCCESS,
        ASSIGNMENT_SKIPPED,
        UNSUPPORTED_CI
    }
}
