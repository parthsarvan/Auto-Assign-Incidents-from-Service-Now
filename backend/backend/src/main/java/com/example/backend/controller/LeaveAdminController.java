package com.example.backend.controller;

import com.example.backend.dto.LeaveEntryRequest;
import com.example.backend.entity.LeaveEntry;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.LeaveEntryRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveAdminController {

    private final LeaveEntryRepository leaveEntryRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public LeaveAdminController(
        LeaveEntryRepository leaveEntryRepository,
        TeamMemberRepository teamMemberRepository,
        CurrentWorkspaceService currentWorkspaceService,
        WorkspaceAccessService workspaceAccessService
    ) {
        this.leaveEntryRepository = leaveEntryRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<LeaveEntry> getAll() {
        return leaveEntryRepository.findAllByTeamWithTeamMember(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LeaveEntryRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Leave request is required.");
        }
        if (request.getTeamMemberId() == null) {
            return ResponseEntity.badRequest().body("Team member is required.");
        }
        if (request.getStartTs() == null || request.getEndTs() == null) {
            return ResponseEntity.badRequest().body("Start and end time are required.");
        }
        if (request.getEndTs().isBefore(request.getStartTs())) {
            return ResponseEntity.badRequest().body("End time must be on or after start time.");
        }
        TeamMember tm = teamMemberRepository.findByIdAndTeam(request.getTeamMemberId(), team).orElse(null);
        if (tm == null) {
            return ResponseEntity.badRequest().body("Invalid team member id");
        }

        LeaveEntry entry = new LeaveEntry(
                tm,
                request.getStartTs(),
                request.getEndTs(),
                normalizeOptionalText(request.getReason()));
        entry.setTeam(team);
        return ResponseEntity.ok(leaveEntryRepository.save(entry));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody LeaveEntryRequest request
    ) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Leave request is required.");
        }
        if (request.getTeamMemberId() == null) {
            return ResponseEntity.badRequest().body("Team member is required.");
        }
        if (request.getStartTs() == null || request.getEndTs() == null) {
            return ResponseEntity.badRequest().body("Start and end time are required.");
        }
        if (request.getEndTs().isBefore(request.getStartTs())) {
            return ResponseEntity.badRequest().body("End time must be on or after start time.");
        }
        TeamMember tm = teamMemberRepository.findByIdAndTeam(request.getTeamMemberId(), team).orElse(null);
        if (tm == null) {
            return ResponseEntity.badRequest().body("Invalid team member id");
        }

        return leaveEntryRepository.findByIdAndTeam(id, team)
            .map(entry -> {
                entry.setTeamMember(tm);
                entry.setStartTs(request.getStartTs());
                entry.setEndTs(request.getEndTs());
                entry.setReason(normalizeOptionalText(request.getReason()));
                entry.setTeam(team);
                return ResponseEntity.ok(leaveEntryRepository.save(entry));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        var entry = leaveEntryRepository.findByIdAndTeam(id, team);
        if (entry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        leaveEntryRepository.delete(entry.get());
        return ResponseEntity.noContent().build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s{2,}", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
