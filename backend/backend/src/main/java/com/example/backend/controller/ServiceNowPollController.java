package com.example.backend.controller;

import com.example.backend.dto.ServiceNowPollNowResponse;
import com.example.backend.service.ServiceNowIncidentPoller;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicenow")
public class ServiceNowPollController {
    private final ServiceNowIncidentPoller serviceNowIncidentPoller;
    private final WorkspaceAccessService workspaceAccessService;

    public ServiceNowPollController(
            ServiceNowIncidentPoller serviceNowIncidentPoller,
            WorkspaceAccessService workspaceAccessService) {
        this.serviceNowIncidentPoller = serviceNowIncidentPoller;
        this.workspaceAccessService = workspaceAccessService;
    }

    @PostMapping("/poll-now")
    public ServiceNowPollNowResponse pollNow() {
        workspaceAccessService.requireCurrentTeamManager();
        return serviceNowIncidentPoller.pollCurrentTeamNow();
    }
}
