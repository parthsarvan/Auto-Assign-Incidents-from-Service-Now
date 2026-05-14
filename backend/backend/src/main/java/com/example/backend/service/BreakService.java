package com.example.backend.service;

import com.example.backend.dto.BreakRecord;
import com.example.backend.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class BreakService {

    private final CurrentWorkspaceService currentWorkspaceService;

    public BreakService(CurrentWorkspaceService currentWorkspaceService) {
        this.currentWorkspaceService = currentWorkspaceService;
    }

    @PersistenceContext
    private EntityManager entityManager;

    public List<BreakRecord> getBreaks(LocalDate startDate, LocalDate endDate) {
        Team team = currentWorkspaceService.getCurrentTeam();
        LocalDateTime lowerLdt = startDate.atStartOfDay();
        LocalDateTime upperLdt = endDate.atTime(LocalTime.of(23, 59, 59, 999_000_000));

        Timestamp tsLower = Timestamp.valueOf(lowerLdt);
        Timestamp tsUpper = Timestamp.valueOf(upperLdt);

        String sql =
            "SELECT " +
            "  tm.f_name, " +
            "  tm.l_name, " +
            "  g.name       AS geo_name, " +
            "  s.name       AS shift_name, " +
            "  b.start_ts, " +
            "  b.end_ts, " +
            "  b.reason     AS reason " +
            "FROM break_time b " +
            "JOIN team_member tm ON b.tm_id = tm.tm_id " +
            "JOIN team_member_schedule tms ON tm.tm_id = tms.tm_id " +
            "JOIN geo_shift_mapping gsm ON tms.g_id = gsm.g_id AND tms.s_id = gsm.s_id " +
            "JOIN geo g ON tms.g_id = g.g_id " +
            "JOIN shift s ON tms.s_id = s.s_id " +
            "WHERE " +
            "  b.end_ts   >= :tsLower " +
            "  AND " +
            "  b.start_ts <= :tsUpper " +
            "  AND " +
            "  (b.start_ts AT TIME ZONE 'UTC')::date BETWEEN tms.start_date AND tms.end_date " +
            "  AND (tms.coverage_days IS NULL " +
            "       OR trim(tms.coverage_days) = '' " +
            "       OR position(upper(to_char((b.start_ts AT TIME ZONE 'UTC')::date, 'FMDay')) in tms.coverage_days) > 0) " +
            "  AND b.team_id = :teamId " +
            "  AND tm.team_id = :teamId " +
            "  AND tms.team_id = :teamId " +
            "  AND gsm.team_id = :teamId";

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("tsLower", tsLower)
            .setParameter("tsUpper", tsUpper)
            .setParameter("teamId", team.getTeam_id());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) query.getResultList();
        List<BreakRecord> result = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            String fName = (String) row[0];
            String lName = (String) row[1];
            String geoName = (String) row[2];
            String shiftName = (String) row[3];
            Object startObj = row[4];
            Object endObj = row[5];
            String reason = (String) row[6];

            LocalDateTime startLdt = convertToLocalDateTime(startObj);
            LocalDateTime endLdt = convertToLocalDateTime(endObj);

            String fullName = fName + " " + lName;
            result.add(new BreakRecord(
                fullName,
                geoName,
                shiftName,
                startLdt,
                endLdt,
                reason
            ));
        }

        return result;
    }

    private LocalDateTime convertToLocalDateTime(Object tsObj) {
        if (tsObj instanceof Timestamp) {
            return ((Timestamp) tsObj).toLocalDateTime();
        } else if (tsObj instanceof Instant) {
            return LocalDateTime.ofInstant((Instant) tsObj, ZoneOffset.UTC);
        } else {
            throw new IllegalArgumentException(
                "Unable to convert object to LocalDateTime: " + tsObj.getClass()
            );
        }
    }
}
