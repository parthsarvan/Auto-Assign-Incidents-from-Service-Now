package com.example.backend.dto;

public class AccountDeletionResponse {
    private Long deletedUserId;
    private String deletedUsername;
    private boolean userDeleted;
    private int teamMemberRecordsDeleted;
    private int teamMembershipsDeleted;
    private int organizationMembershipsDeleted;
    private int mobileDeviceTokensDeleted;
    private String message;

    public AccountDeletionResponse() {
    }

    public AccountDeletionResponse(
            Long deletedUserId,
            String deletedUsername,
            boolean userDeleted,
            int teamMemberRecordsDeleted,
            int teamMembershipsDeleted,
            int organizationMembershipsDeleted,
            int mobileDeviceTokensDeleted,
            String message) {
        this.deletedUserId = deletedUserId;
        this.deletedUsername = deletedUsername;
        this.userDeleted = userDeleted;
        this.teamMemberRecordsDeleted = teamMemberRecordsDeleted;
        this.teamMembershipsDeleted = teamMembershipsDeleted;
        this.organizationMembershipsDeleted = organizationMembershipsDeleted;
        this.mobileDeviceTokensDeleted = mobileDeviceTokensDeleted;
        this.message = message;
    }

    public Long getDeletedUserId() {
        return deletedUserId;
    }

    public String getDeletedUsername() {
        return deletedUsername;
    }

    public boolean isUserDeleted() {
        return userDeleted;
    }

    public int getTeamMemberRecordsDeleted() {
        return teamMemberRecordsDeleted;
    }

    public int getTeamMembershipsDeleted() {
        return teamMembershipsDeleted;
    }

    public int getOrganizationMembershipsDeleted() {
        return organizationMembershipsDeleted;
    }

    public int getMobileDeviceTokensDeleted() {
        return mobileDeviceTokensDeleted;
    }

    public String getMessage() {
        return message;
    }
}
