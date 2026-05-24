package com.example.backend.controller;

import com.example.backend.dto.NotificationSettingsRequest;
import com.example.backend.service.NotificationSettingsService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/settings")
public class NotificationSettingsController {
    private final NotificationSettingsService notificationSettingsService;
    private final WorkspaceAccessService workspaceAccessService;

    public NotificationSettingsController(
            NotificationSettingsService notificationSettingsService,
            WorkspaceAccessService workspaceAccessService) {
        this.notificationSettingsService = notificationSettingsService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public ResponseEntity<?> getSettings() {
        try {
            workspaceAccessService.requireCurrentTeamManager();
            return ResponseEntity.ok(notificationSettingsService.getSettings());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody NotificationSettingsRequest request) {
        try {
            workspaceAccessService.requireCurrentTeamManager();
            return ResponseEntity.ok(notificationSettingsService.updateSettings(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> sendTestEmail() {
        try {
            workspaceAccessService.requireCurrentTeamManager();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
        try {
            return ResponseEntity.ok(notificationSettingsService.sendTestEmail());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
