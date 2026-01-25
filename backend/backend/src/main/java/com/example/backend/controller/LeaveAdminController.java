package com.example.backend.controller;

import com.example.backend.dto.LeaveEntryRequest;
import com.example.backend.entity.LeaveEntry;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.LeaveEntryRepository;
import com.example.backend.repository.TeamMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveAdminController {

    private final LeaveEntryRepository leaveEntryRepository;
    private final TeamMemberRepository teamMemberRepository;

    public LeaveAdminController(
        LeaveEntryRepository leaveEntryRepository,
        TeamMemberRepository teamMemberRepository
    ) {
        this.leaveEntryRepository = leaveEntryRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @GetMapping
    public List<LeaveEntry> getAll() {
        return leaveEntryRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LeaveEntryRequest request) {
        TeamMember tm = teamMemberRepository.findById(request.getTeamMemberId()).orElse(null);
        if (tm == null) {
            return ResponseEntity.badRequest().body("Invalid team member id");
        }

        LeaveEntry entry = new LeaveEntry(tm, request.getStartTs(), request.getEndTs(), request.getReason());
        return ResponseEntity.ok(leaveEntryRepository.save(entry));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody LeaveEntryRequest request
    ) {
        TeamMember tm = teamMemberRepository.findById(request.getTeamMemberId()).orElse(null);
        if (tm == null) {
            return ResponseEntity.badRequest().body("Invalid team member id");
        }

        return leaveEntryRepository.findById(id)
            .map(entry -> {
                entry.setTeamMember(tm);
                entry.setStartTs(request.getStartTs());
                entry.setEndTs(request.getEndTs());
                entry.setReason(request.getReason());
                return ResponseEntity.ok(leaveEntryRepository.save(entry));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!leaveEntryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        leaveEntryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
