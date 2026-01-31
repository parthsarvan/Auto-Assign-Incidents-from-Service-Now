package com.example.backend.service;

import com.example.backend.dto.IncidentAssignmentSuggestion;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
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
    private final ConcurrentHashMap<Long, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public IncidentAssignmentService(
            ConfigurationItemRepository configurationItemRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            LeaveEntryRepository leaveEntryRepository,
            BreakEntryRepository breakEntryRepository) {
        this.configurationItemRepository = configurationItemRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.breakEntryRepository = breakEntryRepository;
    }

    public Optional<IncidentAssignmentSuggestion> suggestAssignee(ServiceNowIncident incident) {
        String ciName = resolveDisplayValue(incident.getCmdb_ci());
        if (ciName == null || ciName.isBlank()) {
            return Optional.empty();
        }

        Optional<ConfigurationItem> configurationItem = configurationItemRepository.findByName(ciName);
        if (configurationItem.isEmpty()) {
            return Optional.empty();
        }

        List<ShiftWindow> activeShifts = resolveActiveShifts(ZonedDateTime.now(PT_ZONE).toLocalTime());
        if (activeShifts.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        LocalDate today = ZonedDateTime.now(PT_ZONE).toLocalDate();
        List<CiUserMapping> mappings =
                ciUserMappingRepository.findByConfigurationItemOrderBySortOrderAsc(configurationItem.get());
        if (mappings.isEmpty()) {
            return Optional.empty();
        }

        List<Candidate> eligible = new ArrayList<>();
        List<Candidate> fallbackEligible = new ArrayList<>();
        for (CiUserMapping mapping : sortedMappings(mappings)) {
            TeamMember member = mapping.getTeamMember();
            MatchResult match = resolveShiftMatch(member, activeShifts, today);
            if (match.matchType() == ShiftMatch.NONE) {
                continue;
            }
            if (leaveEntryRepository.existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
                    member, now, now)) {
                continue;
            }
            if (breakEntryRepository.existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
                    member, now, now)) {
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
            return Optional.empty();
        }

        int index = selectRoundRobinIndex(configurationItem.get().getCi_id(), eligible.size());
        Candidate chosen = eligible.get(index);
        TeamMember chosenMember = chosen.mapping().getTeamMember();
        String fullName = String.format("%s %s", chosenMember.getF_name(), chosenMember.getL_name());
        return Optional.of(new IncidentAssignmentSuggestion(
                fullName,
                chosenMember.getEmail(),
                chosenMember.getSys_id(),
                chosenMember.getPhone(),
                chosen.window().geoName(),
                chosen.window().shiftName()));
    }

    private List<CiUserMapping> sortedMappings(List<CiUserMapping> mappings) {
        return mappings.stream()
                .sorted(Comparator.comparing(CiUserMapping::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private MatchResult resolveShiftMatch(TeamMember member, List<ShiftWindow> activeShifts, LocalDate date) {
        List<TeamMemberSchedule> schedules = teamMemberScheduleRepository.findActiveSchedules(member, date);
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

    private List<ShiftWindow> resolveActiveShifts(LocalTime nowPt) {
        List<ShiftWindow> active = new ArrayList<>();
        for (ShiftWindow window : SHIFT_WINDOWS) {
            if (window.includes(nowPt)) {
                active.add(window);
            }
        }
        return active;
    }

    private int selectRoundRobinIndex(Long ciId, int size) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(ciId, key -> new AtomicInteger(0));
        return Math.floorMod(counter.getAndIncrement(), size);
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
