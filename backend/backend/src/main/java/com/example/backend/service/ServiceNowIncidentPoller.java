package com.example.backend.service;

import java.util.List;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowPollNowResponse;
import com.example.backend.entity.Team;
import com.example.backend.repository.TeamRepository;

@Service
public class ServiceNowIncidentPoller {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentPoller.class);

    private final ServiceNowIncidentClient incidentClient;
    private final ServiceNowIncidentAssigner incidentAssigner;
    private final ServiceNowLogService logService;
    private final IncidentAssignmentNotificationService notificationService;
    private final TeamRepository teamRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;

    public ServiceNowIncidentPoller(
            ServiceNowIncidentClient incidentClient,
            ServiceNowIncidentAssigner incidentAssigner,
            ServiceNowLogService logService,
            IncidentAssignmentNotificationService notificationService,
            TeamRepository teamRepository,
            CurrentWorkspaceService currentWorkspaceService,
            OrganizationServiceNowConfigService organizationServiceNowConfigService) {
        this.incidentClient = incidentClient;
        this.incidentAssigner = incidentAssigner;
        this.logService = logService;
        this.notificationService = notificationService;
        this.teamRepository = teamRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.organizationServiceNowConfigService = organizationServiceNowConfigService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        for (Team team : teamRepository.findAll()) {
            logService.recordStartup(team);
        }
        logger.info("ServiceNow poller initialized.");
    }

    @Scheduled(fixedDelayString = "${servicenow.poll-interval-ms:300000}")
    public void poll() {
        for (Team team : teamRepository.findAll()) {
            currentWorkspaceService.runInTeam(team, () -> {
                pollTeam(team);
            });
        }
    }

    public ServiceNowPollNowResponse pollCurrentTeamNow() {
        Team team = currentWorkspaceService.getCurrentTeam();
        return pollTeam(team);
    }

    private ServiceNowPollNowResponse pollTeam(Team team) {
        if (!organizationServiceNowConfigService.isConfiguredForTeam(team)) {
            String message = "ServiceNow is not connected yet for this team.";
            logger.info("Skipping ServiceNow polling for team {} because the organization is not connected yet.", team.getName());
            return new ServiceNowPollNowResponse(Instant.now(), "SKIPPED", message, 0, 0, 0, 0);
        }
        try {
            List<ServiceNowIncident> incidents = incidentClient.fetchUnassignedIncidents();
            List<ServiceNowAssignmentResult> results = incidentAssigner.assignIncidents(incidents);
            long successCount = countByStatus(results, "SUCCESS");
            long failedCount = countByStatus(results, "FAILED");
            long skippedCount = countByStatus(results, "SKIPPED");
            if (successCount > 0) {
                logger.info("ServiceNow assignment applied to {} incidents for team {}.", successCount, team.getName());
            }
            logService.recordPollSuccess(team, incidents, results);
            notificationService.notifyAssignmentResults(incidents, results);
            return new ServiceNowPollNowResponse(
                    Instant.now(),
                    "OK",
                    String.format(
                            "Poll completed: %d fetched, %d assigned, %d failed, %d skipped.",
                            incidents.size(),
                            successCount,
                            failedCount,
                            skippedCount),
                    incidents.size(),
                    successCount,
                    failedCount,
                    skippedCount);
        } catch (Exception ex) {
            logger.error("ServiceNow polling failed for team {}: {}", team.getName(), ex.getMessage());
            logService.recordPollFailure(team, ex);
            return new ServiceNowPollNowResponse(
                    Instant.now(),
                    "ERROR",
                    ex.getMessage(),
                    0,
                    0,
                    0,
                    0);
        }
    }

    private long countByStatus(List<ServiceNowAssignmentResult> results, String status) {
        return results.stream()
                .filter(result -> status.equals(result.getStatus()))
                .count();
    }
}
