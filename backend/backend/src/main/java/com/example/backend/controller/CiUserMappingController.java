package com.example.backend.controller;

import com.example.backend.dto.CiUserMappingRequest;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.TeamMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ci-user-mappings")
public class CiUserMappingController {

    private final CiUserMappingRepository mappingRepository;
    private final ConfigurationItemRepository configurationItemRepository;
    private final TeamMemberRepository teamMemberRepository;

    public CiUserMappingController(
        CiUserMappingRepository mappingRepository,
        ConfigurationItemRepository configurationItemRepository,
        TeamMemberRepository teamMemberRepository
    ) {
        this.mappingRepository = mappingRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @GetMapping
    public List<CiUserMapping> getAll() {
        return mappingRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CiUserMappingRequest request) {
        ConfigurationItem ci = configurationItemRepository.findById(request.getConfigurationItemId()).orElse(null);
        TeamMember tm = teamMemberRepository.findById(request.getTeamMemberId()).orElse(null);

        if (ci == null || tm == null) {
            return ResponseEntity.badRequest().body("Invalid configuration item or team member id");
        }

        CiUserMapping mapping = new CiUserMapping(ci, tm, request.getSortOrder());
        return ResponseEntity.ok(mappingRepository.save(mapping));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!mappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mappingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
