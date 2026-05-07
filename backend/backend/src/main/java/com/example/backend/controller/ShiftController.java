package com.example.backend.controller;

import com.example.backend.dto.ShiftRequest;
import com.example.backend.entity.Geo;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Shift;
import com.example.backend.entity.Team;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftRepository shiftRepository;
    private final GeoRepository geoRepository;
    private final GeoShiftMappingRepository geoShiftMappingRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public ShiftController(
            ShiftRepository shiftRepository,
            GeoRepository geoRepository,
            GeoShiftMappingRepository geoShiftMappingRepository,
            CurrentWorkspaceService currentWorkspaceService,
            WorkspaceAccessService workspaceAccessService) {
        this.shiftRepository = shiftRepository;
        this.geoRepository = geoRepository;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<Shift> getAll() {
        return shiftRepository.findAllByTeamOrderByNameAsc(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ShiftRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Shift request is required.");
        }
        String normalizedName = normalizeText(request.getName());
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Shift name is required.");
        }
        if (shiftRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
            return ResponseEntity.badRequest().body("A shift with that name already exists in this team.");
        }
        if (team.getTimezone() == null || team.getTimezone().isBlank()) {
            return ResponseEntity.badRequest().body("Set the team timezone before adding shifts.");
        }
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (startTime == null || endTime == null) {
            return ResponseEntity.badRequest().body("Shift start time and end time are required.");
        }
        List<Geo> selectedGeos = resolveSelectedGeos(request.getGeoIds(), team);
        if (selectedGeos == null) {
            return ResponseEntity.badRequest().body("Select at least one valid geo for this shift.");
        }

        Shift shift = new Shift();
        shift.setName(normalizedName);
        shift.setTeam(team);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        Shift savedShift = shiftRepository.save(shift);
        syncShiftGeos(savedShift, team, selectedGeos);
        return ResponseEntity.ok(savedShift);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ShiftRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Shift request is required.");
        }
        String normalizedName = normalizeText(request.getName());
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body("Shift name is required.");
        }
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (startTime == null || endTime == null) {
            return ResponseEntity.badRequest().body("Shift start time and end time are required.");
        }
        List<Geo> selectedGeos = resolveSelectedGeos(request.getGeoIds(), team);
        if (selectedGeos == null) {
            return ResponseEntity.badRequest().body("Select at least one valid geo for this shift.");
        }
        return shiftRepository.findByIdAndTeam(id, team)
            .map(existing -> {
                if (!normalizeText(existing.getName()).equalsIgnoreCase(normalizedName)
                        && shiftRepository.existsByTeamAndNormalizedName(team, normalizedName)) {
                    return ResponseEntity.badRequest().body("A shift with that name already exists in this team.");
                }
                existing.setName(normalizedName);
                existing.setTeam(team);
                existing.setStartTime(startTime);
                existing.setEndTime(endTime);
                Shift savedShift = shiftRepository.save(existing);
                syncShiftGeos(savedShift, team, selectedGeos);
                return ResponseEntity.ok(savedShift);
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

    private List<Geo> resolveSelectedGeos(List<Long> geoIds, Team team) {
        if (geoIds == null || geoIds.isEmpty()) {
            return null;
        }
        Set<Long> uniqueGeoIds = new LinkedHashSet<>();
        for (Long geoId : geoIds) {
            if (geoId != null) {
                uniqueGeoIds.add(geoId);
            }
        }
        if (uniqueGeoIds.isEmpty()) {
            return null;
        }

        List<Geo> selectedGeos = new ArrayList<>();
        for (Long geoId : uniqueGeoIds) {
            Geo geo = geoRepository.findByIdAndTeam(geoId, team).orElse(null);
            if (geo == null) {
                return null;
            }
            selectedGeos.add(geo);
        }
        return selectedGeos;
    }

    private void syncShiftGeos(Shift shift, Team team, List<Geo> selectedGeos) {
        Set<Long> selectedGeoIds = selectedGeos.stream()
                .map(Geo::getG_id)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        List<GeoShiftMapping> existingMappings = geoShiftMappingRepository.findAllByShiftAndTeam(shift, team);
        for (GeoShiftMapping existingMapping : existingMappings) {
            Long existingGeoId = existingMapping.getGeo() != null ? existingMapping.getGeo().getG_id() : null;
            if (existingGeoId == null || !selectedGeoIds.contains(existingGeoId)) {
                geoShiftMappingRepository.delete(existingMapping);
            } else {
                existingMapping.syncTimesFromShift();
                geoShiftMappingRepository.save(existingMapping);
            }
        }

        for (Geo geo : selectedGeos) {
            if (!geoShiftMappingRepository.existsByGeoAndShiftAndTeam(geo, shift, team)) {
                GeoShiftMapping mapping = new GeoShiftMapping(geo, shift);
                mapping.setTeam(team);
                mapping.syncTimesFromShift();
                geoShiftMappingRepository.save(mapping);
            }
        }
    }
}
