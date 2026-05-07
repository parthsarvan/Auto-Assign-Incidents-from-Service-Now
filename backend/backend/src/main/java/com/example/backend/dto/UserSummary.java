package com.example.backend.dto;

import java.util.List;

public class UserSummary {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String workEmail;
    private String role;
    private Long currentTeamId;
    private String currentTeamName;
    private List<UserTeamMembershipSummary> teamMemberships;

    public UserSummary(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.firstName = null;
        this.lastName = null;
        this.workEmail = null;
        this.role = role;
        this.teamMemberships = List.of();
    }

    public UserSummary(
            Long id,
            String username,
            String firstName,
            String lastName,
            String workEmail,
            String role,
            Long currentTeamId,
            String currentTeamName,
            List<UserTeamMembershipSummary> teamMemberships) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.workEmail = workEmail;
        this.role = role;
        this.currentTeamId = currentTeamId;
        this.currentTeamName = currentTeamName;
        this.teamMemberships = teamMemberships;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getWorkEmail() {
        return workEmail;
    }

    public String getRole() {
        return role;
    }

    public Long getCurrentTeamId() {
        return currentTeamId;
    }

    public String getCurrentTeamName() {
        return currentTeamName;
    }

    public List<UserTeamMembershipSummary> getTeamMemberships() {
        return teamMemberships;
    }
}
