package com.example.backend.service;

import com.example.backend.dto.IncidentPushPayload;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.entity.User;
import com.example.backend.repository.MobileDeviceTokenRepository;
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

    public IncidentAssignmentNotificationService(
            UserRepository userRepository,
            MobileDeviceTokenRepository tokenRepository,
            ApplePushNotificationService pushNotificationService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.pushNotificationService = pushNotificationService;
    }

    public void notifyAssignmentResults(
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
}
