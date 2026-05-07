package com.example.backend.service;

import com.example.backend.dto.SetupStatusResponse;
import com.example.backend.dto.SetupStepStatus;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Team;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.ShiftRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.example.backend.repository.TeamMemberScheduleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SetupStatusService {
    private final GeoRepository geoRepository;
    private final ShiftRepository shiftRepository;
    private final ConfigurationItemRepository configurationItemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final TeamMemberScheduleRepository teamMemberScheduleRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;

    public SetupStatusService(
            GeoRepository geoRepository,
            ShiftRepository shiftRepository,
            ConfigurationItemRepository configurationItemRepository,
            TeamMemberRepository teamMemberRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            CurrentWorkspaceService currentWorkspaceService,
            OrganizationServiceNowConfigService organizationServiceNowConfigService) {
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.currentWorkspaceService = currentWorkspaceService;
        this.organizationServiceNowConfigService = organizationServiceNowConfigService;
    }

    public SetupStatusResponse getStatus() {
        Team team = currentWorkspaceService.getCurrentTeam();
        Organization organization = team.getOrganization();
        List<SetupStepStatus> steps = List.of(
                requiredStep(
                        "servicenow_connection",
                        "Connect ServiceNow",
                        organizationServiceNowConfigService.isConfiguredForTeam(team) ? 1 : 0,
                        "/setup",
                        "Connect your organization's ServiceNow instance and set the assignment groups for this team."),
                requiredStep("geos", "Geos", geoRepository.countByTeam(team), "/geos",
                        "Define the geographic regions your team supports."),
                new SetupStepStatus(
                        "shifts",
                        "Shifts",
                        shiftRepository.countByTeam(team),
                        hasConfiguredShifts(team),
                        true,
                        "/shifts",
                        "Set one team timezone and create the shift hours used across your geos."),
                requiredStep("configuration_items", "Configuration Items", configurationItemRepository.countByTeam(team), "/configuration-items",
                        "Add your supported CIs and their ServiceNow CI sys IDs."),
                requiredStep("team_members", "Team Members", teamMemberRepository.countByTeam(team), "/team-members",
                        "Add team members and their ServiceNow user sys IDs."),
                requiredStep("ci_user_mappings", "CI User Mappings", ciUserMappingRepository.countByTeam(team), "/ci-user-mappings",
                        "Map each CI to the team members who can own it."),
                optionalStep("schedules", "Schedules", teamMemberScheduleRepository.countByTeam(team), "/schedules",
                        "Optional but recommended: assign team members to geos and shifts over time."));

        List<SetupStepStatus> requiredSteps = steps.stream().filter(SetupStepStatus::isRequired).toList();
        int completedSteps = (int) requiredSteps.stream().filter(SetupStepStatus::isComplete).count();
        boolean brandNew = requiredSteps.stream().allMatch(step -> step.getCount() == 0);
        return new SetupStatusResponse(
                brandNew,
                completedSteps == requiredSteps.size(),
                completedSteps,
                requiredSteps.size(),
                steps);
    }

    private SetupStepStatus requiredStep(String key, String label, long count, String route, String description) {
        return new SetupStepStatus(key, label, count, count > 0, true, route, description);
    }

    private SetupStepStatus optionalStep(String key, String label, long count, String route, String description) {
        return new SetupStepStatus(key, label, count, count > 0, false, route, description);
    }

    private boolean hasConfiguredShifts(Team team) {
        return team.getTimezone() != null
                && !team.getTimezone().isBlank()
                && shiftRepository.countByTeam(team) > 0;
    }
}
