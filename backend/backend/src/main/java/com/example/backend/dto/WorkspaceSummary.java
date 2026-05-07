package com.example.backend.dto;

public class WorkspaceSummary {
    private Long organizationId;
    private String organizationName;
    private Long teamId;
    private String teamName;
    private String teamRole;
    private String teamTimezone;

    public WorkspaceSummary() {}

    public WorkspaceSummary(
            Long organizationId,
            String organizationName,
            Long teamId,
            String teamName,
            String teamRole,
            String teamTimezone) {
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamRole = teamRole;
        this.teamTimezone = teamTimezone;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public String getTeamTimezone() {
        return teamTimezone;
    }
}
