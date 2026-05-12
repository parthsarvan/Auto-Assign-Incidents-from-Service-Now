package com.example.backend.controller;

import com.example.backend.dto.CiUserMappingRequest;
import com.example.backend.dto.CiUserMappingBulkRequest;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @PostMapping("/bulk")
    public ResponseEntity<?> replaceForConfigurationItem(@RequestBody CiUserMappingBulkRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null || request.getConfigurationItemId() == null) {
            return ResponseEntity.badRequest().body("Configuration item is required.");
        }

        ConfigurationItem ci = configurationItemRepository.findByIdAndTeam(request.getConfigurationItemId(), team).orElse(null);
        if (ci == null) {
            return ResponseEntity.badRequest().body("Invalid configuration item id.");
        }

        List<Long> orderedMemberIds = new ArrayList<>(new LinkedHashSet<>(
                request.getTeamMemberIds() != null ? request.getTeamMemberIds() : List.of()));
        if (orderedMemberIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Select at least one team member.");
        }

        List<TeamMember> selectedMembers = new ArrayList<>();
        for (Long teamMemberId : orderedMemberIds) {
            TeamMember member = teamMemberRepository.findByIdAndTeam(teamMemberId, team).orElse(null);
            if (member == null) {
                return ResponseEntity.badRequest().body("Invalid team member id: " + teamMemberId);
            }
            selectedMembers.add(member);
        }

        List<CiUserMapping> existingMappings = mappingRepository.findByConfigurationItemAndTeamOrderBySortOrderAsc(ci, team);
        Map<Long, CiUserMapping> existingByMemberId = existingMappings.stream()
                .collect(Collectors.toMap(mapping -> mapping.getTeamMember().getTm_id(), mapping -> mapping));
        Set<Long> selectedMemberIds = new LinkedHashSet<>(orderedMemberIds);

        for (CiUserMapping existing : existingMappings) {
            if (!selectedMemberIds.contains(existing.getTeamMember().getTm_id())) {
                mappingRepository.delete(existing);
            }
        }

        List<CiUserMapping> savedMappings = new ArrayList<>();
        for (int index = 0; index < selectedMembers.size(); index++) {
            TeamMember member = selectedMembers.get(index);
            CiUserMapping mapping = existingByMemberId.getOrDefault(member.getTm_id(), new CiUserMapping());
            mapping.setConfigurationItem(ci);
            mapping.setTeamMember(member);
            mapping.setSortOrder(index);
            mapping.setTeam(team);
            savedMappings.add(mappingRepository.save(mapping));
        }

        return ResponseEntity.ok(savedMappings);
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
