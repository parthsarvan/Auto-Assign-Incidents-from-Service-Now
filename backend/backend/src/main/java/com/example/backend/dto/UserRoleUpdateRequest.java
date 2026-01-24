package com.example.backend.dto;

public class UserRoleUpdateRequest {
    private String role;

    public UserRoleUpdateRequest() {}

    public UserRoleUpdateRequest(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
