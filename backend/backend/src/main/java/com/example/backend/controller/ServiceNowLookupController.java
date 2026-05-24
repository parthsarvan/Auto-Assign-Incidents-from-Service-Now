package com.example.backend.controller;

import com.example.backend.dto.ServiceNowLookupResult;
import com.example.backend.service.ServiceNowLookupService;
import com.example.backend.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicenow/lookup")
public class ServiceNowLookupController {
    private final ServiceNowLookupService serviceNowLookupService;
    private final WorkspaceAccessService workspaceAccessService;

    public ServiceNowLookupController(
            ServiceNowLookupService serviceNowLookupService,
            WorkspaceAccessService workspaceAccessService) {
        this.serviceNowLookupService = serviceNowLookupService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> searchUsers(@RequestParam(defaultValue = "") String query) {
        try {
            workspaceAccessService.requireCurrentTeamManager();
            List<ServiceNowLookupResult> results = serviceNowLookupService.searchUsers(query);
            return ResponseEntity.ok(results);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(400).body(ex.getMessage());
        }
    }

    @GetMapping("/configuration-items")
    public ResponseEntity<?> searchConfigurationItems(@RequestParam(defaultValue = "") String query) {
        try {
            workspaceAccessService.requireCurrentTeamManager();
            List<ServiceNowLookupResult> results = serviceNowLookupService.searchConfigurationItems(query);
            return ResponseEntity.ok(results);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(400).body(ex.getMessage());
        }
    }
}
