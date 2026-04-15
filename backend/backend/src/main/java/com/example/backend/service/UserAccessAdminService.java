package com.example.backend.service;

import com.example.backend.dto.UserSummary;
import com.example.backend.dto.UserTeamMembershipSummary;
import com.example.backend.dto.WorkspaceSummary;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMembership;
import com.example.backend.entity.User;
import com.example.backend.repository.TeamMembershipRepository;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccessAdminService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public UserAccessAdminService(
            CurrentWorkspaceService currentWorkspaceService,
            WorkspaceBootstrapService workspaceBootstrapService,
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMembershipRepository teamMembershipRepository) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    @Transactional
    public List<UserSummary> getUsersForCurrentOrganization() {
        User currentUser = currentWorkspaceService.getCurrentUser();
        Organization organization = currentUser.getCurrentOrganization();
        if (organization == null) {
            throw new IllegalStateException("Current organization is not available.");
        }

        Map<Long, UserSummaryBuilder> summaries = new LinkedHashMap<>();
        for (User user : userRepository.findAll()) {
            WorkspaceSummary workspace = workspaceBootstrapService.ensureWorkspaceForUser(user);
            if (workspace.getOrganizationId() == null
                    || !workspace.getOrganizationId().equals(organization.getOrg_id())) {
                continue;
            }
            summaries.put(user.getU_id(), new UserSummaryBuilder(user));
        }

        List<TeamMembership> memberships =
                teamMembershipRepository.findAllByOrganizationWithTeamAndUser(organization);
        for (TeamMembership membership : memberships) {
            User user = membership.getUser();
            UserSummaryBuilder builder = summaries.get(user.getU_id());
            if (builder == null) {
                continue;
            }
            Team team = membership.getTeam();
            boolean current = user.getCurrentTeam() != null
                    && user.getCurrentTeam().getTeam_id().equals(team.getTeam_id());
            builder.memberships.add(new UserTeamMembershipSummary(
                    team.getTeam_id(),
                    team.getName(),
                    membership.getRole(),
                    current));
        }

        return summaries.values().stream()
                .map(UserSummaryBuilder::build)
                .sorted(Comparator.comparing(UserSummary::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public UserSummary assignUserToTeam(Long userId, Long teamId) {
        User actingUser = currentWorkspaceService.getCurrentUser();
        ensureAdmin(actingUser);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        workspaceBootstrapService.ensureWorkspaceForUser(targetUser);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found."));
        if (actingUser.getCurrentOrganization() == null
                || !team.getOrganization().getOrg_id().equals(actingUser.getCurrentOrganization().getOrg_id())) {
            throw new IllegalStateException("That team does not belong to your organization.");
        }

        if (!teamMembershipRepository.existsByUserAndTeam(targetUser, team)) {
            TeamMembership membership = new TeamMembership();
            membership.setUser(targetUser);
            membership.setTeam(team);
            membership.setRole("MEMBER");
            membership.setCreated_at(Instant.now());
            teamMembershipRepository.save(membership);
        }

        if (targetUser.getCurrentTeam() == null
                || targetUser.getCurrentOrganization() == null
                || !targetUser.getCurrentOrganization().getOrg_id().equals(actingUser.getCurrentOrganization().getOrg_id())) {
            targetUser.setCurrentOrganization(actingUser.getCurrentOrganization());
            targetUser.setCurrentTeam(team);
            userRepository.save(targetUser);
        }

        return buildSummary(targetUser, actingUser.getCurrentOrganization());
    }

    @Transactional
    public UserSummary removeUserFromTeam(Long userId, Long teamId) {
        User actingUser = currentWorkspaceService.getCurrentUser();
        ensureAdmin(actingUser);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found."));
        Organization organization = actingUser.getCurrentOrganization();
        if (organization == null || !team.getOrganization().getOrg_id().equals(organization.getOrg_id())) {
            throw new IllegalStateException("That team does not belong to your organization.");
        }

        TeamMembership membership = teamMembershipRepository.findByUserAndTeam(targetUser, team)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to that team."));
        ensureTeamWillKeepAdmin(membership, null);

        List<TeamMembership> orgMemberships = teamMembershipRepository.findAllByUserWithTeam(targetUser).stream()
                .filter(tm -> tm.getTeam().getOrganization().getOrg_id().equals(organization.getOrg_id()))
                .toList();
        if (orgMemberships.size() <= 1) {
            throw new IllegalArgumentException("A user must keep access to at least one team.");
        }

        teamMembershipRepository.delete(membership);

        if (targetUser.getCurrentTeam() != null
                && targetUser.getCurrentTeam().getTeam_id().equals(team.getTeam_id())) {
            Team fallbackTeam = teamMembershipRepository.findAllByUserWithTeam(targetUser).stream()
                    .map(TeamMembership::getTeam)
                    .filter(t -> t.getOrganization().getOrg_id().equals(organization.getOrg_id()))
                    .findFirst()
                    .orElse(null);
            targetUser.setCurrentTeam(fallbackTeam);
            userRepository.save(targetUser);
        }

        return buildSummary(targetUser, organization);
    }

    @Transactional
    public UserSummary updateUserTeamRole(Long userId, Long teamId, String role) {
        User actingUser = currentWorkspaceService.getCurrentUser();
        ensureAdmin(actingUser);

        String normalizedRole = normalizeTeamRole(role);
        if (normalizedRole == null) {
            throw new IllegalArgumentException("Team role must be TEAM_ADMIN, MANAGER, or MEMBER.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found."));
        Organization organization = actingUser.getCurrentOrganization();
        if (organization == null || !team.getOrganization().getOrg_id().equals(organization.getOrg_id())) {
            throw new IllegalStateException("That team does not belong to your organization.");
        }

        TeamMembership membership = teamMembershipRepository.findByUserAndTeam(targetUser, team)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to that team."));
        ensureTeamWillKeepAdmin(membership, normalizedRole);
        membership.setRole(normalizedRole);
        teamMembershipRepository.save(membership);
        return buildSummary(targetUser, organization);
    }

    private UserSummary buildSummary(User user, Organization organization) {
        User managedUser = userRepository.findById(user.getU_id()).orElse(user);
        List<UserTeamMembershipSummary> memberships = teamMembershipRepository.findAllByUserWithTeam(managedUser).stream()
                .filter(tm -> tm.getTeam().getOrganization().getOrg_id().equals(organization.getOrg_id()))
                .map(tm -> new UserTeamMembershipSummary(
                        tm.getTeam().getTeam_id(),
                        tm.getTeam().getName(),
                        tm.getRole(),
                        managedUser.getCurrentTeam() != null
                                && managedUser.getCurrentTeam().getTeam_id().equals(tm.getTeam().getTeam_id())))
                .sorted(Comparator.comparing(UserTeamMembershipSummary::getTeamName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new UserSummary(
                managedUser.getU_id(),
                managedUser.getUsername(),
                managedUser.getRole(),
                managedUser.getCurrentTeam() != null ? managedUser.getCurrentTeam().getTeam_id() : null,
                managedUser.getCurrentTeam() != null ? managedUser.getCurrentTeam().getName() : null,
                memberships);
    }

    private void ensureAdmin(User user) {
        if (!"Admin".equalsIgnoreCase(user.getRole())) {
            throw new IllegalStateException("Only admin users can manage team access.");
        }
    }

    private String normalizeTeamRole(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim().toUpperCase(Locale.ROOT);
        return switch (trimmed) {
            case "TEAM_ADMIN" -> "TEAM_ADMIN";
            case "MANAGER" -> "MANAGER";
            case "MEMBER" -> "MEMBER";
            default -> null;
        };
    }

    private void ensureTeamWillKeepAdmin(TeamMembership membership, String nextRole) {
        if (!"TEAM_ADMIN".equalsIgnoreCase(membership.getRole())) {
            return;
        }
        if ("TEAM_ADMIN".equalsIgnoreCase(nextRole)) {
            return;
        }
        long adminCount = teamMembershipRepository.countByTeamAndRole(membership.getTeam(), "TEAM_ADMIN");
        if (adminCount <= 1) {
            throw new IllegalArgumentException("Each team must keep at least one TEAM_ADMIN.");
        }
    }

    private static class UserSummaryBuilder {
        private final User user;
        private final List<UserTeamMembershipSummary> memberships = new ArrayList<>();

        private UserSummaryBuilder(User user) {
            this.user = user;
        }

        private UserSummary build() {
            memberships.sort(Comparator.comparing(UserTeamMembershipSummary::getTeamName, String.CASE_INSENSITIVE_ORDER));
            return new UserSummary(
                    user.getU_id(),
                    user.getUsername(),
                    user.getRole(),
                    user.getCurrentTeam() != null ? user.getCurrentTeam().getTeam_id() : null,
                    user.getCurrentTeam() != null ? user.getCurrentTeam().getName() : null,
                    List.copyOf(memberships));
        }
    }
}
