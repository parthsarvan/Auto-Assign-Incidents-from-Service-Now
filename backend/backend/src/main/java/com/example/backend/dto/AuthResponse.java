package com.example.backend.dto;

public class AuthResponse {
    private String token;
    private Long u_id;
    private String username;
    private String role;

    public AuthResponse(String token, Long u_id, String username, String role) {
        this.token = token;
        this.u_id = u_id;
        this.username = username;
        this.role = role;
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

    public String getRole() {
        return role;
    }
}
