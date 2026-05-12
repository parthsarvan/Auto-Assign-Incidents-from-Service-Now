package com.example.backend.dto;

public class UnsupportedCiHandlingSettingsResponse {
    private String policy;
    private Long fallbackTeamMemberId;
    private String fallbackTeamMemberName;

    public UnsupportedCiHandlingSettingsResponse() {}

    public UnsupportedCiHandlingSettingsResponse(
            String policy,
            Long fallbackTeamMemberId,
            String fallbackTeamMemberName) {
        this.policy = policy;
        this.fallbackTeamMemberId = fallbackTeamMemberId;
        this.fallbackTeamMemberName = fallbackTeamMemberName;
    }

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

    public String getFallbackTeamMemberName() {
        return fallbackTeamMemberName;
    }

    public void setFallbackTeamMemberName(String fallbackTeamMemberName) {
        this.fallbackTeamMemberName = fallbackTeamMemberName;
    }
}
