package com.example.backend.controller;

import com.example.backend.dto.TeamMemberRequest;
import com.example.backend.dto.TeamJoinedUserSummary;
import com.example.backend.entity.Geo;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.repository.TeamMembershipRepository;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/team-members")
public class TeamMemberController {

    private final TeamMemberRepository teamMemberRepository;
    private final GeoRepository geoRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public TeamMemberController(
        TeamMemberRepository teamMemberRepository,
        GeoRepository geoRepository,
        TeamMembershipRepository teamMembershipRepository,
        CurrentWorkspaceService currentWorkspaceService,
        WorkspaceAccessService workspaceAccessService
    ) {
        this.teamMemberRepository = teamMemberRepository;
        this.geoRepository = geoRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<TeamMember> getAll() {
        return teamMemberRepository.findAllByTeamOrderByName(currentWorkspaceService.getCurrentTeam());
    }

    @GetMapping("/joined-users")
    public List<TeamJoinedUserSummary> getJoinedUsers() {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        return teamMembershipRepository.findAllByTeamWithUser(team).stream()
            .map(membership -> membership.getUser())
            .filter(user -> user.getWorkEmail() != null && !user.getWorkEmail().isBlank())
            .map(user -> new TeamJoinedUserSummary(
                user.getU_id(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getWorkEmail().trim().toLowerCase(Locale.ROOT)
            ))
            .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TeamMemberRequest request) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Team member request is required.");
        }
        String firstName = normalizeText(request != null ? request.getF_name() : null);
        String lastName = normalizeText(request != null ? request.getL_name() : null);
        String email = normalizeCompactText(request != null ? request.getEmail() : null).toLowerCase();
        String phone = normalizeOptionalText(request != null ? request.getPhone() : null);
        String sysId = normalizeCompactText(request != null ? request.getSys_id() : null);
        if (firstName.isBlank()) {
            return ResponseEntity.badRequest().body("First name is required for team members.");
        }
        if (lastName.isBlank()) {
            return ResponseEntity.badRequest().body("Last name is required for team members.");
        }
        if (request.getGeoId() == null) {
            return ResponseEntity.badRequest().body("Geo is required for team members.");
        }
        if (email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required for team members.");
        }
        if (sysId.isBlank()) {
            return ResponseEntity.badRequest().body("Select the matching ServiceNow user for this team member.");
        }
        if (teamMemberRepository.existsByTeamAndNormalizedEmail(email, team)) {
            return ResponseEntity.badRequest().body("A team member with that email already exists in this team.");
        }
        if (teamMemberRepository.existsByTeamAndNormalizedSysId(sysId, team)) {
            return ResponseEntity.badRequest().body("That ServiceNow user is already linked in this team.");
        }
        Optional<Geo> geo = geoRepository.findByIdAndTeam(request.getGeoId(), team);
        if (geo.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid geo ID.");
        }
        TeamMember teamMember = new TeamMember(
            firstName,
            lastName,
            email,
            geo.get()
        );
        teamMember.setPhone(phone);
        teamMember.setSys_id(sysId);
        teamMember.setTeam(team);
        return ResponseEntity.ok(teamMemberRepository.save(teamMember));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable Long id,
        @RequestBody TeamMemberRequest request
    ) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (request == null) {
            return ResponseEntity.badRequest().body("Team member request is required.");
        }
        String firstName = normalizeText(request != null ? request.getF_name() : null);
        String lastName = normalizeText(request != null ? request.getL_name() : null);
        String email = normalizeCompactText(request != null ? request.getEmail() : null).toLowerCase();
        String phone = normalizeOptionalText(request != null ? request.getPhone() : null);
        String sysId = normalizeCompactText(request != null ? request.getSys_id() : null);
        if (firstName.isBlank()) {
            return ResponseEntity.badRequest().body("First name is required for team members.");
        }
        if (lastName.isBlank()) {
            return ResponseEntity.badRequest().body("Last name is required for team members.");
        }
        if (request.getGeoId() == null) {
            return ResponseEntity.badRequest().body("Geo is required for team members.");
        }
        if (email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required for team members.");
        }
        if (sysId.isBlank()) {
            return ResponseEntity.badRequest().body("Select the matching ServiceNow user for this team member.");
        }
        Optional<Geo> geo = geoRepository.findByIdAndTeam(request.getGeoId(), team);
        if (geo.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid geo ID.");
        }

        return teamMemberRepository.findByIdAndTeam(id, team)
            .map(member -> {
                String existingEmail = normalizeCompactText(member.getEmail()).toLowerCase();
                if (!existingEmail.equalsIgnoreCase(email)
                        && teamMemberRepository.existsByTeamAndNormalizedEmail(email, team)) {
                    return ResponseEntity.badRequest().body("A team member with that email already exists in this team.");
                }
                String existingSysId = normalizeCompactText(member.getSys_id());
                if (!existingSysId.equalsIgnoreCase(sysId)
                        && teamMemberRepository.existsByTeamAndNormalizedSysId(sysId, team)) {
                    return ResponseEntity.badRequest().body("That ServiceNow user is already linked in this team.");
                }
                member.setF_name(firstName);
                member.setL_name(lastName);
                member.setEmail(email);
                member.setPhone(phone);
                member.setSys_id(sysId);
                member.setGeo(geo.get());
                member.setTeam(team);
                return ResponseEntity.ok(teamMemberRepository.save(member));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceAccessService.requireCurrentTeamManager();
        Team team = currentWorkspaceService.getCurrentTeam();
        var member = teamMemberRepository.findByIdAndTeam(id, team);
        if (member.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        teamMemberRepository.delete(member.get());
        return ResponseEntity.noContent().build();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCompactText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
