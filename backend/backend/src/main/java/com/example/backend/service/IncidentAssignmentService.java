package com.example.backend.service;

import com.example.backend.dto.AssignmentCandidateCheck;
import com.example.backend.dto.IncidentAssignmentAnalysis;
import com.example.backend.dto.IncidentAssignmentDecision;
import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.TeamMemberSchedule;
import com.example.backend.entity.UnsupportedCiHandlingPolicy;
import com.example.backend.repository.BreakEntryRepository;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.LeaveEntryRepository;
import com.example.backend.repository.TeamMemberScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class IncidentAssignmentService {
    private final ConfigurationItemRepository configurationItemRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final TeamMemberScheduleRepository teamMemberScheduleRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final BreakEntryRepository breakEntryRepository;
    private final GeoShiftMappingRepository geoShiftMappingRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final ConcurrentHashMap<Long, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public IncidentAssignmentService(
            ConfigurationItemRepository configurationItemRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            LeaveEntryRepository leaveEntryRepository,
            BreakEntryRepository breakEntryRepository,
            GeoShiftMappingRepository geoShiftMappingRepository,
            CurrentWorkspaceService currentWorkspaceService) {
        this.configurationItemRepository = configurationItemRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.breakEntryRepository = breakEntryRepository;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
        this.currentWorkspaceService = currentWorkspaceService;
    }

    public IncidentAssignmentDecision determineAssignment(ServiceNowIncident incident) {
        return analyzeAssignment(incident, true).getDecision();
    }

    public IncidentAssignmentDecision determineAssignment(ServiceNowIncident incident, boolean advanceRoundRobin) {
        return analyzeAssignment(incident, advanceRoundRobin).getDecision();
    }

    public IncidentAssignmentAnalysis analyzeAssignment(ServiceNowIncident incident, boolean advanceRoundRobin) {
        Team team = currentWorkspaceService.getCurrentTeam();
        return analyzeAssignmentForTeam(team, incident, advanceRoundRobin, true);
    }

    private IncidentAssignmentAnalysis analyzeAssignmentForTeam(
            Team team,
            ServiceNowIncident incident,
            boolean advanceRoundRobin,
            boolean allowUnsupportedCiPolicy) {
        List<AssignmentCandidateCheck> candidateChecks = new ArrayList<>();
        String ciName = resolveDisplayValue(incident.getCmdb_ci());
        if (ciName == null || ciName.isBlank()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("Incident is missing a configuration item."),
                    candidateChecks);
        }

        String ciSysId = resolveReferenceValue(incident.getCmdb_ci());
        Optional<ConfigurationItem> configurationItem = findConfigurationItemForTeam(team, ciName, ciSysId);
        if (configurationItem.isEmpty()) {
            if (allowUnsupportedCiPolicy) {
                return handleUnsupportedCi(team, incident, ciName, ciSysId, advanceRoundRobin, candidateChecks);
            }
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("CI not configured for this team: " + ciName + "."),
                    candidateChecks);
        }

        ZoneId teamZone = resolveTeamZone(team);
        ZonedDateTime teamNow = ZonedDateTime.now(teamZone);
        List<ShiftWindow> activeShifts = resolveActiveShifts(team, teamNow.toLocalTime());
        if (activeShifts.isEmpty()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("No active shift window matches the current time."),
                    candidateChecks);
        }

        Instant now = teamNow.toInstant();
        LocalDate today = teamNow.toLocalDate();
        List<CiUserMapping> mappings =
                ciUserMappingRepository.findByConfigurationItemOrderBySortOrderAsc(configurationItem.get());
        if (mappings.isEmpty()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("No CI-user mapping found for CI: " + ciName + "."),
                    candidateChecks);
        }

        List<Candidate> eligible = new ArrayList<>();
        for (CiUserMapping mapping : sortedMappings(mappings)) {
            TeamMember member = mapping.getTeamMember();
            List<TeamMemberSchedule> schedules = teamMemberScheduleRepository.findActiveSchedules(member, today);
            MatchResult match = resolveShiftMatch(schedules, activeShifts);
            boolean onLeave = leaveEntryRepository.existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
                    member, now, now);
            boolean onBreak = breakEntryRepository.existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
                    member, now, now);
            AssignmentCandidateCheck candidateCheck = buildCandidateCheck(
                    mapping,
                    schedules,
                    match,
                    onLeave,
                    onBreak);
            candidateChecks.add(candidateCheck);
            if (match.matchType() == ShiftMatch.NONE) {
                continue;
            }
            if (onLeave) {
                continue;
            }
            if (onBreak) {
                continue;
            }
            eligible.add(new Candidate(mapping, match.window()));
        }

        if (eligible.isEmpty()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped(
                            "No eligible mapped team member is available for the current shift."),
                    candidateChecks);
        }

        int index = selectRoundRobinIndex(configurationItem.get().getCi_id(), eligible.size(), advanceRoundRobin);
        Candidate chosen = eligible.get(index);
        TeamMember chosenMember = chosen.mapping().getTeamMember();
        String fullName = String.format("%s %s", chosenMember.getF_name(), chosenMember.getL_name());
        markSelectedCandidate(candidateChecks, chosenMember);
        return new IncidentAssignmentAnalysis(
                IncidentAssignmentDecision.assigned(new IncidentAssignmentSuggestion(
                        fullName,
                        chosenMember.getEmail(),
                        chosenMember.getSys_id(),
                        chosenMember.getPhone(),
                        chosen.window().geoName(),
                        chosen.window().shiftName())),
                candidateChecks);
    }

    private Optional<ConfigurationItem> findConfigurationItemForTeam(Team team, String ciName, String ciSysId) {
        if (ciSysId != null && !ciSysId.isBlank()) {
            Optional<ConfigurationItem> bySysId =
                    configurationItemRepository.findByTeamAndNormalizedServiceNowSysId(team, ciSysId);
            if (bySysId.isPresent()) {
                return bySysId;
            }
        }
        return configurationItemRepository.findByNameAndTeam(ciName, team);
    }

    private IncidentAssignmentAnalysis handleUnsupportedCi(
            Team team,
            ServiceNowIncident incident,
            String ciName,
            String ciSysId,
            boolean advanceRoundRobin,
            List<AssignmentCandidateCheck> candidateChecks) {
        UnsupportedCiHandlingPolicy policy = team.getUnsupportedCiPolicy();
        if (policy == UnsupportedCiHandlingPolicy.ROUTE_TO_CI_OWNER) {
            Optional<ConfigurationItem> owningCi = findOwningConfigurationItem(team, ciName, ciSysId);
            if (owningCi.isEmpty()) {
                return new IncidentAssignmentAnalysis(
                        IncidentAssignmentDecision.skipped(
                                "CI not configured for this team and no owning team was found in this organization: "
                                        + ciName + "."),
                        candidateChecks);
            }

            Team owningTeam = owningCi.get().getTeam();
            IncidentAssignmentAnalysis ownerAnalysis =
                    analyzeAssignmentForTeam(owningTeam, incident, advanceRoundRobin, false);
            if (!ownerAnalysis.getDecision().hasSuggestion()) {
                return new IncidentAssignmentAnalysis(
                        IncidentAssignmentDecision.skipped(
                                "CI " + ciName + " is owned by " + owningTeam.getName()
                                        + ", but that team could not assign it: "
                                        + ownerAnalysis.getDecision().getReason()),
                        ownerAnalysis.getCandidates());
            }

            IncidentAssignmentSuggestion suggestion = ownerAnalysis.getDecision().getSuggestion();
            suggestion.setRoutedTeamName(owningTeam.getName());
            suggestion.setRoutingNote(
                    "InciTeam routed this incident to " + owningTeam.getName()
                            + " because CI '" + ciName + "' is not configured for "
                            + team.getName()
                            + ". Please update the CI or assignment group if this incident should be handled by "
                            + team.getName() + ".");
            return ownerAnalysis;
        }

        if (policy == UnsupportedCiHandlingPolicy.FALLBACK_TRIAGE_OWNER) {
            TeamMember fallbackTeamMember = team.getUnsupportedCiFallbackTeamMember();
            if (fallbackTeamMember == null) {
                return new IncidentAssignmentAnalysis(
                        IncidentAssignmentDecision.skipped(
                                "CI not configured for this team and no fallback triage owner is configured: "
                                        + ciName + "."),
                        candidateChecks);
            }

            String fullName = String.format("%s %s",
                    fallbackTeamMember.getF_name(),
                    fallbackTeamMember.getL_name()).trim();
            IncidentAssignmentSuggestion suggestion = new IncidentAssignmentSuggestion(
                    fullName,
                    fallbackTeamMember.getEmail(),
                    fallbackTeamMember.getSys_id(),
                    fallbackTeamMember.getPhone(),
                    fallbackTeamMember.getGeo() != null ? fallbackTeamMember.getGeo().getName() : null,
                    "Unsupported CI triage");
            suggestion.setRoutedTeamName(team.getName());
            suggestion.setRoutingNote(
                    "InciTeam assigned this incident to the unsupported-CI fallback triage owner because CI '"
                            + ciName + "' is not configured for " + team.getName() + ".");
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.assigned(suggestion),
                    candidateChecks);
        }

        return new IncidentAssignmentAnalysis(
                IncidentAssignmentDecision.skipped("CI not configured for this team: " + ciName + "."),
                candidateChecks);
    }

    private Optional<ConfigurationItem> findOwningConfigurationItem(Team currentTeam, String ciName, String ciSysId) {
        Organization organization = currentTeam.getOrganization();
        if (organization == null) {
            return Optional.empty();
        }
        if (ciSysId != null && !ciSysId.isBlank()) {
            List<ConfigurationItem> matches = configurationItemRepository.findOrganizationCiOwnersByServiceNowSysId(
                    organization,
                    currentTeam,
                    ciSysId);
            if (!matches.isEmpty()) {
                return Optional.of(matches.get(0));
            }
        }
        return configurationItemRepository.findOrganizationCiOwnersByName(
                        organization,
                        currentTeam,
                        ciName)
                .stream()
                .findFirst();
    }

    private List<CiUserMapping> sortedMappings(List<CiUserMapping> mappings) {
        return mappings.stream()
                .sorted(Comparator.comparing(CiUserMapping::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private MatchResult resolveShiftMatch(List<TeamMemberSchedule> schedules, List<ShiftWindow> activeShifts) {
        if (schedules.isEmpty()) {
            return new MatchResult(ShiftMatch.NONE, null);
        }
        for (TeamMemberSchedule schedule : schedules) {
            String geoName = schedule.getGeo().getName();
            String shiftName = schedule.getShift().getName();
            for (ShiftWindow window : activeShifts) {
                if (window.geoName().equalsIgnoreCase(geoName)
                        && window.shiftName().equalsIgnoreCase(shiftName)) {
                    return new MatchResult(ShiftMatch.EXACT, window);
                }
            }
        }
        return new MatchResult(ShiftMatch.NONE, null);
    }

    private AssignmentCandidateCheck buildCandidateCheck(
            CiUserMapping mapping,
            List<TeamMemberSchedule> schedules,
            MatchResult match,
            boolean onLeave,
            boolean onBreak) {
        TeamMember member = mapping.getTeamMember();
        String fullName = String.format("%s %s", member.getF_name(), member.getL_name());
        boolean eligible = match.matchType() != ShiftMatch.NONE && !onLeave && !onBreak;
        return new AssignmentCandidateCheck(
                fullName,
                member.getEmail(),
                member.getSys_id(),
                mapping.getSortOrder(),
                member.getGeo() != null ? member.getGeo().getName() : null,
                summarizeSchedules(schedules),
                describeMatch(match),
                onLeave,
                onBreak,
                eligible,
                false,
                buildCandidateReason(schedules, match, onLeave, onBreak, eligible));
    }

    private String summarizeSchedules(List<TeamMemberSchedule> schedules) {
        if (schedules.isEmpty()) {
            return "No active schedule";
        }
        return schedules.stream()
                .map(schedule -> schedule.getGeo().getName() + " / " + schedule.getShift().getName())
                .collect(Collectors.joining(", "));
    }

    private String describeMatch(MatchResult match) {
        return switch (match.matchType()) {
            case EXACT -> "Exact active shift match";
            case NONE -> "No active shift match";
        };
    }

    private String buildCandidateReason(
            List<TeamMemberSchedule> schedules,
            MatchResult match,
            boolean onLeave,
            boolean onBreak,
            boolean eligible) {
        if (schedules.isEmpty()) {
            return "No active schedule covers today.";
        }
        if (match.matchType() == ShiftMatch.NONE) {
            return "Active schedule does not match the current shift window.";
        }
        if (onLeave) {
            return "Team member is currently on leave.";
        }
        if (onBreak) {
            return "Team member is currently on break.";
        }
        if (eligible) {
            return "Eligible for assignment.";
        }
        return "Not eligible for assignment.";
    }

    private void markSelectedCandidate(List<AssignmentCandidateCheck> candidateChecks, TeamMember chosenMember) {
        for (AssignmentCandidateCheck candidateCheck : candidateChecks) {
            if (!candidateCheck.isEligible()) {
                continue;
            }
            boolean selected = matchesChosenMember(candidateCheck, chosenMember);
            candidateCheck.setSelected(selected);
            if (selected) {
                candidateCheck.setReason("Selected by round-robin for assignment.");
            } else if (candidateCheck.getReason() != null && candidateCheck.getReason().startsWith("Eligible")) {
                candidateCheck.setReason("Eligible but not selected by round-robin.");
            }
        }
    }

    private boolean matchesChosenMember(AssignmentCandidateCheck candidateCheck, TeamMember chosenMember) {
        if (chosenMember.getSys_id() != null
                && !chosenMember.getSys_id().isBlank()
                && chosenMember.getSys_id().equals(candidateCheck.getServiceNowUserSysId())) {
            return true;
        }
        return chosenMember.getEmail() != null
                && chosenMember.getEmail().equalsIgnoreCase(candidateCheck.getEmail());
    }

    private List<ShiftWindow> resolveActiveShifts(Team team, LocalTime teamLocalTime) {
        List<ShiftWindow> active = new ArrayList<>();
        for (GeoShiftMapping mapping : geoShiftMappingRepository.findAllByTeamWithGeoAndShift(team)) {
            if (mapping.getShift().getStartTime() == null || mapping.getShift().getEndTime() == null) {
                continue;
            }
            ShiftWindow window = new ShiftWindow(
                    mapping.getGeo().getName(),
                    mapping.getShift().getName(),
                    mapping.getShift().getStartTime(),
                    mapping.getShift().getEndTime());
            if (window.includes(teamLocalTime)) {
                active.add(window);
            }
        }
        return active;
    }

    private ZoneId resolveTeamZone(Team team) {
        String timezone = team.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalStateException("Current team does not have a timezone configured.");
        }
        return ZoneId.of(timezone);
    }

    private int selectRoundRobinIndex(Long ciId, int size, boolean advanceRoundRobin) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(ciId, key -> new AtomicInteger(0));
        return Math.floorMod(advanceRoundRobin ? counter.getAndIncrement() : counter.get(), size);
    }

    private String resolveDisplayValue(ServiceNowReference reference) {
        if (reference == null) {
            return null;
        }
        if (reference.getDisplayValue() != null && !reference.getDisplayValue().isBlank()) {
            return reference.getDisplayValue();
        }
        return reference.getValue();
    }

    private String resolveReferenceValue(ServiceNowReference reference) {
        if (reference == null || reference.getValue() == null || reference.getValue().isBlank()) {
            return null;
        }
        return reference.getValue().trim();
    }

    private record ShiftWindow(String geoName, String shiftName, LocalTime start, LocalTime end) {
        boolean includes(LocalTime now) {
            if (start.equals(end)) {
                return true;
            }
            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            }
            return !now.isBefore(start) || now.isBefore(end);
        }
    }

    private record MatchResult(ShiftMatch matchType, ShiftWindow window) {}

    private record Candidate(CiUserMapping mapping, ShiftWindow window) {}

    private enum ShiftMatch {
        NONE,
        EXACT
    }
}
