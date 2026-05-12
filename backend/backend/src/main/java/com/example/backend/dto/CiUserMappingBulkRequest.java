package com.example.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class CiUserMappingBulkRequest {
    private Long configurationItemId;
    private List<Long> teamMemberIds = new ArrayList<>();

    public Long getConfigurationItemId() {
        return configurationItemId;
    }

    public void setConfigurationItemId(Long configurationItemId) {
        this.configurationItemId = configurationItemId;
    }

    public List<Long> getTeamMemberIds() {
        return teamMemberIds;
    }

    public void setTeamMemberIds(List<Long> teamMemberIds) {
        this.teamMemberIds = teamMemberIds;
    }
}
