package com.example.backend.controller;

import com.example.backend.dto.TeamMemberScheduleRequest;
import com.example.backend.entity.Geo;
import com.example.backend.entity.Shift;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.TeamMemberSchedule;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.repository.TeamMemberScheduleRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-member-schedules")
public class TeamMemberScheduleController {

    private final TeamMemberScheduleRepository scheduleRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final GeoRepository geoRepository;
    private final ShiftRepository shiftRepository;
    private final GeoShiftMappingRepository geoShiftMappingRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public TeamMemberScheduleController(
        TeamMemberScheduleRepository scheduleRepository,
        TeamMemberRepository teamMemberRepository,
        GeoRepository geoRepository,
        ShiftRepository shiftRepository,
        GeoShiftMappingRepository geoShiftMappingRepository,
        CurrentWorkspaceService currentWorkspaceService,
        WorkspaceAccessService workspaceAccessService
    ) {
        this.scheduleRepository = scheduleRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<TeamMemberSchedule> getAll() {
        return scheduleRepository.findAllByTeamWithDetails(currentWorkspaceService.getCurrentTeam());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TeamMemberScheduleRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Schedule request is required.");
        }
        if (request.getTeamMemberId() == null || request.getGeoId() == null || request.getShiftId() == null) {
            return ResponseEntity.badRequest().body("Team member, geo, and shift are required.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            return ResponseEntity.badRequest().body("Start date and end date are required.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            return ResponseEntity.badRequest().body("End date must be on or after start date.");
        }
        TeamMember tm = teamMemberRepository.findByIdAndTeam(request.getTeamMemberId(), team).orElse(null);
        Geo geo = geoRepository.findByIdAndTeam(request.getGeoId(), team).orElse(null);
        Shift shift = shiftRepository.findByIdAndTeam(request.getShiftId(), team).orElse(null);

        if (tm == null || geo == null || shift == null) {
            return ResponseEntity.badRequest().body("Invalid team member, geo, or shift id");
        }
        if (tm.getGeo() == null || !tm.getGeo().getG_id().equals(geo.getG_id())) {
            return ResponseEntity.badRequest().body("The selected team member does not belong to that geo.");
        }
        if (!geoShiftMappingRepository.existsByGeoAndShiftAndTeam(geo, shift, team)) {
            return ResponseEntity.badRequest().body("That shift is not mapped to the selected geo in this team.");
        }
        if (scheduleRepository.existsOverlappingSchedule(tm, request.getStartDate(), request.getEndDate(), null)) {
            return ResponseEntity.badRequest().body("That team member already has an overlapping schedule.");
        }

        TeamMemberSchedule schedule = new TeamMemberSchedule(
            tm,
            geo,
            shift,
            request.getStartDate(),
            request.getEndDate()
        );
        schedule.setTeam(team);
        return ResponseEntity.ok(scheduleRepository.save(schedule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody TeamMemberScheduleRequest request
    ) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Schedule request is required.");
        }
        if (request.getTeamMemberId() == null || request.getGeoId() == null || request.getShiftId() == null) {
            return ResponseEntity.badRequest().body("Team member, geo, and shift are required.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            return ResponseEntity.badRequest().body("Start date and end date are required.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            return ResponseEntity.badRequest().body("End date must be on or after start date.");
        }
        TeamMember tm = teamMemberRepository.findByIdAndTeam(request.getTeamMemberId(), team).orElse(null);
        Geo geo = geoRepository.findByIdAndTeam(request.getGeoId(), team).orElse(null);
        Shift shift = shiftRepository.findByIdAndTeam(request.getShiftId(), team).orElse(null);

        if (tm == null || geo == null || shift == null) {
            return ResponseEntity.badRequest().body("Invalid team member, geo, or shift id");
        }
        if (tm.getGeo() == null || !tm.getGeo().getG_id().equals(geo.getG_id())) {
            return ResponseEntity.badRequest().body("The selected team member does not belong to that geo.");
        }
        if (!geoShiftMappingRepository.existsByGeoAndShiftAndTeam(geo, shift, team)) {
            return ResponseEntity.badRequest().body("That shift is not mapped to the selected geo in this team.");
        }

        return scheduleRepository.findByIdAndTeam(id, team)
            .map(existing -> {
                if (scheduleRepository.existsOverlappingSchedule(
                        tm,
                        request.getStartDate(),
                        request.getEndDate(),
                        existing.getTms_id())) {
                    return ResponseEntity.badRequest().body("That team member already has an overlapping schedule.");
                }
                existing.setTeamMember(tm);
                existing.setGeo(geo);
                existing.setShift(shift);
                existing.setStartDate(request.getStartDate());
                existing.setEndDate(request.getEndDate());
                existing.setTeam(team);
                return ResponseEntity.ok(scheduleRepository.save(existing));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        var schedule = scheduleRepository.findByIdAndTeam(id, team);
        if (schedule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        scheduleRepository.delete(schedule.get());
        return ResponseEntity.noContent().build();
    }
}
