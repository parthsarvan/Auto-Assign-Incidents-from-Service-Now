package com.example.backend.dto;

public class ServiceNowConfigRequest {
    private String instanceUrl;
    private String username;
    private String password;
    private java.util.List<String> assignmentGroups;

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public java.util.List<String> getAssignmentGroups() {
        return assignmentGroups;
    }

    public void setAssignmentGroups(java.util.List<String> assignmentGroups) {
        this.assignmentGroups = assignmentGroups;
    }
}
