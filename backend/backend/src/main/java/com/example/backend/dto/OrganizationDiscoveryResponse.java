package com.example.backend.dto;

public class OrganizationDiscoveryResponse {
    private boolean organizationExists;
    private String organizationName;
    private String emailDomain;
    private boolean inviteCodeRequired;

    public OrganizationDiscoveryResponse(boolean organizationExists, String organizationName, String emailDomain, boolean inviteCodeRequired) {
        this.organizationExists = organizationExists;
        this.organizationName = organizationName;
        this.emailDomain = emailDomain;
        this.inviteCodeRequired = inviteCodeRequired;
    }

    public boolean isOrganizationExists() {
        return organizationExists;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public boolean isInviteCodeRequired() {
        return inviteCodeRequired;
    }
}
