package com.example.backend.dto;

public class CreateTeamRequest {
    private String name;
    private String description;
    private Long copyFromTeamId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCopyFromTeamId() {
        return copyFromTeamId;
    }

    public void setCopyFromTeamId(Long copyFromTeamId) {
        this.copyFromTeamId = copyFromTeamId;
    }
}
