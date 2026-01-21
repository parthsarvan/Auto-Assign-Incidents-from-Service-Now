// src/main/java/com/example/backend/service/AvailabilityService.java
package com.example.backend.service;

import com.example.backend.dto.AvailabilityRecord;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AvailabilityService now uses EntityManager.createNativeQuery(…)
 * to run the same SQL, and then maps each Object[] row into an AvailabilityRecord.
 */
@Service
public class AvailabilityService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Fetch all team-member schedules between start and end.
     * Translates each row into an AvailabilityRecord DTO.
     */
    public List<AvailabilityRecord> getAvailability(LocalDate start, LocalDate end) {
        // Convert LocalDate to java.sql.Date
        Date sqlStart = Date.valueOf(start);
        Date sqlEnd   = Date.valueOf(end);

        // The native SQL matching exactly the one you wrote:
        String sql =
            "SELECT " +
            "  g.name AS geo_name, " +
            "  s.name AS shift_name, " +
            "  tms.date AS schedule_date, " +
            "  CONCAT(tm.f_name, ' ', tm.l_name) AS full_name " +
            "FROM team_member_schedule tms " +
            "JOIN team_member tm        ON tm.tm_id = tms.tm_id " +
            "JOIN geo g                 ON tms.g_id = g.g_id " +
            "JOIN shift s               ON tms.s_id = s.s_id " +
            "JOIN geo_shift_mapping gsm ON tms.g_id = gsm.g_id AND tms.s_id = gsm.s_id " +
            "WHERE tms.date BETWEEN :startDate AND :endDate";

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("startDate", sqlStart)
            .setParameter("endDate",   sqlEnd);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) query.getResultList();

        List<AvailabilityRecord> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            // row[0] = geo_name (String)
            // row[1] = shift_name (String)
            // row[2] = schedule_date (java.sql.Date)
            // row[3] = full_name (String)
            String geoName   = (String) row[0];
            String shiftName = (String) row[1];
            Date   dateSql   = (Date) row[2];
            String fullName  = (String) row[3];

            LocalDate dateLocal = dateSql.toLocalDate();
            result.add(new AvailabilityRecord(geoName, shiftName, dateLocal, fullName));
        }

        return result;
    }
}
