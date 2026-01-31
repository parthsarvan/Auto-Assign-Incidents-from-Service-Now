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

@Service
public class ServiceNowIncidentPoller {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentPoller.class);

    private final ServiceNowIncidentClient incidentClient;
    private final ServiceNowIncidentAssigner incidentAssigner;
    private final ServiceNowLogService logService;

    public ServiceNowIncidentPoller(
            ServiceNowIncidentClient incidentClient,
            ServiceNowIncidentAssigner incidentAssigner,
            ServiceNowLogService logService) {
        this.incidentClient = incidentClient;
        this.incidentAssigner = incidentAssigner;
        this.logService = logService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        logService.recordStartup();
        logger.info("ServiceNow poller initialized.");
    }

    @Scheduled(fixedDelayString = "${servicenow.poll-interval-ms:300000}")
    public void poll() {
        try {
            List<ServiceNowIncident> incidents = incidentClient.fetchUnassignedIncidents();
            List<ServiceNowAssignmentResult> results = incidentAssigner.assignIncidents(incidents);
            long assignedCount = results.stream()
                    .filter(result -> "SUCCESS".equals(result.getStatus()))
                    .count();
            if (assignedCount > 0) {
                logger.info("ServiceNow assignment applied to {} incidents.", assignedCount);
            }
            logService.recordPollSuccess(incidents, results);
        } catch (Exception ex) {
            logService.recordPollFailure(ex);
        }
    }
}
