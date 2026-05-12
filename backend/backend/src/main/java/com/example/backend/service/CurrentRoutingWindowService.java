package com.example.backend.service;

import com.example.backend.dto.CurrentRoutingWindowItem;
import com.example.backend.dto.CurrentRoutingWindowResponse;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Team;
import com.example.backend.repository.GeoShiftMappingRepository;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CurrentRoutingWindowService {
    private static final DateTimeFormatter TEAM_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CurrentWorkspaceService currentWorkspaceService;
    private final GeoShiftMappingRepository geoShiftMappingRepository;

    public CurrentRoutingWindowService(
            CurrentWorkspaceService currentWorkspaceService,
            GeoShiftMappingRepository geoShiftMappingRepository) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.geoShiftMappingRepository = geoShiftMappingRepository;
    }

    public CurrentRoutingWindowResponse getCurrentWindow() {
        Team team = currentWorkspaceService.getCurrentTeam();
        CurrentRoutingWindowResponse response = new CurrentRoutingWindowResponse();

        String timezone = team.getTimezone();
        response.setTimezone(timezone);
        if (timezone == null || timezone.isBlank()) {
            response.setStatus("MISSING_TIMEZONE");
            response.setMessage("Set the team timezone before InciTeam can show the active geo and shift.");
            return response;
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            response.setStatus("INVALID_TIMEZONE");
            response.setMessage("The configured team timezone is not valid.");
            return response;
        }

        ZonedDateTime teamNow = ZonedDateTime.now(zoneId);
        LocalTime teamLocalTime = teamNow.toLocalTime();
        response.setTeamLocalDateTime(teamNow.format(TEAM_TIME_FORMAT));

        List<CurrentRoutingWindowItem> activeWindows = new ArrayList<>();
        for (GeoShiftMapping mapping : geoShiftMappingRepository.findAllByTeamWithGeoAndShift(team)) {
            if (mapping.getGeo() == null
                    || mapping.getShift() == null
                    || mapping.getShift().getStartTime() == null
                    || mapping.getShift().getEndTime() == null) {
                continue;
            }

            LocalTime start = mapping.getShift().getStartTime();
            LocalTime end = mapping.getShift().getEndTime();
            if (includes(start, end, teamLocalTime)) {
                activeWindows.add(new CurrentRoutingWindowItem(
                        mapping.getGeo().getName(),
                        mapping.getShift().getName(),
                        start.toString(),
                        end.toString()));
            }
        }

        response.setActiveWindows(activeWindows);
        response.setHasActiveWindow(!activeWindows.isEmpty());
        if (activeWindows.isEmpty()) {
            response.setStatus("NO_ACTIVE_SHIFT");
            response.setMessage("No geo and shift window matches the current team time.");
        } else {
            response.setStatus("OK");
            response.setMessage("Active geo and shift window resolved from the same team timezone used by polling.");
        }
        return response;
    }

    private boolean includes(LocalTime start, LocalTime end, LocalTime now) {
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }
}
