package com.example.backend.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.dto.TeamMembershipUpdateRequest;
import com.example.backend.dto.TeamMembershipRoleUpdateRequest;
import com.example.backend.dto.UserRoleUpdateRequest;
import com.example.backend.dto.UserSummary;
import com.example.backend.service.AccountDeletionService;
import com.example.backend.service.CurrentWorkspaceService;
import com.example.backend.service.UserAccessAdminService;
import com.example.backend.service.WorkspaceAccessService;
import com.example.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserRepository userRepository;
    private final UserAccessAdminService userAccessAdminService;
    private final AccountDeletionService accountDeletionService;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public UserAdminController(
            UserRepository userRepository,
            UserAccessAdminService userAccessAdminService,
            AccountDeletionService accountDeletionService,
            CurrentWorkspaceService currentWorkspaceService,
            WorkspaceAccessService workspaceAccessService) {
        this.userRepository = userRepository;
        this.userAccessAdminService = userAccessAdminService;
        this.accountDeletionService = accountDeletionService;
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<UserSummary> getAllUsers() {
        workspaceAccessService.requireGlobalAdmin();
        return userAccessAdminService.getUsersForCurrentOrganization();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(
        @PathVariable Long id,
        @RequestBody UserRoleUpdateRequest request
    ) {
        workspaceAccessService.requireGlobalAdmin();
        if (request == null || request.getRole() == null || request.getRole().isBlank()) {
            return ResponseEntity.badRequest().body("Role is required.");
        }

        String normalizedRole = normalizeRole(request.getRole());
        if (normalizedRole == null) {
            return ResponseEntity.badRequest().body("Role must be User or Admin.");
        }

        return userRepository.findById(id)
            .map(user -> {
                if ("Admin".equalsIgnoreCase(user.getRole())
                        && "User".equalsIgnoreCase(normalizedRole)
                        && userRepository.countByRole("Admin") <= 1) {
                    return ResponseEntity.badRequest().body("At least one Admin must remain.");
                }
                var actingUser = currentWorkspaceService.getCurrentUser();
                if (actingUser.getU_id().equals(user.getU_id())
                        && "User".equalsIgnoreCase(normalizedRole)
                        && userRepository.countByRole("Admin") <= 1) {
                    return ResponseEntity.badRequest().body("You cannot remove the last Admin role from your own account.");
                }
                user.setRole(normalizedRole);
                userRepository.save(user);
                return ResponseEntity.ok(
                    new UserSummary(user.getU_id(), user.getUsername(), user.getRole())
                );
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/teams")
    public ResponseEntity<?> assignUserToTeam(
            @PathVariable Long id,
            @RequestBody TeamMembershipUpdateRequest request) {
        try {
            workspaceAccessService.requireGlobalAdmin();
            if (request == null || request.getTeamId() == null) {
                return ResponseEntity.badRequest().body("Team id is required.");
            }
            return ResponseEntity.ok(userAccessAdminService.assignUserToTeam(id, request.getTeamId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    public ResponseEntity<?> removeUserFromTeam(
            @PathVariable Long id,
            @PathVariable Long teamId) {
        try {
            workspaceAccessService.requireGlobalAdmin();
            return ResponseEntity.ok(userAccessAdminService.removeUserFromTeam(id, teamId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(accountDeletionService.deleteUserAsOrganizationAdmin(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    @PutMapping("/{id}/teams/{teamId}/role")
    public ResponseEntity<?> updateUserTeamRole(
            @PathVariable Long id,
            @PathVariable Long teamId,
            @RequestBody TeamMembershipRoleUpdateRequest request) {
        try {
            workspaceAccessService.requireGlobalAdmin();
            if (request == null || request.getRole() == null || request.getRole().isBlank()) {
                return ResponseEntity.badRequest().body("Team role is required.");
            }
            return ResponseEntity.ok(userAccessAdminService.updateUserTeamRole(id, teamId, request.getRole()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    private String normalizeRole(String input) {
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        return switch (trimmed) {
            case "admin" -> "Admin";
            case "user" -> "User";
            default -> null;
        };
    }
}
