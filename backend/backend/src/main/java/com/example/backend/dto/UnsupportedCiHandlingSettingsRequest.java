package com.example.backend.dto;

public class UnsupportedCiHandlingSettingsRequest {
    private String policy;
    private Long fallbackTeamMemberId;

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public Long getFallbackTeamMemberId() {
        return fallbackTeamMemberId;
    }

    public void setFallbackTeamMemberId(Long fallbackTeamMemberId) {
        this.fallbackTeamMemberId = fallbackTeamMemberId;
    }
}
