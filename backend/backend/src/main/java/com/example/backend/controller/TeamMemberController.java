package com.example.backend.controller;

import com.example.backend.entity.TeamMember;
import com.example.backend.repository.TeamMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-members")
public class TeamMemberController {

    private final TeamMemberRepository teamMemberRepository;

    public TeamMemberController(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    @GetMapping
    public List<TeamMember> getAll() {
        return teamMemberRepository.findAll();
    }

    @PostMapping
    public TeamMember create(@RequestBody TeamMember teamMember) {
        return teamMemberRepository.save(teamMember);
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
