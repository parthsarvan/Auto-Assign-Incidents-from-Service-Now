package com.example.backend.service;

import com.example.backend.dto.AvailabilityRecord;
import com.example.backend.dto.CoverageIssue;
import com.example.backend.dto.CoverageSummaryResponse;
import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Team;
import com.example.backend.repository.CiUserMappingRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.TeamMemberScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CoverageSummaryService {
    private final AvailabilityService availabilityService;
    private final GeoShiftMappingRepository geoShiftMappingRepository;
    private final CiUserMappingRepository ciUserMappingRepository;
    private final TeamMemberScheduleRepository teamMemberScheduleRepository;
    private final CurrentWorkspaceService currentWorkspaceService;

    public CoverageSummaryService(
            AvailabilityService availabilityService,
            GeoShiftMappingRepository geoShiftMappingRepository,
            CiUserMappingRepository ciUserMappingRepository,
            TeamMemberScheduleRepository teamMemberScheduleRepository,
            CurrentWorkspaceService currentWorkspaceService) {
        this.availabilityService = availabilityService;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
        this.ciUserMappingRepository = ciUserMappingRepository;
        this.teamMemberScheduleRepository = teamMemberScheduleRepository;
        this.currentWorkspaceService = currentWorkspaceService;
    }

    public CoverageSummaryResponse buildSummary(LocalDate startDate, int days) {
        Team team = currentWorkspaceService.getCurrentTeam();
        int normalizedDays = Math.max(days, 1);
        LocalDate endDate = startDate.plusDays(normalizedDays - 1L);
        List<AvailabilityRecord> availability = availabilityService.getAvailability(startDate, endDate);
        Set<String> coveredKeys = new HashSet<>();
        for (AvailabilityRecord record : availability) {
            coveredKeys.add(key(record.getGeoName(), record.getShiftName(), record.getDate()));
        }

        List<CoverageIssue> issues = new ArrayList<>();
        List<GeoShiftMapping> geoShiftMappings = geoShiftMappingRepository.findAllByTeamWithGeoAndShift(team);
        int totalGeoShiftDays = geoShiftMappings.size() * normalizedDays;
        int coveredGeoShiftDays = 0;

        for (GeoShiftMapping mapping : geoShiftMappings) {
            for (int offset = 0; offset < normalizedDays; offset++) {
                LocalDate date = startDate.plusDays(offset);
                String geoName = mapping.getGeo().getName();
                String shiftName = mapping.getShift().getName();
                if (coveredKeys.contains(key(geoName, shiftName, date))) {
                    coveredGeoShiftDays++;
                    continue;
                }
                issues.add(new CoverageIssue(
                        "GEO_SHIFT_GAP",
                        "WARNING",
                        String.format("No coverage for %s / %s on %s.", geoName, shiftName, date),
                        date,
                        geoName,
                        shiftName,
                        null));
            }
        }

        int ciRiskCount = 0;
        List<CiUserMapping> mappings = ciUserMappingRepository.findAllByTeamWithDetails(team);
        Set<Long> ciIdsWithMappings = new HashSet<>();
        Set<Long> ciWithScheduledCoverage = new HashSet<>();
        for (CiUserMapping mapping : mappings) {
            Long ciId = mapping.getConfigurationItem().getCi_id();
            ciIdsWithMappings.add(ciId);
            boolean hasScheduledCoverage = teamMemberScheduleRepository
                    .existsByTeamMemberAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            mapping.getTeamMember(),
                            endDate,
                            startDate);
            if (hasScheduledCoverage) {
                ciWithScheduledCoverage.add(ciId);
            }
        }

        Set<Long> ciWithoutScheduledCoverage = new HashSet<>(ciIdsWithMappings);
        ciWithoutScheduledCoverage.removeAll(ciWithScheduledCoverage);
        for (CiUserMapping mapping : mappings) {
            Long ciId = mapping.getConfigurationItem().getCi_id();
            if (!ciWithoutScheduledCoverage.contains(ciId)) {
                continue;
            }
            ciWithoutScheduledCoverage.remove(ciId);
            ciRiskCount++;
            issues.add(new CoverageIssue(
                    "CI_SCHEDULE_RISK",
                    "RISK",
                    String.format("CI %s has mapped users, but none are scheduled between %s and %s.",
                            mapping.getConfigurationItem().getName(),
                            startDate,
                            endDate),
                    null,
                    null,
                    null,
                    mapping.getConfigurationItem().getName()));
        }

        issues.sort(Comparator
                .comparing(CoverageIssue::getSeverity)
                .thenComparing(issue -> issue.getDate() != null ? issue.getDate() : LocalDate.MAX)
                .thenComparing(issue -> issue.getConfigurationItem() != null ? issue.getConfigurationItem() : ""));

        return new CoverageSummaryResponse(
                Instant.now(),
                startDate,
                endDate,
                totalGeoShiftDays,
                coveredGeoShiftDays,
                (int) issues.stream().filter(issue -> "GEO_SHIFT_GAP".equals(issue.getType())).count(),
                ciRiskCount,
                issues);
    }

    private String key(String geoName, String shiftName, LocalDate date) {
        return geoName + "|" + shiftName + "|" + date;
    }
}
