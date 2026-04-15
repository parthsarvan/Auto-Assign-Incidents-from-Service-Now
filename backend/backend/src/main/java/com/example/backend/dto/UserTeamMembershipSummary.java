package com.example.backend.dto;

public class UserTeamMembershipSummary {
    private Long teamId;
    private String teamName;
    private String role;
    private boolean current;

    public UserTeamMembershipSummary(Long teamId, String teamName, String role, boolean current) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.role = role;
        this.current = current;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getRole() {
        return role;
    }

    public boolean isCurrent() {
        return current;
    }
}
