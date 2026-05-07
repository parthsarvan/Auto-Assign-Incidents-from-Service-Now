package com.example.backend.service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdentitySequenceRepairService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentitySequenceRepairService.class);
    private static final List<TableIdentity> TABLE_IDENTITIES = List.of(
            new TableIdentity("organization", "org_id"),
            new TableIdentity("team", "team_id"),
            new TableIdentity("users", "u_id"),
            new TableIdentity("organization_membership", "membership_id"),
            new TableIdentity("geo", "g_id"),
            new TableIdentity("shift", "s_id"),
            new TableIdentity("configuration_item", "ci_id"),
            new TableIdentity("team_member", "tm_id"),
            new TableIdentity("geo_shift_mapping", "gsm_id"),
            new TableIdentity("ci_user_mapping", "mapping_id"),
            new TableIdentity("team_member_schedule", "tms_id"),
            new TableIdentity("\"leave\"", "leave_id"),
            new TableIdentity("break_time", "break_id"),
            new TableIdentity("team_membership", "membership_id"),
            new TableIdentity("servicenow_run_log", "log_id"));

    private final JdbcTemplate jdbcTemplate;

    public IdentitySequenceRepairService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void repairIdentitySequences() {
        for (TableIdentity tableIdentity : TABLE_IDENTITIES) {
            try {
                syncSequence(tableIdentity.tableName(), tableIdentity.idColumn());
            } catch (Exception ex) {
                LOGGER.warn(
                        "Skipping identity sequence repair for {}.{}: {}",
                        tableIdentity.tableName(),
                        tableIdentity.idColumn(),
                        ex.getMessage());
            }
        }
    }

    private void syncSequence(String tableName, String idColumn) {
        String sequenceLookupTableName = unquoteIdentifier(tableName);
        String sequenceName = jdbcTemplate.queryForObject(
                "select pg_get_serial_sequence(?, ?)",
                String.class,
                sequenceLookupTableName,
                idColumn);

        if (sequenceName == null || sequenceName.isBlank()) {
            return;
        }

        Long nextValue = jdbcTemplate.queryForObject(
                String.format("select coalesce(max(%s), 0) + 1 from %s", idColumn, tableName),
                Long.class);

        Long repairedValue = jdbcTemplate.queryForObject(
                "select setval(?, ?, false)",
                Long.class,
                sequenceName,
                nextValue != null ? nextValue : 1L);

        LOGGER.info(
                "Repaired identity sequence {} for {}.{} to {}",
                sequenceName,
                sequenceLookupTableName,
                idColumn,
                repairedValue);
    }

    private String unquoteIdentifier(String identifier) {
        return identifier.replace("\"", "");
    }

    private record TableIdentity(String tableName, String idColumn) {}
}
