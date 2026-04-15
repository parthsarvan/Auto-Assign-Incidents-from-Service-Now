package com.example.backend.service;

import com.example.backend.dto.AssignmentCandidateCheck;
import com.example.backend.dto.IncidentAssignmentAnalysis;
import com.example.backend.dto.IncidentAssignmentDecision;
import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.TeamMemberSchedule;
import com.example.backend.repository.BreakEntryRepository;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.ConfigurationItemRepository;
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
    private static final ZoneId PT_ZONE = ZoneId.of("America/Los_Angeles");
    private static final List<ShiftWindow> SHIFT_WINDOWS = List.of(
            new ShiftWindow("AMR", "General", LocalTime.of(8, 30), LocalTime.of(17, 30)),
            new ShiftWindow("APAC", "General", LocalTime.of(17, 30), LocalTime.of(2, 30)),
            new ShiftWindow("INDIA", "Morning", LocalTime.of(17, 30), LocalTime.of(2, 30)),
            new ShiftWindow("INDIA", "General", LocalTime.of(20, 30), LocalTime.of(5, 30)),
            new ShiftWindow("INDIA", "Evening", LocalTime.of(0, 30), LocalTime.of(8, 30)),
            new ShiftWindow("EMEA", "General", LocalTime.of(0, 30), LocalTime.of(8, 30))
    );

    private final ConfigurationItemRepository configurationItemRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final TeamMemberScheduleRepository teamMemberScheduleRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final BreakEntryRepository breakEntryRepository;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final ConcurrentHashMap<Long, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public IncidentAssignmentService(
            ConfigurationItemRepository configurationItemRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            LeaveEntryRepository leaveEntryRepository,
            BreakEntryRepository breakEntryRepository,
            CurrentWorkspaceService currentWorkspaceService) {
        this.configurationItemRepository = configurationItemRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.breakEntryRepository = breakEntryRepository;
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
        List<AssignmentCandidateCheck> candidateChecks = new ArrayList<>();
        String ciName = resolveDisplayValue(incident.getCmdb_ci());
        if (ciName == null || ciName.isBlank()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("Incident is missing a configuration item."),
                    candidateChecks);
        }

        Optional<ConfigurationItem> configurationItem = configurationItemRepository.findByNameAndTeam(ciName, team);
        if (configurationItem.isEmpty()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("No configuration item record found for CI: " + ciName + "."),
                    candidateChecks);
        }

        List<ShiftWindow> activeShifts = resolveActiveShifts(ZonedDateTime.now(PT_ZONE).toLocalTime());
        if (activeShifts.isEmpty()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("No active shift window matches the current time."),
                    candidateChecks);
        }

        Instant now = Instant.now();
        LocalDate today = ZonedDateTime.now(PT_ZONE).toLocalDate();
        List<CiUserMapping> mappings =
                ciUserMappingRepository.findByConfigurationItemOrderBySortOrderAsc(configurationItem.get());
        if (mappings.isEmpty()) {
            return new IncidentAssignmentAnalysis(
                    IncidentAssignmentDecision.skipped("No CI-user mapping found for CI: " + ciName + "."),
                    candidateChecks);
        }

        List<Candidate> eligible = new ArrayList<>();
        List<Candidate> fallbackEligible = new ArrayList<>();
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
            Candidate candidate = new Candidate(mapping, match.window());
            if (match.matchType() == ShiftMatch.EXACT) {
                eligible.add(candidate);
            } else {
                fallbackEligible.add(candidate);
            }
        }

        if (eligible.isEmpty()) {
            eligible = fallbackEligible;
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

    private List<CiUserMapping> sortedMappings(List<CiUserMapping> mappings) {
        return mappings.stream()
                .sorted(Comparator.comparing(CiUserMapping::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private MatchResult resolveShiftMatch(List<TeamMemberSchedule> schedules, List<ShiftWindow> activeShifts) {
        if (schedules.isEmpty()) {
            return new MatchResult(ShiftMatch.NONE, null);
        }
        boolean geoMatch = false;
        boolean shiftMatch = false;
        ShiftWindow fallbackWindow = null;
        for (TeamMemberSchedule schedule : schedules) {
            String geoName = schedule.getGeo().getName();
            String shiftName = schedule.getShift().getName();
            for (ShiftWindow window : activeShifts) {
                if (window.geoName().equalsIgnoreCase(geoName)
                        && window.shiftName().equalsIgnoreCase(shiftName)) {
                    return new MatchResult(ShiftMatch.EXACT, window);
                }
                if (window.geoName().equalsIgnoreCase(geoName)) {
                    geoMatch = true;
                    fallbackWindow = window;
                }
                if (window.shiftName().equalsIgnoreCase(shiftName)) {
                    shiftMatch = true;
                    if (fallbackWindow == null) {
                        fallbackWindow = window;
                    }
                }
            }
        }
        if (geoMatch || shiftMatch) {
            return new MatchResult(ShiftMatch.PARTIAL, fallbackWindow != null ? fallbackWindow : activeShifts.get(0));
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
            case PARTIAL -> "Partial shift match";
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
        if (eligible && match.matchType() == ShiftMatch.PARTIAL) {
            return "Eligible through partial shift matching fallback.";
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

    private List<ShiftWindow> resolveActiveShifts(LocalTime nowPt) {
        List<ShiftWindow> active = new ArrayList<>();
        for (ShiftWindow window : SHIFT_WINDOWS) {
            if (window.includes(nowPt)) {
                active.add(window);
            }
        }
        return active;
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
        PARTIAL,
        EXACT
    }
}
