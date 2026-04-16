package com.example.backend.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import com.example.backend.dto.ServiceNowAssignmentResult;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.entity.Team;
import com.example.backend.repository.TeamRepository;

@Service
public class ServiceNowIncidentPoller {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentPoller.class);

    private final ServiceNowIncidentClient incidentClient;
    private final ServiceNowIncidentAssigner incidentAssigner;
    private final ServiceNowLogService logService;
    private final TeamRepository teamRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;

    public ServiceNowIncidentPoller(
            ServiceNowIncidentClient incidentClient,
            ServiceNowIncidentAssigner incidentAssigner,
            ServiceNowLogService logService,
            TeamRepository teamRepository,
            CurrentWorkspaceService currentWorkspaceService,
            OrganizationServiceNowConfigService organizationServiceNowConfigService) {
        this.incidentClient = incidentClient;
        this.incidentAssigner = incidentAssigner;
        this.logService = logService;
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
                if (!organizationServiceNowConfigService.isConfiguredForTeam(team)) {
                    logger.info("Skipping ServiceNow polling for team {} because the organization is not connected yet.", team.getName());
                    return;
                }
                try {
                    List<ServiceNowIncident> incidents = incidentClient.fetchUnassignedIncidents();
                    List<ServiceNowAssignmentResult> results = incidentAssigner.assignIncidents(incidents);
                    long assignedCount = results.stream()
                            .filter(result -> "SUCCESS".equals(result.getStatus()))
                            .count();
                    if (assignedCount > 0) {
                        logger.info("ServiceNow assignment applied to {} incidents for team {}.", assignedCount, team.getName());
                    }
                    logService.recordPollSuccess(team, incidents, results);
                } catch (Exception ex) {
                    logger.error("ServiceNow polling failed for team {}: {}", team.getName(), ex.getMessage());
                    logService.recordPollFailure(team, ex);
                }
            });
        }
    }
}
