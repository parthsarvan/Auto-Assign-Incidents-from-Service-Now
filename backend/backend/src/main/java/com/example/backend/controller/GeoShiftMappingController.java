package com.example.backend.controller;

import com.example.backend.dto.GeoShiftMappingRequest;
import com.example.backend.entity.Geo;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Shift;
import com.example.backend.entity.Team;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo-shift-mappings")
public class GeoShiftMappingController {

    private final GeoShiftMappingRepository mappingRepository;
    private final GeoRepository geoRepository;
    private final ShiftRepository shiftRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public GeoShiftMappingController(
        GeoShiftMappingRepository mappingRepository,
        GeoRepository geoRepository,
        ShiftRepository shiftRepository,
        CurrentWorkspaceService currentWorkspaceService,
        WorkspaceAccessService workspaceAccessService
    ) {
        this.mappingRepository = mappingRepository;
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<GeoShiftMapping> getAll() {
        return mappingRepository.findAllByTeamWithGeoAndShift(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody GeoShiftMappingRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Geo-shift mapping request is required.");
        }
        if (request.getGeoId() == null || request.getShiftId() == null) {
            return ResponseEntity.badRequest().body("Geo and shift are required.");
        }
        Geo geo = geoRepository.findByIdAndTeam(request.getGeoId(), team).orElse(null);
        Shift shift = shiftRepository.findByIdAndTeam(request.getShiftId(), team).orElse(null);

        if (geo == null || shift == null) {
            return ResponseEntity.badRequest().body("Invalid geo or shift id");
        }
        if (mappingRepository.existsByGeoAndShiftAndTeam(geo, shift, team)) {
            return ResponseEntity.badRequest().body("That geo/shift mapping already exists in this team.");
        }

        GeoShiftMapping mapping = new GeoShiftMapping(geo, shift);
        mapping.setTeam(team);
        return ResponseEntity.ok(mappingRepository.save(mapping));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody GeoShiftMappingRequest request
    ) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Geo-shift mapping request is required.");
        }
        if (request.getGeoId() == null || request.getShiftId() == null) {
            return ResponseEntity.badRequest().body("Geo and shift are required.");
        }
        Geo geo = geoRepository.findByIdAndTeam(request.getGeoId(), team).orElse(null);
        Shift shift = shiftRepository.findByIdAndTeam(request.getShiftId(), team).orElse(null);

        if (geo == null || shift == null) {
            return ResponseEntity.badRequest().body("Invalid geo or shift id");
        }

        return mappingRepository.findByIdAndTeam(id, team)
            .map(existing -> {
                if ((!existing.getGeo().getG_id().equals(geo.getG_id()) || !existing.getShift().getS_id().equals(shift.getS_id()))
                        && mappingRepository.existsByGeoAndShiftAndTeam(geo, shift, team)) {
                    return ResponseEntity.badRequest().body("That geo/shift mapping already exists in this team.");
                }
                existing.setGeo(geo);
                existing.setShift(shift);
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
