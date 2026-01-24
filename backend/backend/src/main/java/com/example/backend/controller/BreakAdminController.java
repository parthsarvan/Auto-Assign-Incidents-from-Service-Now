package com.example.backend.controller;

import com.example.backend.dto.BreakEntryRequest;
import com.example.backend.entity.BreakEntry;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.BreakEntryRepository;
import com.example.backend.repository.TeamMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/breaks")
public class BreakAdminController {

    private final BreakEntryRepository breakEntryRepository;
    private final TeamMemberRepository teamMemberRepository;

    public BreakAdminController(
        BreakEntryRepository breakEntryRepository,
        TeamMemberRepository teamMemberRepository
    ) {
        this.breakEntryRepository = breakEntryRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @GetMapping
    public List<BreakEntry> getAll() {
        return breakEntryRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BreakEntryRequest request) {
        TeamMember tm = teamMemberRepository.findById(request.getTeamMemberId()).orElse(null);
        if (tm == null) {
            return ResponseEntity.badRequest().body("Invalid team member id");
        }

        BreakEntry entry = new BreakEntry(tm, request.getStartTs(), request.getEndTs(), request.getReason());
        return ResponseEntity.ok(breakEntryRepository.save(entry));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody BreakEntryRequest request
    ) {
        TeamMember tm = teamMemberRepository.findById(request.getTeamMemberId()).orElse(null);
        if (tm == null) {
            return ResponseEntity.badRequest().body("Invalid team member id");
        }

        return breakEntryRepository.findById(id)
            .map(entry -> {
                entry.setTeamMember(tm);
                entry.setStartTs(request.getStartTs());
                entry.setEndTs(request.getEndTs());
                entry.setReason(request.getReason());
                return ResponseEntity.ok(breakEntryRepository.save(entry));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!breakEntryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        breakEntryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
