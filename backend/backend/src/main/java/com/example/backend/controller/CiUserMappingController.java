package com.example.backend.controller;

import com.example.backend.dto.CiUserMappingRequest;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.Team;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ci-user-mappings")
public class CiUserMappingController {

    private final CiUserMappingRepository mappingRepository;
    private final ConfigurationItemRepository configurationItemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public CiUserMappingController(
        CiUserMappingRepository mappingRepository,
        ConfigurationItemRepository configurationItemRepository,
        TeamMemberRepository teamMemberRepository,
        CurrentWorkspaceService currentWorkspaceService,
        WorkspaceAccessService workspaceAccessService
    ) {
        this.mappingRepository = mappingRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<CiUserMapping> getAll() {
        return mappingRepository.findAllByTeamWithDetails(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CiUserMappingRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("CI-user mapping request is required.");
        }
        if (request.getConfigurationItemId() == null || request.getTeamMemberId() == null) {
            return ResponseEntity.badRequest().body("Configuration item and team member are required.");
        }
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : 0;
        if (sortOrder < 0) {
            return ResponseEntity.badRequest().body("Sort order cannot be negative.");
        }
        ConfigurationItem ci = configurationItemRepository.findByIdAndTeam(request.getConfigurationItemId(), team).orElse(null);
        TeamMember tm = teamMemberRepository.findByIdAndTeam(request.getTeamMemberId(), team).orElse(null);

        if (ci == null || tm == null) {
            return ResponseEntity.badRequest().body("Invalid configuration item or team member id");
        }
        if (mappingRepository.existsByConfigurationItemAndTeamMemberAndTeam(ci, tm, team)) {
            return ResponseEntity.badRequest().body("That CI-user mapping already exists in this team.");
        }

        CiUserMapping mapping = new CiUserMapping(ci, tm, sortOrder);
        mapping.setTeam(team);
        return ResponseEntity.ok(mappingRepository.save(mapping));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody CiUserMappingRequest request
    ) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("CI-user mapping request is required.");
        }
        if (request.getConfigurationItemId() == null || request.getTeamMemberId() == null) {
            return ResponseEntity.badRequest().body("Configuration item and team member are required.");
        }
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : 0;
        if (sortOrder < 0) {
            return ResponseEntity.badRequest().body("Sort order cannot be negative.");
        }
        ConfigurationItem ci = configurationItemRepository.findByIdAndTeam(request.getConfigurationItemId(), team).orElse(null);
        TeamMember tm = teamMemberRepository.findByIdAndTeam(request.getTeamMemberId(), team).orElse(null);

        if (ci == null || tm == null) {
            return ResponseEntity.badRequest().body("Invalid configuration item or team member id");
        }

        return mappingRepository.findByIdAndTeam(id, team)
            .map(existing -> {
                boolean samePair = existing.getConfigurationItem().getCi_id().equals(ci.getCi_id())
                        && existing.getTeamMember().getTm_id().equals(tm.getTm_id());
                if (!samePair && mappingRepository.existsByConfigurationItemAndTeamMemberAndTeam(ci, tm, team)) {
                    return ResponseEntity.badRequest().body("That CI-user mapping already exists in this team.");
                }
                existing.setConfigurationItem(ci);
                existing.setTeamMember(tm);
                existing.setSortOrder(sortOrder);
                existing.setTeam(team);
                return ResponseEntity.ok(mappingRepository.save(existing));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        var mapping = mappingRepository.findByIdAndTeam(id, team);
        if (mapping.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        mappingRepository.delete(mapping.get());
        return ResponseEntity.noContent().build();
    }
}
