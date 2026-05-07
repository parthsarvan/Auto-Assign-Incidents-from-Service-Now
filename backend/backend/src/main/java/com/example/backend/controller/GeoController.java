package com.example.backend.controller;

import com.example.backend.entity.Geo;
import com.example.backend.entity.Team;
import com.example.backend.repository.GeoRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geos")
public class GeoController {
    private static final String LEGACY_DEFAULT_TIMEZONE = "UTC";

    private final GeoRepository geoRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public GeoController(
            GeoRepository geoRepository,
            CurrentWorkspaceService currentWorkspaceService,
            WorkspaceAccessService workspaceAccessService) {
        this.geoRepository = geoRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<Geo> getAll() {
        return geoRepository.findAllByTeamOrderByNameAsc(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Geo geo) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (geo == null) {
            return ResponseEntity.badRequest().body("Geo request is required.");
        }
        String normalizedName = normalizeText(geo != null ? geo.getName() : null);
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Geo name is required.");
        }
        if (geoRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
            return ResponseEntity.badRequest().body("A geo with that name already exists in this team.");
        }
        geo.setName(normalizedName);
        geo.setTeam(team);
        geo.setTimeZone(resolveGeoTimezone(team));
        return ResponseEntity.ok(geoRepository.save(geo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Geo geo) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (geo == null) {
            return ResponseEntity.badRequest().body("Geo request is required.");
        }
        String normalizedName = normalizeText(geo != null ? geo.getName() : null);
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Geo name is required.");
        }
        return geoRepository.findByIdAndTeam(id, team)
            .map(existing -> {
                if (!normalizeText(existing.getName()).equalsIgnoreCase(normalizedName)
                        && geoRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
                    return ResponseEntity.badRequest().body("A geo with that name already exists in this team.");
                }
                existing.setName(normalizedName);
                existing.setTeam(team);
                existing.setTimeZone(resolveGeoTimezone(team));
                return ResponseEntity.ok(geoRepository.save(existing));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        var geo = geoRepository.findByIdAndTeam(id, team);
        if (geo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        geoRepository.delete(geo.get());
        return ResponseEntity.noContent().build();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }

    private String resolveGeoTimezone(Team team) {
        if (team.getTimezone() == null || team.getTimezone().isBlank()) {
            return LEGACY_DEFAULT_TIMEZONE;
        }
        return team.getTimezone();
    }
}
