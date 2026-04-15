package com.example.backend.dto;

public class TeamSummary {
    private Long teamId;
    private String teamName;
    private String description;
    private String joinCode;
    private boolean current;

    public TeamSummary() {}

    public TeamSummary(Long teamId, String teamName, String description, String joinCode, boolean current) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.description = description;
        this.joinCode = joinCode;
        this.current = current;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getDescription() {
        return description;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public boolean isCurrent() {
        return current;
    }
}
