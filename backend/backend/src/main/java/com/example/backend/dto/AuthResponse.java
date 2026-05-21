package com.example.backend.dto;

public class AuthResponse {
    private String token;
    private Long u_id;
    private String username;
    private String firstName;
    private String lastName;
    private String workEmail;
    private String role;
    private WorkspaceSummary workspace;

    public AuthResponse(
            String token,
            Long u_id,
            String username,
            String firstName,
            String lastName,
            String workEmail,
            String role,
            WorkspaceSummary workspace) {
        this.token = token;
        this.u_id = u_id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.workEmail = workEmail;
        this.role = role;
        this.workspace = workspace;
    }

    // Getters only (no setters needed)
    public String getToken() {
        return token;
    }

    public Long getU_id() {
        return u_id;
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

    public WorkspaceSummary getWorkspace() {
        return workspace;
    }
}
