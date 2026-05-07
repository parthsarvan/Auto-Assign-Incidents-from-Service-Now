package com.example.backend.dto;

public class TeamJoinedUserSummary {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String workEmail;

    public TeamJoinedUserSummary(Long id, String username, String firstName, String lastName, String workEmail) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.workEmail = workEmail;
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
}
