package com.example.backend.service;

import com.example.backend.dto.SetupStatusResponse;
import com.example.backend.dto.SetupStepStatus;
import com.example.backend.entity.Team;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
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
    private final GeoShiftMappingRepository geoShiftMappingRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final TeamMemberScheduleRepository teamMemberScheduleRepository;
    private final CurrentWorkspaceService currentWorkspaceService;

    public SetupStatusService(
            GeoRepository geoRepository,
            ShiftRepository shiftRepository,
            ConfigurationItemRepository configurationItemRepository,
            TeamMemberRepository teamMemberRepository,
            GeoShiftMappingRepository geoShiftMappingRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            CurrentWorkspaceService currentWorkspaceService) {
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.currentWorkspaceService = currentWorkspaceService;
    }

    public SetupStatusResponse getStatus() {
        Team team = currentWorkspaceService.getCurrentTeam();
        List<SetupStepStatus> steps = List.of(
                requiredStep("geos", "Geos", geoRepository.countByTeam(team), "/geos",
                        "Define the geographic regions your team supports."),
                requiredStep("shifts", "Shifts", shiftRepository.countByTeam(team), "/shifts",
                        "Create the shift names used across your team."),
                requiredStep("geo_shift_mappings", "Geo Shift Mappings", geoShiftMappingRepository.countByTeam(team), "/geo-shift-mappings",
                        "Connect each geo to the shifts that can run there."),
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
}
