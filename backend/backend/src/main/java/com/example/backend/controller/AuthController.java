package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.entity.OrganizationMembership;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMembership;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.OrganizationMembershipRepository;
import com.example.backend.repository.TeamMembershipRepository;
import com.example.backend.repository.TeamRepository;
import com.example.backend.security.JwtUtils;
import com.example.backend.service.WorkspaceBootstrapService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private WorkspaceBootstrapService workspaceBootstrapService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private OrganizationMembershipRepository organizationMembershipRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {
        if (signupRequest == null) {
            return ResponseEntity.badRequest().body("Signup request is required.");
        }

        String normalizedUsername = normalizeUsername(signupRequest.getUsername());
        String password = signupRequest.getPassword();

        if (normalizedUsername.isBlank()) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Password is required.");
        }

        if (userRepo.existsByNormalizedUsername(normalizedUsername)) {
            return ResponseEntity.status(409).body("Error: Username is already taken!");
        }
        boolean firstUser = userRepo.count() == 0;
        Team invitedTeam = null;
        if (!firstUser) {
            String inviteCode = normalizeInviteCode(signupRequest.getInviteCode());
            if (inviteCode.isBlank()) {
                return ResponseEntity.badRequest().body("Invite code is required to join an existing InciTeam organization.");
            }
            invitedTeam = teamRepository.findByJoinCode(inviteCode)
                    .orElse(null);
            if (invitedTeam == null) {
                return ResponseEntity.badRequest().body("Invite code is invalid.");
            }
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(firstUser ? "Admin" : "User");
        user.setCreated_at(Instant.now());
        user.setCreated_by(null);

        userRepo.save(user);
        if (firstUser) {
            workspaceBootstrapService.ensureWorkspaceForUser(user);
            return ResponseEntity.ok("Admin user registered successfully!");
        }

        OrganizationMembership organizationMembership = new OrganizationMembership();
        organizationMembership.setUser(user);
        organizationMembership.setOrganization(invitedTeam.getOrganization());
        organizationMembership.setRole("ORG_MEMBER");
        organizationMembership.setCreated_at(Instant.now());
        organizationMembershipRepository.save(organizationMembership);

        TeamMembership teamMembership = new TeamMembership();
        teamMembership.setUser(user);
        teamMembership.setTeam(invitedTeam);
        teamMembership.setRole("MEMBER");
        teamMembership.setCreated_at(Instant.now());
        teamMembershipRepository.save(teamMembership);

        user.setCurrentOrganization(invitedTeam.getOrganization());
        user.setCurrentTeam(invitedTeam);
        userRepo.save(user);
        return ResponseEntity.ok(firstUser
                ? "Admin user registered successfully!"
                : "User registered successfully and joined the invited team.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest authRequest) {
        if (authRequest == null) {
            return ResponseEntity.badRequest().body("Login request is required.");
        }

        String normalizedUsername = normalizeUsername(authRequest.getUsername());
        String password = authRequest.getPassword();

        if (normalizedUsername.isBlank()) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Password is required.");
        }

        return userRepo.findByNormalizedUsername(normalizedUsername)
            .map(user -> {
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    return ResponseEntity.status(401).body("Error: Invalid credentials");
                }
                String jwt = jwtUtils.generateToken(user.getUsername(), user.getRole());
                var workspace = workspaceBootstrapService.getWorkspaceSummary(user);
                AuthResponse resp = new AuthResponse(
                    jwt,
                    user.getU_id(),
                    user.getUsername(),
                    user.getRole(),
                    workspace
                );
                return ResponseEntity.ok(resp);
            })
            .orElseGet(() -> ResponseEntity.status(404).body("Error: User not found"));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode == null ? "" : inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
