package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.OrganizationDiscoveryRequest;
import com.example.backend.dto.OrganizationDiscoveryResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.entity.OrganizationMembership;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMembership;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.OrganizationMembershipRepository;
import com.example.backend.repository.OrganizationRepository;
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
    private OrganizationRepository organizationRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @PostMapping("/organization-discovery")
    public ResponseEntity<?> discoverOrganization(@RequestBody OrganizationDiscoveryRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("Organization and work email are required.");
        }

        String organizationName = normalizeName(request.getOrganizationName());
        String normalizedWorkEmail = normalizeWorkEmail(request.getWorkEmail());

        if (organizationName.isBlank()) {
            return ResponseEntity.badRequest().body("Organization name is required.");
        }
        if (normalizedWorkEmail.isBlank()) {
            return ResponseEntity.badRequest().body("Work email is required.");
        }

        String emailDomain = extractEmailDomain(normalizedWorkEmail);
        if (emailDomain.isBlank()) {
            return ResponseEntity.badRequest().body("Enter a valid work email address.");
        }

        var existingOrganization = organizationRepository.findByEmailDomain(emailDomain);
        OrganizationDiscoveryResponse response = new OrganizationDiscoveryResponse(
            existingOrganization.isPresent(),
            existingOrganization.map(org -> normalizeName(org.getName())).filter(name -> !name.isBlank()).orElse(organizationName),
            emailDomain,
            existingOrganization.isPresent()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {
        if (signupRequest == null) {
            return ResponseEntity.badRequest().body("Signup request is required.");
        }

        String normalizedUsername = normalizeUsername(signupRequest.getUsername());
        String firstName = normalizeName(signupRequest.getFirstName());
        String lastName = normalizeName(signupRequest.getLastName());
        String normalizedWorkEmail = normalizeWorkEmail(signupRequest.getWorkEmail());
        String password = signupRequest.getPassword();
        String inviteCode = normalizeInviteCode(signupRequest.getInviteCode());
        String organizationName = normalizeName(signupRequest.getOrganizationName());
        String teamName = normalizeName(signupRequest.getTeamName());

        if (normalizedUsername.isBlank()) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        if (normalizedWorkEmail.isBlank()) {
            return ResponseEntity.badRequest().body("Work email is required.");
        }
        if (firstName.isBlank()) {
            return ResponseEntity.badRequest().body("First name is required.");
        }
        if (lastName.isBlank()) {
            return ResponseEntity.badRequest().body("Last name is required.");
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Password is required.");
        }

        if (userRepo.existsByNormalizedUsername(normalizedUsername)) {
            return ResponseEntity.status(409).body("Error: Username is already taken!");
        }
        if (userRepo.existsByNormalizedWorkEmail(normalizedWorkEmail)) {
            return ResponseEntity.status(409).body("Error: Work email is already in use!");
        }

        String emailDomain = extractEmailDomain(normalizedWorkEmail);
        if (emailDomain.isBlank()) {
            return ResponseEntity.badRequest().body("Enter a valid work email address.");
        }

        var existingOrganization = organizationRepository.findByEmailDomain(emailDomain);
        boolean creatingNewOrganization = inviteCode.isBlank();
        Team invitedTeam = null;

        if (creatingNewOrganization) {
            if (existingOrganization.isPresent()) {
                return ResponseEntity.badRequest().body("An InciTeam organization already exists for this email domain. Ask your admin for a team invite code.");
            }
            if (organizationName.isBlank()) {
                return ResponseEntity.badRequest().body("Organization name is required when creating a new organization.");
            }
            if (teamName.isBlank()) {
                return ResponseEntity.badRequest().body("Team name is required when creating a new organization.");
            }
        } else {
            invitedTeam = teamRepository.findByJoinCode(inviteCode).orElse(null);
            if (invitedTeam == null) {
                return ResponseEntity.badRequest().body("Invite code is invalid.");
            }
            String invitedDomain = invitedTeam.getOrganization().getEmailDomain();
            if (invitedDomain != null && !invitedDomain.isBlank() && !invitedDomain.equalsIgnoreCase(emailDomain)) {
                return ResponseEntity.badRequest().body("Use your organization email address to join this team.");
            }
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setWorkEmail(normalizedWorkEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(creatingNewOrganization ? "Admin" : "User");
        user.setCreated_at(Instant.now());
        user.setCreated_by(null);

        userRepo.save(user);
        if (creatingNewOrganization) {
            workspaceBootstrapService.createWorkspaceForNewOrganization(user, organizationName, teamName, emailDomain);
            return ResponseEntity.ok("Organization owner registered successfully!");
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
        return ResponseEntity.ok("User registered successfully and joined the invited team.");
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
                    user.getFirstName(),
                    user.getLastName(),
                    user.getWorkEmail(),
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

    private String normalizeWorkEmail(String workEmail) {
        return workEmail == null ? "" : workEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String extractEmailDomain(String workEmail) {
        int atIndex = workEmail.indexOf('@');
        if (atIndex <= 0 || atIndex == workEmail.length() - 1) {
            return "";
        }
        return workEmail.substring(atIndex + 1).toLowerCase(Locale.ROOT);
    }
}
