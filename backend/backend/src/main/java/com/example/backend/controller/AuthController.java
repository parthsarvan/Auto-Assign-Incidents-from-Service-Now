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
import com.example.backend.security.AuthRateLimiter;
import com.example.backend.security.ClientIpAddressResolver;
import com.example.backend.security.JwtUtils;
import com.example.backend.service.WorkspaceBootstrapService;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password.";
    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_ORGANIZATION_NAME_LENGTH = 120;
    private static final int MAX_TEAM_NAME_LENGTH = 120;
    private static final int MAX_INVITE_CODE_LENGTH = 80;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._@-]{3,64}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

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

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @Autowired
    private ClientIpAddressResolver clientIpAddressResolver;

    @Value("${inciteam.security.password.min-length:12}")
    private int minPasswordLength;

    @Value("${inciteam.security.password.max-length:128}")
    private int maxPasswordLength;

    private String dummyPasswordHash;

    @PostConstruct
    public void initDummyPasswordHash() {
        this.dummyPasswordHash = passwordEncoder.encode("__inciteam_nonexistent_user_password__");
    }

    @PostMapping("/organization-discovery")
    public ResponseEntity<?> discoverOrganization(
            @RequestBody OrganizationDiscoveryRequest request,
            HttpServletRequest servletRequest) {
        var limit = authRateLimiter.consumeDiscoveryAttempt(clientIpAddressResolver.resolve(servletRequest));
        if (!limit.allowed()) {
            return rateLimited(limit);
        }

        if (request == null) {
            return ResponseEntity.badRequest().body("Organization and work email are required.");
        }

        String organizationName = normalizeName(request.getOrganizationName());
        String normalizedWorkEmail = normalizeWorkEmail(request.getWorkEmail());

        if (organizationName.isBlank()) {
            return ResponseEntity.badRequest().body("Organization name is required.");
        }
        if (organizationName.length() > MAX_ORGANIZATION_NAME_LENGTH) {
            return ResponseEntity.badRequest().body("Organization name is too long.");
        }
        if (normalizedWorkEmail.isBlank()) {
            return ResponseEntity.badRequest().body("Work email is required.");
        }
        if (!isValidEmail(normalizedWorkEmail)) {
            return ResponseEntity.badRequest().body("Enter a valid work email address.");
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
    public ResponseEntity<?> registerUser(
            @RequestBody SignupRequest signupRequest,
            HttpServletRequest servletRequest) {
        var limit = authRateLimiter.consumeSignupAttempt(clientIpAddressResolver.resolve(servletRequest));
        if (!limit.allowed()) {
            return rateLimited(limit);
        }

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
        String validationError = validateSignupFields(
                normalizedUsername,
                firstName,
                lastName,
                normalizedWorkEmail,
                password,
                inviteCode,
                organizationName,
                teamName);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(validationError);
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
    public ResponseEntity<?> authenticateUser(
            @RequestBody AuthRequest authRequest,
            HttpServletRequest servletRequest) {
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
        if (normalizedUsername.length() > MAX_USERNAME_LENGTH || password.length() > maxPasswordLength) {
            return invalidCredentials();
        }

        String clientIp = clientIpAddressResolver.resolve(servletRequest);
        var limit = authRateLimiter.consumeLoginAttempt(clientIp, normalizedUsername);
        if (!limit.allowed()) {
            return rateLimited(limit);
        }

        Optional<User> foundUser = userRepo.findByNormalizedUsername(normalizedUsername);
        if (foundUser.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash);
            return invalidCredentials();
        }

        User user = foundUser.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return invalidCredentials();
        }

        authRateLimiter.clearLoginAttempts(clientIp, normalizedUsername);
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
    }

    private ResponseEntity<String> invalidCredentials() {
        return ResponseEntity.status(401).body(INVALID_CREDENTIALS_MESSAGE);
    }

    private ResponseEntity<String> rateLimited(AuthRateLimiter.RateLimitResult limit) {
        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(limit.retryAfterSeconds()))
                .body("Too many attempts. Please wait before trying again.");
    }

    private String validateSignupFields(
            String username,
            String firstName,
            String lastName,
            String workEmail,
            String password,
            String inviteCode,
            String organizationName,
            String teamName) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username must be 3-64 characters and use only letters, numbers, dots, underscores, dashes, or @.";
        }
        if (!isValidEmail(workEmail)) {
            return "Enter a valid work email address.";
        }
        if (firstName.length() > MAX_NAME_LENGTH || lastName.length() > MAX_NAME_LENGTH) {
            return "Name fields are too long.";
        }
        if (organizationName.length() > MAX_ORGANIZATION_NAME_LENGTH) {
            return "Organization name is too long.";
        }
        if (teamName.length() > MAX_TEAM_NAME_LENGTH) {
            return "Team name is too long.";
        }
        if (inviteCode.length() > MAX_INVITE_CODE_LENGTH) {
            return "Invite code is too long.";
        }
        return validatePassword(password);
    }

    private String validatePassword(String password) {
        if (password.length() < minPasswordLength || password.length() > maxPasswordLength) {
            return "Password must be " + minPasswordLength + "-" + maxPasswordLength + " characters.";
        }

        int classes = 0;
        classes += password.chars().anyMatch(Character::isUpperCase) ? 1 : 0;
        classes += password.chars().anyMatch(Character::isLowerCase) ? 1 : 0;
        classes += password.chars().anyMatch(Character::isDigit) ? 1 : 0;
        classes += password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch)) ? 1 : 0;
        if (classes < 3) {
            return "Password must include at least three of: uppercase letters, lowercase letters, numbers, and symbols.";
        }
        return null;
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

    private boolean isValidEmail(String workEmail) {
        return workEmail.length() <= MAX_EMAIL_LENGTH && EMAIL_PATTERN.matcher(workEmail).matches();
    }

    private String extractEmailDomain(String workEmail) {
        int atIndex = workEmail.indexOf('@');
        if (atIndex <= 0 || atIndex == workEmail.length() - 1) {
            return "";
        }
        return workEmail.substring(atIndex + 1).toLowerCase(Locale.ROOT);
    }
}
