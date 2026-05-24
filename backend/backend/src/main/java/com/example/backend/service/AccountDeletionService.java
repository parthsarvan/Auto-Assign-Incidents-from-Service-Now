package com.example.backend.service;

import com.example.backend.dto.AccountDeletionResponse;
import com.example.backend.entity.Organization;
import com.example.backend.entity.OrganizationMembership;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.TeamMembership;
import com.example.backend.entity.User;
import com.example.backend.repository.BreakEntryRepository;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.LeaveEntryRepository;
import com.example.backend.repository.MobileDeviceTokenRepository;
import com.example.backend.repository.OrganizationMembershipRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.repository.TeamMemberScheduleRepository;
import com.example.backend.repository.TeamMembershipRepository;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final WorkspaceAccessService workspaceAccessService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final MobileDeviceTokenRepository mobileDeviceTokenRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final TeamMemberScheduleRepository teamMemberScheduleRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final BreakEntryRepository breakEntryRepository;

    public AccountDeletionService(
            CurrentWorkspaceService currentWorkspaceService,
            WorkspaceAccessService workspaceAccessService,
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamMembershipRepository teamMembershipRepository,
            OrganizationMembershipRepository organizationMembershipRepository,
            MobileDeviceTokenRepository mobileDeviceTokenRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            LeaveEntryRepository leaveEntryRepository,
            BreakEntryRepository breakEntryRepository) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.workspaceAccessService = workspaceAccessService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.mobileDeviceTokenRepository = mobileDeviceTokenRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.breakEntryRepository = breakEntryRepository;
    }

    @Transactional
    public AccountDeletionResponse deleteCurrentAccount() {
        User targetUser = currentWorkspaceService.getCurrentUser();
        return deleteUserAccount(targetUser, true);
    }

    @Transactional
    public AccountDeletionResponse deleteUserAsOrganizationAdmin(Long userId) {
        workspaceAccessService.requireGlobalAdmin();
        User actingUser = currentWorkspaceService.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Organization organization = actingUser.getCurrentOrganization();
        if (organization == null || !belongsToOrganization(targetUser, organization)) {
            throw new IllegalStateException("You can only delete users in your current organization.");
        }
        return deleteUserAccount(targetUser, actingUser.getU_id().equals(targetUser.getU_id()));
    }

    @Transactional
    public AccountDeletionResponse deleteTeamMemberFromCurrentTeam(Long teamMemberId) {
        workspaceAccessService.requireCurrentTeamManager();
        User actingUser = currentWorkspaceService.getCurrentUser();
        Team team = currentWorkspaceService.getCurrentTeam();
        TeamMember teamMember = teamMemberRepository.findByIdAndTeam(teamMemberId, team)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found."));

        String email = normalizeEmail(teamMember.getEmail());
        if (!email.isBlank()) {
            var linkedUser = userRepository.findByNormalizedWorkEmail(email)
                    .filter(user -> belongsToOrganization(user, team.getOrganization()));
            if (linkedUser.isPresent()) {
                if (!workspaceAccessService.isGlobalAdmin(actingUser)) {
                    throw new IllegalStateException(
                            "Only organization admins can delete a team member with a linked InciTeam account.");
                }
                return deleteUserAccount(linkedUser.get(), actingUser.getU_id().equals(linkedUser.get().getU_id()));
            }
        }

        int deletedTeamMembers = deleteTeamMemberRecord(teamMember);
        return new AccountDeletionResponse(
                null,
                null,
                false,
                deletedTeamMembers,
                0,
                0,
                0,
                "Team member and related routing, schedule, leave, and break records were deleted.");
    }

    private AccountDeletionResponse deleteUserAccount(User targetUser, boolean selfDeletion) {
        User managedUser = userRepository.findById(targetUser.getU_id())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        List<TeamMembership> teamMemberships = teamMembershipRepository.findAllByUserWithTeam(managedUser);
        List<OrganizationMembership> organizationMemberships =
                organizationMembershipRepository.findAllByUserWithOrganization(managedUser);
        List<Organization> organizations = collectOrganizations(managedUser, teamMemberships, organizationMemberships);

        ensureAccountCanBeDeleted(managedUser, teamMemberships, organizations, selfDeletion);

        int mobileTokensDeleted = Math.toIntExact(mobileDeviceTokenRepository.deleteByUser(managedUser));
        int teamMemberRecordsDeleted = deleteTeamMemberRecordsForUser(managedUser, organizations);
        int teamMembershipsDeleted = Math.toIntExact(teamMembershipRepository.deleteByUser(managedUser));
        int organizationMembershipsDeleted = Math.toIntExact(organizationMembershipRepository.deleteByUser(managedUser));

        Long deletedUserId = managedUser.getU_id();
        String deletedUsername = managedUser.getUsername();
        userRepository.delete(managedUser);

        return new AccountDeletionResponse(
                deletedUserId,
                deletedUsername,
                true,
                teamMemberRecordsDeleted,
                teamMembershipsDeleted,
                organizationMembershipsDeleted,
                mobileTokensDeleted,
                "Account deleted. The user was removed from team access, roster records, routing, schedules, leaves, breaks, and push notification tokens.");
    }

    private void ensureAccountCanBeDeleted(
            User user,
            List<TeamMembership> teamMemberships,
            List<Organization> organizations,
            boolean selfDeletion) {
        if ("Admin".equalsIgnoreCase(user.getRole())) {
            for (Organization organization : organizations) {
                long adminCount = teamMembershipRepository.countDistinctUsersByOrganizationAndUserRole(
                        organization,
                        "Admin");
                if (adminCount <= 1) {
                    throw new IllegalStateException(selfDeletion
                            ? "Add another organization Admin before deleting your own account."
                            : "Add another organization Admin before deleting this account.");
                }
            }
        }

        for (TeamMembership membership : teamMemberships) {
            if (!"TEAM_ADMIN".equalsIgnoreCase(membership.getRole())) {
                continue;
            }
            long teamAdminCount = teamMembershipRepository.countByTeamAndRole(membership.getTeam(), "TEAM_ADMIN");
            if (teamAdminCount <= 1) {
                String teamName = membership.getTeam().getName();
                throw new IllegalStateException(selfDeletion
                        ? "Assign another TEAM_ADMIN for " + teamName + " before deleting your own account."
                        : "Assign another TEAM_ADMIN for " + teamName + " before deleting this account.");
            }
        }
    }

    private boolean belongsToOrganization(User user, Organization organization) {
        if (user == null || organization == null) {
            return false;
        }
        if (user.getCurrentOrganization() != null
                && organization.getOrg_id().equals(user.getCurrentOrganization().getOrg_id())) {
            return true;
        }
        return teamMembershipRepository.findAllByUserWithTeam(user).stream()
                .anyMatch(membership ->
                        membership.getTeam().getOrganization().getOrg_id().equals(organization.getOrg_id()));
    }

    private List<Organization> collectOrganizations(
            User user,
            List<TeamMembership> teamMemberships,
            List<OrganizationMembership> organizationMemberships) {
        Map<Long, Organization> organizations = new LinkedHashMap<>();
        if (user.getCurrentOrganization() != null) {
            organizations.put(user.getCurrentOrganization().getOrg_id(), user.getCurrentOrganization());
        }
        for (TeamMembership membership : teamMemberships) {
            Organization organization = membership.getTeam().getOrganization();
            organizations.put(organization.getOrg_id(), organization);
        }
        for (OrganizationMembership membership : organizationMemberships) {
            Organization organization = membership.getOrganization();
            organizations.put(organization.getOrg_id(), organization);
        }
        return new ArrayList<>(organizations.values());
    }

    private int deleteTeamMemberRecordsForUser(User user, List<Organization> organizations) {
        String email = normalizeEmail(user.getWorkEmail());
        if (email.isBlank() || organizations.isEmpty()) {
            return 0;
        }
        Map<Long, TeamMember> teamMembersById = new LinkedHashMap<>();
        for (Organization organization : organizations) {
            for (TeamMember teamMember : teamMemberRepository.findAllByOrganizationAndNormalizedEmail(
                    organization,
                    email)) {
                teamMembersById.put(teamMember.getTm_id(), teamMember);
            }
        }
        int deleted = 0;
        for (TeamMember teamMember : teamMembersById.values()) {
            deleted += deleteTeamMemberRecord(teamMember);
        }
        return deleted;
    }

    private int deleteTeamMemberRecord(TeamMember teamMember) {
        for (Team team : teamRepository.findAllByUnsupportedCiFallbackTeamMember(teamMember)) {
            team.setUnsupportedCiFallbackTeamMember(null);
            teamRepository.save(team);
        }
        ciUserMappingRepository.deleteByTeamMember(teamMember);
        teamMemberScheduleRepository.deleteByTeamMember(teamMember);
        leaveEntryRepository.deleteByTeamMember(teamMember);
        breakEntryRepository.deleteByTeamMember(teamMember);
        teamMemberRepository.delete(teamMember);
        return 1;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
