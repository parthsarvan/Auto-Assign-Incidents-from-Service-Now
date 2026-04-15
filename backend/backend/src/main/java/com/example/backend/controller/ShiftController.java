package com.example.backend.controller;

import com.example.backend.entity.Shift;
import com.example.backend.entity.Team;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftRepository shiftRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public ShiftController(
            ShiftRepository shiftRepository,
            CurrentWorkspaceService currentWorkspaceService,
            WorkspaceAccessService workspaceAccessService) {
        this.shiftRepository = shiftRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<Shift> getAll() {
        return shiftRepository.findAllByTeamOrderByNameAsc(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Shift shift) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (shift == null) {
            return ResponseEntity.badRequest().body("Shift request is required.");
        }
        String normalizedName = normalizeText(shift != null ? shift.getName() : null);
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Shift name is required.");
        }
        if (shiftRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
            return ResponseEntity.badRequest().body("A shift with that name already exists in this team.");
        }
        shift.setName(normalizedName);
        shift.setTeam(team);
        return ResponseEntity.ok(shiftRepository.save(shift));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Shift shift) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (shift == null) {
            return ResponseEntity.badRequest().body("Shift request is required.");
        }
        String normalizedName = normalizeText(shift != null ? shift.getName() : null);
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Shift name is required.");
        }
        return shiftRepository.findByIdAndTeam(id, team)
            .map(existing -> {
                if (!normalizeText(existing.getName()).equalsIgnoreCase(normalizedName)
                        && shiftRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
                    return ResponseEntity.badRequest().body("A shift with that name already exists in this team.");
                }
                existing.setName(normalizedName);
                existing.setTeam(team);
                return ResponseEntity.ok(shiftRepository.save(existing));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        var shift = shiftRepository.findByIdAndTeam(id, team);
        if (shift.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        shiftRepository.delete(shift.get());
        return ResponseEntity.noContent().build();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }
}
