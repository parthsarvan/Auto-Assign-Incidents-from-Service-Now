package com.example.backend.controller;

import com.example.backend.dto.TeamMemberRequest;
import com.example.backend.entity.Geo;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.TeamMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/team-members")
public class TeamMemberController {

    private final TeamMemberRepository teamMemberRepository;
    private final GeoRepository geoRepository;

    public TeamMemberController(
        TeamMemberRepository teamMemberRepository,
        GeoRepository geoRepository
    ) {
        this.teamMemberRepository = teamMemberRepository;
        this.geoRepository = geoRepository;
    }

    @GetMapping
    public List<TeamMember> getAll() {
        return teamMemberRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TeamMemberRequest request) {
        if (request.getGeoId() == null) {
            return ResponseEntity.badRequest().body("Geo is required for team members.");
        }
        Optional<Geo> geo = geoRepository.findById(request.getGeoId());
        if (geo.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid geo ID.");
        }
        TeamMember teamMember = new TeamMember(
            request.getF_name(),
            request.getL_name(),
            geo.get()
        );
        return ResponseEntity.ok(teamMemberRepository.save(teamMember));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!teamMemberRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        teamMemberRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
