package com.example.backend.controller;

import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.Team;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuration-items")
public class ConfigurationItemController {

    private final ConfigurationItemRepository configurationItemRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public ConfigurationItemController(
            ConfigurationItemRepository configurationItemRepository,
            CurrentWorkspaceService currentWorkspaceService,
            WorkspaceAccessService workspaceAccessService) {
        this.configurationItemRepository = configurationItemRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<ConfigurationItem> getAll() {
        return configurationItemRepository.findAllByTeamOrderByNameAsc(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ConfigurationItem configurationItem) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (configurationItem == null) {
            return ResponseEntity.badRequest().body("Configuration item request is required.");
        }
        String normalizedName = normalizeText(configurationItem != null ? configurationItem.getName() : null);
        String normalizedDescription = normalizeOptionalText(configurationItem != null ? configurationItem.getDescription() : null);
        String normalizedServiceNowSysId = normalizeCompactText(configurationItem != null ? configurationItem.getServiceNowSysId() : null);
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Name is required for configuration items.");
        }
        if (normalizedServiceNowSysId.isBlank()) {
            return ResponseEntity.badRequest().body("ServiceNow CI sys ID is required for configuration items.");
        }
        if (configurationItemRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
            return ResponseEntity.badRequest().body("A configuration item with that name already exists in this team.");
        }
        if (configurationItemRepository.existsByTeamAndNormalizedServiceNowSysId(team, normalizedServiceNowSysId)) {
            return ResponseEntity.badRequest().body("That ServiceNow CI sys ID already exists in this team.");
        }
        configurationItem.setName(normalizedName);
        configurationItem.setDescription(normalizedDescription);
        configurationItem.setServiceNowSysId(normalizedServiceNowSysId);
        configurationItem.setTeam(team);
        return ResponseEntity.ok(configurationItemRepository.save(configurationItem));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody ConfigurationItem configurationItem
    ) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (configurationItem == null) {
            return ResponseEntity.badRequest().body("Configuration item request is required.");
        }
        String normalizedName = normalizeText(configurationItem != null ? configurationItem.getName() : null);
        String normalizedDescription = normalizeOptionalText(configurationItem != null ? configurationItem.getDescription() : null);
        String normalizedServiceNowSysId = normalizeCompactText(configurationItem != null ? configurationItem.getServiceNowSysId() : null);
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Name is required for configuration items.");
        }
        if (normalizedServiceNowSysId.isBlank()) {
            return ResponseEntity.badRequest().body("ServiceNow CI sys ID is required for configuration items.");
        }
        return configurationItemRepository.findByIdAndTeam(id, team)
            .map(existing -> {
                if (!normalizeText(existing.getName()).equalsIgnoreCase(normalizedName)
                        && configurationItemRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
                    return ResponseEntity.badRequest().body("A configuration item with that name already exists in this team.");
                }
                String existingSysId = normalizeCompactText(existing.getServiceNowSysId());
                if (!existingSysId.equalsIgnoreCase(normalizedServiceNowSysId)
                        && configurationItemRepository.existsByTeamAndNormalizedServiceNowSysId(team, normalizedServiceNowSysId)) {
                    return ResponseEntity.badRequest().body("That ServiceNow CI sys ID already exists in this team.");
                }
                existing.setName(normalizedName);
                existing.setDescription(normalizedDescription);
                existing.setServiceNowSysId(normalizedServiceNowSysId);
                existing.setTeam(team);
                return ResponseEntity.ok(configurationItemRepository.save(existing));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        var configurationItem = configurationItemRepository.findByIdAndTeam(id, team);
        if (configurationItem.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        configurationItemRepository.delete(configurationItem.get());
        return ResponseEntity.noContent().build();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCompactText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
