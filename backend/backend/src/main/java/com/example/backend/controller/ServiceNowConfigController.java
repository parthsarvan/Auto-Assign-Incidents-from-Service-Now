package com.example.backend.controller;

import com.example.backend.dto.ServiceNowConfigRequest;
import com.example.backend.dto.ServiceNowConfigResponse;
import com.example.backend.service.OrganizationServiceNowConfigService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicenow")
public class ServiceNowConfigController {
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;
    private final WorkspaceAccessService workspaceAccessService;

    public ServiceNowConfigController(
            OrganizationServiceNowConfigService organizationServiceNowConfigService,
            WorkspaceAccessService workspaceAccessService) {
        this.organizationServiceNowConfigService = organizationServiceNowConfigService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping("/config")
    public ServiceNowConfigResponse getConfig() {
        workspaceAccessService.requireCurrentTeamManager();
        return organizationServiceNowConfigService.getCurrentOrganizationConfig();
    }

    @PutMapping("/config")
    public ResponseEntity<?> saveConfig(@RequestBody ServiceNowConfigRequest request) {
        try {
            workspaceAccessService.requireCurrentTeamManager();
            return ResponseEntity.ok(organizationServiceNowConfigService.saveCurrentOrganizationConfig(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(400).body(ex.getMessage());
        }
    }
}
