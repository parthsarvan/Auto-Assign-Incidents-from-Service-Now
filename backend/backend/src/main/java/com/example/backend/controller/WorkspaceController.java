package com.example.backend.controller;

import com.example.backend.dto.CreateTeamRequest;
import com.example.backend.dto.SwitchTeamRequest;
import com.example.backend.dto.TeamSummary;
import com.example.backend.dto.WorkspaceSummary;
import com.example.backend.service.TeamWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {
    private final TeamWorkspaceService teamWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public WorkspaceController(
            TeamWorkspaceService teamWorkspaceService,
            WorkspaceAccessService workspaceAccessService) {
        this.teamWorkspaceService = teamWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping("/teams")
    public List<TeamSummary> getTeams() {
        return teamWorkspaceService.getAccessibleTeams();
    }

    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestBody CreateTeamRequest request) {
        try {
            workspaceAccessService.requireGlobalAdmin();
            WorkspaceSummary workspace = teamWorkspaceService.createTeam(
                    request != null ? request.getName() : null,
                    request != null ? request.getDescription() : null,
                    request != null ? request.getCopyFromTeamId() : null);
            return ResponseEntity.ok(workspace);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @PostMapping("/switch-team")
    public ResponseEntity<?> switchTeam(@RequestBody SwitchTeamRequest request) {
        try {
            WorkspaceSummary workspace = teamWorkspaceService.switchTeam(
                    request != null ? request.getTeamId() : null);
            return ResponseEntity.ok(workspace);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @PostMapping("/teams/{teamId}/regenerate-invite")
    public ResponseEntity<?> regenerateInvite(@PathVariable Long teamId) {
        try {
            workspaceAccessService.requireGlobalAdmin();
            return ResponseEntity.ok(teamWorkspaceService.regenerateJoinCode(teamId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }
}
