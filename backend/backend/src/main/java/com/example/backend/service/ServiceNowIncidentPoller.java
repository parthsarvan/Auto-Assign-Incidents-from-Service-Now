package com.example.backend.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import com.example.backend.dto.ServiceNowIncident;

@Service
public class ServiceNowIncidentPoller {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentPoller.class);

    private final ServiceNowIncidentClient incidentClient;
    private final ServiceNowLogService logService;

    public ServiceNowIncidentPoller(ServiceNowIncidentClient incidentClient, ServiceNowLogService logService) {
        this.incidentClient = incidentClient;
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
            logService.recordPollSuccess(incidents);
        } catch (Exception ex) {
            logService.recordPollFailure(ex);
        }
    }
}
