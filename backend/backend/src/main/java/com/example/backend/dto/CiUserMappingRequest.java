package com.example.backend.dto;

public class CiUserMappingRequest {
    private Long configurationItemId;
    private Long teamMemberId;
    private Integer sortOrder;

    public Long getConfigurationItemId() {
        return configurationItemId;
    }

    public void setConfigurationItemId(Long configurationItemId) {
        this.configurationItemId = configurationItemId;
    }

    public Long getTeamMemberId() {
        return teamMemberId;
    }

    public void setTeamMemberId(Long teamMemberId) {
        this.teamMemberId = teamMemberId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
