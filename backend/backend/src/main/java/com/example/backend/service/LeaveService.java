package com.example.backend.service;

import com.example.backend.dto.LeaveRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * LeaveService now handles the fact that a TIMESTAMP WITH TIME ZONE
 * column may be returned as java.time.Instant (or java.sql.Timestamp).
 */
@Service
public class LeaveService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Fetch all leave entries overlapping [startDate 00:00, endDate 23:59:59].
     * Uses the SQL that joins leave → team_member_schedule → geo → shift.
     */
    public List<LeaveRecord> getLeaves(LocalDate startDate, LocalDate endDate) {
        // Build the lower/upper bounds as LocalDateTime
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
            "  l.start_ts, " +
            "  l.end_ts, " +
            "  l.reason     AS reason " +
            "FROM \"leave\" l " +
            "JOIN team_member tm ON l.tm_id = tm.tm_id " +
            "JOIN team_member_schedule tms ON tm.tm_id = tms.tm_id " +
            "JOIN geo_shift_mapping gsm ON tms.g_id = gsm.g_id AND tms.s_id = gsm.s_id " +
            "JOIN geo g ON tms.g_id = g.g_id " +
            "JOIN shift s ON tms.s_id = s.s_id " +
            "WHERE " +
            "  l.end_ts   >= :tsLower " +
            "  AND " +
            "  l.start_ts <= :tsUpper " +
            "  AND " +
            "  tms.date = (l.start_ts AT TIME ZONE 'UTC')::date";

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("tsLower", tsLower)
            .setParameter("tsUpper", tsUpper);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) query.getResultList();
        List<LeaveRecord> result = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            // row[0] = f_name       (String)
            // row[1] = l_name       (String)
            // row[2] = geo_name     (String)
            // row[3] = shift_name   (String)
            // row[4] = start_ts     (Instant or Timestamp)
            // row[5] = end_ts       (Instant or Timestamp)
            // row[6] = reason       (String)

            String fName     = (String) row[0];
            String lName     = (String) row[1];
            String geoName   = (String) row[2];
            String shiftName = (String) row[3];
            Object  startObj = row[4];
            Object  endObj   = row[5];
            String  reason    = (String) row[6];

            // Convert startObj to LocalDateTime (in UTC). Then front end can zone‐convert.
            LocalDateTime startLdt = convertToLocalDateTime(startObj);
            LocalDateTime endLdt   = convertToLocalDateTime(endObj);

            String fullName = fName + " " + lName;
            result.add(new LeaveRecord(
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

    /**
     * Helper: convert either java.sql.Timestamp or java.time.Instant to LocalDateTime (UTC).
     * If other types appear (e.g. OffsetDateTime), you can extend this logic.
     */
    private LocalDateTime convertToLocalDateTime(Object tsObj) {
        if (tsObj instanceof Timestamp) {
            return ((Timestamp) tsObj).toLocalDateTime();
        } else if (tsObj instanceof Instant) {
            // The Instant is in UTC; convert to LocalDateTime in UTC
            return LocalDateTime.ofInstant((Instant) tsObj, ZoneOffset.UTC);
        } else {
            throw new IllegalArgumentException(
                "Unable to convert object to LocalDateTime: " + tsObj.getClass()
            );
        }
    }
}
