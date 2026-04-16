package com.example.backend.service;

import com.example.backend.dto.TeamSummary;
import com.example.backend.dto.WorkspaceSummary;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.Geo;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Shift;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.TeamMembership;
import com.example.backend.entity.User;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.repository.TeamMembershipRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamWorkspaceService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;
    private final GeoRepository geoRepository;
    private final ShiftRepository shiftRepository;
    private final ConfigurationItemRepository configurationItemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final GeoShiftMappingRepository geoShiftMappingRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public TeamWorkspaceService(
            CurrentWorkspaceService currentWorkspaceService,
            TeamRepository teamRepository,
            TeamMembershipRepository teamMembershipRepository,
            UserRepository userRepository,
            GeoRepository geoRepository,
            ShiftRepository shiftRepository,
            ConfigurationItemRepository configurationItemRepository,
            TeamMemberRepository teamMemberRepository,
            GeoShiftMappingRepository geoShiftMappingRepository,
            CiUserMappingRepository ciUserMappingRepository,
            WorkspaceAccessService workspaceAccessService) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.userRepository = userRepository;
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional(readOnly = true)
    public List<TeamSummary> getAccessibleTeams() {
        User user = currentWorkspaceService.getCurrentUser();
        boolean admin = isAdmin(user);
        Organization organization = user.getCurrentOrganization();
        Long currentTeamId = user.getCurrentTeam() != null ? user.getCurrentTeam().getTeam_id() : null;
        if (admin) {
            return teamRepository.findAllByOrganizationOrderByNameAsc(organization).stream()
                    .map(team -> new TeamSummary(
                            team.getTeam_id(),
                            team.getName(),
                            team.getDescription(),
                            team.getJoinCode(),
                            team.getTeam_id().equals(currentTeamId)))
                    .toList();
        }
        return teamMembershipRepository.findAllByUserWithTeam(user).stream()
                .map(TeamMembership::getTeam)
                .filter(team -> team.getOrganization().getOrg_id().equals(organization.getOrg_id()))
                .map(team -> new TeamSummary(
                        team.getTeam_id(),
                        team.getName(),
                        team.getDescription(),
                        team.getJoinCode(),
                        team.getTeam_id().equals(currentTeamId)))
                .toList();
    }

    @Transactional
    public WorkspaceSummary createTeam(String name, String description, Long copyFromTeamId) {
        User user = currentWorkspaceService.getCurrentUser();
        if (!isAdmin(user)) {
            throw new IllegalStateException("Only admin users can create teams.");
        }
        String normalizedName = normalizeTeamName(name);
        String normalizedDescription = normalizeDescription(description);
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Team name is required.");
        }
        Organization organization = user.getCurrentOrganization();
        if (teamRepository.existsByOrganizationAndNormalizedName(organization, normalizedName)) {
            throw new IllegalArgumentException("A team with that name already exists in this organization.");
        }

        Team team = new Team();
        team.setOrganization(organization);
        team.setName(normalizedName);
        team.setDescription(normalizedDescription);
        team.setJoinCode(generateJoinCode());
        team.setCreated_at(Instant.now());
        Team savedTeam = teamRepository.save(team);

        if (copyFromTeamId != null) {
            cloneSetup(savedTeam, copyFromTeamId, organization);
        }

        if (!teamMembershipRepository.existsByUserAndTeam(user, savedTeam)) {
            TeamMembership membership = new TeamMembership();
            membership.setUser(user);
            membership.setTeam(savedTeam);
            membership.setRole("TEAM_ADMIN");
            membership.setCreated_at(Instant.now());
            teamMembershipRepository.save(membership);
        }

        user.setCurrentOrganization(organization);
        user.setCurrentTeam(savedTeam);
        userRepository.save(user);
        return new WorkspaceSummary(
                organization.getOrg_id(),
                organization.getName(),
                savedTeam.getTeam_id(),
                savedTeam.getName(),
                workspaceAccessService.getCurrentTeamRole(user));
    }

    private void cloneSetup(Team targetTeam, Long copyFromTeamId, Organization organization) {
        Team sourceTeam = teamRepository.findById(copyFromTeamId)
                .orElseThrow(() -> new IllegalArgumentException("Source team not found."));
        if (sourceTeam.getTeam_id().equals(targetTeam.getTeam_id())) {
            throw new IllegalArgumentException("Source and target teams must be different.");
        }
        if (!sourceTeam.getOrganization().getOrg_id().equals(organization.getOrg_id())) {
            throw new IllegalArgumentException("Source team must belong to the same organization.");
        }

        Map<Long, Geo> clonedGeos = new HashMap<>();
        for (Geo sourceGeo : geoRepository.findAllByTeamOrderByNameAsc(sourceTeam)) {
            Geo clonedGeo = new Geo();
            clonedGeo.setName(sourceGeo.getName());
            clonedGeo.setTeam(targetTeam);
            clonedGeo = geoRepository.save(clonedGeo);
            clonedGeos.put(sourceGeo.getG_id(), clonedGeo);
        }

        Map<Long, Shift> clonedShifts = new HashMap<>();
        for (Shift sourceShift : shiftRepository.findAllByTeamOrderByNameAsc(sourceTeam)) {
            Shift clonedShift = new Shift();
            clonedShift.setName(sourceShift.getName());
            clonedShift.setTeam(targetTeam);
            clonedShift = shiftRepository.save(clonedShift);
            clonedShifts.put(sourceShift.getS_id(), clonedShift);
        }

        Map<Long, ConfigurationItem> clonedConfigurationItems = new HashMap<>();
        for (ConfigurationItem sourceItem : configurationItemRepository.findAllByTeamOrderByNameAsc(sourceTeam)) {
            ConfigurationItem clonedItem = new ConfigurationItem();
            clonedItem.setName(sourceItem.getName());
            clonedItem.setDescription(sourceItem.getDescription());
            clonedItem.setServiceNowSysId(sourceItem.getServiceNowSysId());
            clonedItem.setTeam(targetTeam);
            clonedItem = configurationItemRepository.save(clonedItem);
            clonedConfigurationItems.put(sourceItem.getCi_id(), clonedItem);
        }

        Map<Long, TeamMember> clonedTeamMembers = new HashMap<>();
        for (TeamMember sourceMember : teamMemberRepository.findAllByTeamOrderByName(sourceTeam)) {
            TeamMember clonedMember = new TeamMember();
            clonedMember.setF_name(sourceMember.getF_name());
            clonedMember.setL_name(sourceMember.getL_name());
            clonedMember.setEmail(sourceMember.getEmail());
            clonedMember.setPhone(sourceMember.getPhone());
            clonedMember.setSys_id(sourceMember.getSys_id());
            clonedMember.setGeo(sourceMember.getGeo() != null ? clonedGeos.get(sourceMember.getGeo().getG_id()) : null);
            clonedMember.setTeam(targetTeam);
            clonedMember = teamMemberRepository.save(clonedMember);
            clonedTeamMembers.put(sourceMember.getTm_id(), clonedMember);
        }

        for (GeoShiftMapping sourceMapping : geoShiftMappingRepository.findAllByTeamWithGeoAndShift(sourceTeam)) {
            Geo clonedGeo = clonedGeos.get(sourceMapping.getGeo().getG_id());
            Shift clonedShift = clonedShifts.get(sourceMapping.getShift().getS_id());
            if (clonedGeo == null || clonedShift == null) {
                continue;
            }
            GeoShiftMapping clonedMapping = new GeoShiftMapping();
            clonedMapping.setGeo(clonedGeo);
            clonedMapping.setShift(clonedShift);
            clonedMapping.setTeam(targetTeam);
            geoShiftMappingRepository.save(clonedMapping);
        }

        for (CiUserMapping sourceMapping : ciUserMappingRepository.findAllByTeamWithDetails(sourceTeam)) {
            ConfigurationItem clonedConfigurationItem =
                    clonedConfigurationItems.get(sourceMapping.getConfigurationItem().getCi_id());
            TeamMember clonedTeamMember =
                    clonedTeamMembers.get(sourceMapping.getTeamMember().getTm_id());
            if (clonedConfigurationItem == null || clonedTeamMember == null) {
                continue;
            }
            CiUserMapping clonedMapping = new CiUserMapping();
            clonedMapping.setConfigurationItem(clonedConfigurationItem);
            clonedMapping.setTeamMember(clonedTeamMember);
            clonedMapping.setSortOrder(sourceMapping.getSortOrder());
            clonedMapping.setTeam(targetTeam);
            ciUserMappingRepository.save(clonedMapping);
        }
    }

    @Transactional
    public WorkspaceSummary switchTeam(Long teamId) {
        User user = currentWorkspaceService.getCurrentUser();
        if (teamId == null) {
            throw new IllegalArgumentException("Team id is required.");
        }
        Team targetTeam = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found."));
        if (!targetTeam.getOrganization().getOrg_id().equals(user.getCurrentOrganization().getOrg_id())) {
            throw new IllegalStateException("That team does not belong to your organization.");
        }
        if (!isAdmin(user) && !teamMembershipRepository.existsByUserAndTeam(user, targetTeam)) {
            throw new IllegalStateException("You do not have access to that team.");
        }
        if (isAdmin(user) && !teamMembershipRepository.existsByUserAndTeam(user, targetTeam)) {
            TeamMembership membership = new TeamMembership();
            membership.setUser(user);
            membership.setTeam(targetTeam);
            membership.setRole("TEAM_ADMIN");
            membership.setCreated_at(Instant.now());
            teamMembershipRepository.save(membership);
        }
        user.setCurrentOrganization(targetTeam.getOrganization());
        user.setCurrentTeam(targetTeam);
        userRepository.save(user);
        return new WorkspaceSummary(
                targetTeam.getOrganization().getOrg_id(),
                targetTeam.getOrganization().getName(),
                targetTeam.getTeam_id(),
                targetTeam.getName(),
                workspaceAccessService.getCurrentTeamRole(user));
    }

    @Transactional
    public TeamSummary regenerateJoinCode(Long teamId) {
        User user = currentWorkspaceService.getCurrentUser();
        if (!isAdmin(user)) {
            throw new IllegalStateException("Only admin users can regenerate invite codes.");
        }
        if (teamId == null) {
            throw new IllegalArgumentException("Team id is required.");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found."));
        if (user.getCurrentOrganization() == null
                || !team.getOrganization().getOrg_id().equals(user.getCurrentOrganization().getOrg_id())) {
            throw new IllegalStateException("That team does not belong to your organization.");
        }

        team.setJoinCode(generateJoinCode());
        Team savedTeam = teamRepository.save(team);
        Long currentTeamId = user.getCurrentTeam() != null ? user.getCurrentTeam().getTeam_id() : null;
        return new TeamSummary(
                savedTeam.getTeam_id(),
                savedTeam.getName(),
                savedTeam.getDescription(),
                savedTeam.getJoinCode(),
                savedTeam.getTeam_id().equals(currentTeamId));
    }

    private boolean isAdmin(User user) {
        return "Admin".equalsIgnoreCase(user.getRole());
    }

    private String generateJoinCode() {
        String code;
        do {
            code = "TEAM-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (teamRepository.existsByJoinCode(code));
        return code;
    }

    private String normalizeTeamName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim().replaceAll("\\s{2,}", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
