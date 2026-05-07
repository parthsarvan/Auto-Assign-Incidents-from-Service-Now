package com.example.backend.service;

import com.example.backend.dto.WorkspaceSummary;
import com.example.backend.entity.Organization;
import com.example.backend.entity.OrganizationMembership;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMembership;
import com.example.backend.entity.User;
import com.example.backend.entity.Geo;
import com.example.backend.entity.Shift;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.GeoRepository;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.TeamMemberSchedule;
import com.example.backend.entity.LeaveEntry;
import com.example.backend.entity.BreakEntry;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.TeamMemberScheduleRepository;
import com.example.backend.repository.LeaveEntryRepository;
import com.example.backend.repository.BreakEntryRepository;
import com.example.backend.repository.OrganizationMembershipRepository;
import com.example.backend.repository.OrganizationRepository;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.repository.TeamMembershipRepository;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceBootstrapService {
    public static final String DEFAULT_ORG_NAME = "Default Organization";
    public static final String DEFAULT_ORG_SLUG = "default-organization";
    public static final String DEFAULT_TEAM_NAME = "Default Team";

    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;
    private final GeoRepository geoRepository;
    private final ShiftRepository shiftRepository;
    private final ConfigurationItemRepository configurationItemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final GeoShiftMappingRepository geoShiftMappingRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final TeamMemberScheduleRepository teamMemberScheduleRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final BreakEntryRepository breakEntryRepository;

    public WorkspaceBootstrapService(
            OrganizationRepository organizationRepository,
            TeamRepository teamRepository,
            OrganizationMembershipRepository organizationMembershipRepository,
            TeamMembershipRepository teamMembershipRepository,
            UserRepository userRepository,
            GeoRepository geoRepository,
            ShiftRepository shiftRepository,
            ConfigurationItemRepository configurationItemRepository,
            TeamMemberRepository teamMemberRepository,
            GeoShiftMappingRepository geoShiftMappingRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            LeaveEntryRepository leaveEntryRepository,
            BreakEntryRepository breakEntryRepository) {
        this.organizationRepository = organizationRepository;
        this.teamRepository = teamRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.userRepository = userRepository;
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.breakEntryRepository = breakEntryRepository;
    }

    @Transactional
    public void backfillDefaultWorkspace() {
        if (!requiresLegacyWorkspaceBootstrap()) {
            return;
        }
        Organization organization = ensureDefaultOrganization();
        Team team = ensureDefaultTeam(organization);
        ensureJoinCodes();
        for (User user : userRepository.findAll()) {
            attachUserToDefaultWorkspace(user, organization, team);
        }
        assignDefaultTeamToCoreRecords(team);
    }

    @Transactional
    public WorkspaceSummary ensureWorkspaceForUser(User user) {
        User managedUser = userRepository.findById(user.getU_id()).orElse(user);
        java.util.List<TeamMembership> memberships = teamMembershipRepository.findAllByUserWithTeam(managedUser);
        TeamMembership preferredTeamMembership = selectPreferredTeamMembership(managedUser, memberships);

        if (preferredTeamMembership != null) {
            Team team = preferredTeamMembership.getTeam();
            boolean currentMatchesPreferred =
                    managedUser.getCurrentTeam() != null
                            && managedUser.getCurrentOrganization() != null
                            && team.getTeam_id().equals(managedUser.getCurrentTeam().getTeam_id())
                            && team.getOrganization().getOrg_id().equals(managedUser.getCurrentOrganization().getOrg_id());
            if (currentMatchesPreferred) {
                return toSummary(managedUser);
            }
            managedUser.setCurrentTeam(team);
            managedUser.setCurrentOrganization(team.getOrganization());
            managedUser = userRepository.save(managedUser);
            return toSummary(managedUser);
        }

        if (!requiresLegacyWorkspaceBootstrap()) {
            return toSummary(managedUser);
        }

        Organization organization = ensureDefaultOrganization();
        Team team = ensureDefaultTeam(organization);
        User attachedUser = attachUserToDefaultWorkspace(managedUser, organization, team);
        return toSummary(attachedUser);
    }

    @Transactional
    public WorkspaceSummary createWorkspaceForNewOrganization(User user, String organizationName, String teamName, String emailDomain) {
        Organization organization = new Organization();
        organization.setName(organizationName);
        organization.setSlug(generateOrganizationSlug(organizationName));
        organization.setEmailDomain(emailDomain);
        organization.setCreated_at(Instant.now());
        organization = organizationRepository.save(organization);

        Team team = new Team();
        team.setOrganization(organization);
        team.setName(teamName);
        team.setDescription("Initial team created during organization onboarding.");
        team.setJoinCode(generateJoinCode());
        team.setCreated_at(Instant.now());
        team = teamRepository.save(team);

        OrganizationMembership organizationMembership = new OrganizationMembership();
        organizationMembership.setUser(user);
        organizationMembership.setOrganization(organization);
        organizationMembership.setRole("ORG_ADMIN");
        organizationMembership.setCreated_at(Instant.now());
        organizationMembershipRepository.save(organizationMembership);

        TeamMembership teamMembership = new TeamMembership();
        teamMembership.setUser(user);
        teamMembership.setTeam(team);
        teamMembership.setRole("TEAM_ADMIN");
        teamMembership.setCreated_at(Instant.now());
        teamMembershipRepository.save(teamMembership);

        user.setCurrentOrganization(organization);
        user.setCurrentTeam(team);
        userRepository.save(user);
        return toSummary(user);
    }

    @Transactional
    public Team getDefaultTeam() {
        Organization organization = ensureDefaultOrganization();
        return ensureDefaultTeam(organization);
    }

    @Transactional
    public WorkspaceSummary getWorkspaceSummary(User user) {
        User managedUser = userRepository.findById(user.getU_id()).orElse(user);
        if (managedUser.getCurrentOrganization() == null || managedUser.getCurrentTeam() == null) {
            return ensureWorkspaceForUser(managedUser);
        }
        return toSummary(managedUser);
    }

    private Organization ensureDefaultOrganization() {
        return organizationRepository.findBySlug(DEFAULT_ORG_SLUG)
                .orElseGet(() -> {
                    Organization organization = new Organization();
                    organization.setName(DEFAULT_ORG_NAME);
                    organization.setSlug(DEFAULT_ORG_SLUG);
                    organization.setCreated_at(Instant.now());
                    return organizationRepository.save(organization);
                });
    }

    private boolean requiresLegacyWorkspaceBootstrap() {
        if (organizationRepository.count() > 0 || teamRepository.count() > 0) {
            return false;
        }
        return userRepository.count() > 0
                || geoRepository.count() > 0
                || shiftRepository.count() > 0
                || configurationItemRepository.count() > 0
                || teamMemberRepository.count() > 0
                || geoShiftMappingRepository.count() > 0
                || ciUserMappingRepository.count() > 0
                || teamMemberScheduleRepository.count() > 0
                || leaveEntryRepository.count() > 0
                || breakEntryRepository.count() > 0;
    }

    private Team ensureDefaultTeam(Organization organization) {
        return teamRepository.findByOrganizationAndName(organization, DEFAULT_TEAM_NAME)
                .orElseGet(() -> {
                    Team team = new Team();
                    team.setOrganization(organization);
                    team.setName(DEFAULT_TEAM_NAME);
                    team.setDescription("Bootstrap team for existing InciTeam workspace.");
                    team.setJoinCode(generateJoinCode());
                    team.setCreated_at(Instant.now());
                    return teamRepository.save(team);
                });
    }

    private User attachUserToDefaultWorkspace(User user, Organization organization, Team team) {
        if (!organizationMembershipRepository.existsByUserAndOrganization(user, organization)) {
            OrganizationMembership organizationMembership = new OrganizationMembership();
            organizationMembership.setUser(user);
            organizationMembership.setOrganization(organization);
            organizationMembership.setRole(mapOrganizationRole(user.getRole()));
            organizationMembership.setCreated_at(Instant.now());
            organizationMembershipRepository.save(organizationMembership);
        }

        if (!teamMembershipRepository.existsByUserAndTeam(user, team)) {
            TeamMembership teamMembership = new TeamMembership();
            teamMembership.setUser(user);
            teamMembership.setTeam(team);
            teamMembership.setRole(mapTeamRole(user.getRole()));
            teamMembership.setCreated_at(Instant.now());
            teamMembershipRepository.save(teamMembership);
        }

        boolean dirty = false;
        if (user.getCurrentOrganization() == null || !organization.getOrg_id().equals(user.getCurrentOrganization().getOrg_id())) {
            user.setCurrentOrganization(organization);
            dirty = true;
        }
        if (user.getCurrentTeam() == null || !team.getTeam_id().equals(user.getCurrentTeam().getTeam_id())) {
            user.setCurrentTeam(team);
            dirty = true;
        }
        if (dirty) {
            user = userRepository.save(user);
        }
        return user;
    }

    private void assignDefaultTeamToCoreRecords(Team team) {
        for (Geo geo : geoRepository.findAll()) {
            if (geo.getTeam() == null) {
                geo.setTeam(team);
                geoRepository.save(geo);
            }
        }
        for (Shift shift : shiftRepository.findAll()) {
            if (shift.getTeam() == null) {
                shift.setTeam(team);
                shiftRepository.save(shift);
            }
        }
        for (ConfigurationItem configurationItem : configurationItemRepository.findAll()) {
            if (configurationItem.getTeam() == null) {
                configurationItem.setTeam(team);
                configurationItemRepository.save(configurationItem);
            }
        }
        for (TeamMember teamMember : teamMemberRepository.findAll()) {
            if (teamMember.getTeam() == null) {
                teamMember.setTeam(team);
                teamMemberRepository.save(teamMember);
            }
        }
        for (GeoShiftMapping mapping : geoShiftMappingRepository.findAll()) {
            if (mapping.getTeam() == null) {
                mapping.setTeam(team);
                if (mapping.getStartTime() == null || mapping.getEndTime() == null) {
                    mapping.syncTimesFromShift();
                }
                geoShiftMappingRepository.save(mapping);
            }
        }
        for (CiUserMapping mapping : ciUserMappingRepository.findAll()) {
            if (mapping.getTeam() == null) {
                mapping.setTeam(team);
                ciUserMappingRepository.save(mapping);
            }
        }
        for (TeamMemberSchedule schedule : teamMemberScheduleRepository.findAll()) {
            if (schedule.getTeam() == null) {
                schedule.setTeam(team);
                teamMemberScheduleRepository.save(schedule);
            }
        }
        for (LeaveEntry leaveEntry : leaveEntryRepository.findAll()) {
            if (leaveEntry.getTeam() == null) {
                leaveEntry.setTeam(team);
                leaveEntryRepository.save(leaveEntry);
            }
        }
        for (BreakEntry breakEntry : breakEntryRepository.findAll()) {
            if (breakEntry.getTeam() == null) {
                breakEntry.setTeam(team);
                breakEntryRepository.save(breakEntry);
            }
        }
    }

    private WorkspaceSummary toSummary(User user) {
        return new WorkspaceSummary(
                user.getCurrentOrganization() != null ? user.getCurrentOrganization().getOrg_id() : null,
                user.getCurrentOrganization() != null ? user.getCurrentOrganization().getName() : null,
                user.getCurrentTeam() != null ? user.getCurrentTeam().getTeam_id() : null,
                user.getCurrentTeam() != null ? user.getCurrentTeam().getName() : null,
                getCurrentTeamRole(user),
                user.getCurrentTeam() != null ? user.getCurrentTeam().getTimezone() : null);
    }

    private TeamMembership selectPreferredTeamMembership(User user, java.util.List<TeamMembership> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            return null;
        }

        TeamMembership currentMembership = memberships.stream()
                .filter(membership ->
                        user.getCurrentTeam() != null
                                && membership.getTeam().getTeam_id().equals(user.getCurrentTeam().getTeam_id()))
                .findFirst()
                .orElse(null);

        if (currentMembership != null && !shouldPreferNonDefaultMembership(currentMembership, memberships)) {
            return currentMembership;
        }

        return memberships.stream()
                .filter(membership -> !isLegacyDefaultTeam(membership.getTeam()))
                .findFirst()
                .orElse(currentMembership != null ? currentMembership : memberships.get(0));
    }

    private boolean shouldPreferNonDefaultMembership(TeamMembership currentMembership, java.util.List<TeamMembership> memberships) {
        return memberships.size() > 1 && isLegacyDefaultTeam(currentMembership.getTeam());
    }

    private boolean isLegacyDefaultTeam(Team team) {
        return team != null
                && DEFAULT_TEAM_NAME.equals(team.getName())
                && team.getOrganization() != null
                && DEFAULT_ORG_SLUG.equals(team.getOrganization().getSlug());
    }

    private String getCurrentTeamRole(User user) {
        if (user == null || user.getCurrentTeam() == null) {
            return null;
        }
        return teamMembershipRepository.findByUserAndTeam(user, user.getCurrentTeam())
                .map(TeamMembership::getRole)
                .orElse("Admin".equalsIgnoreCase(user.getRole()) ? "TEAM_ADMIN" : null);
    }

    private String mapOrganizationRole(String userRole) {
        return "Admin".equalsIgnoreCase(userRole) ? "ORG_ADMIN" : "ORG_MEMBER";
    }

    private String mapTeamRole(String userRole) {
        return "Admin".equalsIgnoreCase(userRole) ? "TEAM_ADMIN" : "MEMBER";
    }

    private void ensureJoinCodes() {
        for (Team existingTeam : teamRepository.findAll()) {
            if (existingTeam.getJoinCode() == null || existingTeam.getJoinCode().isBlank()) {
                existingTeam.setJoinCode(generateJoinCode());
                teamRepository.save(existingTeam);
            }
        }
    }

    private String generateJoinCode() {
        String code;
        do {
            code = "TEAM-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (teamRepository.existsByJoinCode(code));
        return code;
    }

    private String generateOrganizationSlug(String organizationName) {
        String base = organizationName == null ? "organization" : organizationName
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "organization";
        }

        String candidate = base;
        int suffix = 2;
        while (organizationRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
