package com.example.backend.dto;

public class ServiceNowLookupResult {
    private String sysId;
    private String displayName;
    private String email;
    private String userName;
    private String detail;
    private String secondaryDetail;

    public ServiceNowLookupResult() {
    }

    public ServiceNowLookupResult(
            String sysId,
            String displayName,
            String email,
            String userName,
            String detail,
            String secondaryDetail) {
        this.sysId = sysId;
        this.displayName = displayName;
        this.email = email;
        this.userName = userName;
        this.detail = detail;
        this.secondaryDetail = secondaryDetail;
    }

    public String getSysId() {
        return sysId;
    }

    public void setSysId(String sysId) {
        this.sysId = sysId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getSecondaryDetail() {
        return secondaryDetail;
    }

    public void setSecondaryDetail(String secondaryDetail) {
        this.secondaryDetail = secondaryDetail;
    }
}
