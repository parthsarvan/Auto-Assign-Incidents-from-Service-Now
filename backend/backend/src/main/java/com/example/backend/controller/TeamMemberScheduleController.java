package com.example.backend.controller;

import com.example.backend.dto.TeamMemberScheduleRequest;
import com.example.backend.entity.Geo;
import com.example.backend.entity.Shift;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.TeamMemberSchedule;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.repository.TeamMemberScheduleRepository;
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

    public TeamMemberScheduleController(
        TeamMemberScheduleRepository scheduleRepository,
        TeamMemberRepository teamMemberRepository,
        GeoRepository geoRepository,
        ShiftRepository shiftRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
    }

    @GetMapping
    public List<TeamMemberSchedule> getAll() {
        return scheduleRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TeamMemberScheduleRequest request) {
        TeamMember tm = teamMemberRepository.findById(request.getTeamMemberId()).orElse(null);
        Geo geo = geoRepository.findById(request.getGeoId()).orElse(null);
        Shift shift = shiftRepository.findById(request.getShiftId()).orElse(null);

        if (tm == null || geo == null || shift == null) {
            return ResponseEntity.badRequest().body("Invalid team member, geo, or shift id");
        }

        TeamMemberSchedule schedule = new TeamMemberSchedule(
            tm,
            geo,
            shift,
            request.getStartDate(),
            request.getEndDate()
        );
        return ResponseEntity.ok(scheduleRepository.save(schedule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody TeamMemberScheduleRequest request
    ) {
        TeamMember tm = teamMemberRepository.findById(request.getTeamMemberId()).orElse(null);
        Geo geo = geoRepository.findById(request.getGeoId()).orElse(null);
        Shift shift = shiftRepository.findById(request.getShiftId()).orElse(null);

        if (tm == null || geo == null || shift == null) {
            return ResponseEntity.badRequest().body("Invalid team member, geo, or shift id");
        }

        return scheduleRepository.findById(id)
            .map(existing -> {
                existing.setTeamMember(tm);
                existing.setGeo(geo);
                existing.setShift(shift);
                existing.setStartDate(request.getStartDate());
                existing.setEndDate(request.getEndDate());
                return ResponseEntity.ok(scheduleRepository.save(existing));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scheduleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
