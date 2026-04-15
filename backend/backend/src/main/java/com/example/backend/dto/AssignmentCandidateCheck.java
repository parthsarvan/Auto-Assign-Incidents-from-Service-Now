package com.example.backend.dto;

public class AssignmentCandidateCheck {
    private String teamMemberName;
    private String email;
    private String serviceNowUserSysId;
    private Integer sortOrder;
    private String memberGeo;
    private String activeSchedules;
    private String matchStatus;
    private boolean onLeave;
    private boolean onBreak;
    private boolean eligible;
    private boolean selected;
    private String reason;

    public AssignmentCandidateCheck() {}

    public AssignmentCandidateCheck(
            String teamMemberName,
            String email,
            String serviceNowUserSysId,
            Integer sortOrder,
            String memberGeo,
            String activeSchedules,
            String matchStatus,
            boolean onLeave,
            boolean onBreak,
            boolean eligible,
            boolean selected,
            String reason) {
        this.teamMemberName = teamMemberName;
        this.email = email;
        this.serviceNowUserSysId = serviceNowUserSysId;
        this.sortOrder = sortOrder;
        this.memberGeo = memberGeo;
        this.activeSchedules = activeSchedules;
        this.matchStatus = matchStatus;
        this.onLeave = onLeave;
        this.onBreak = onBreak;
        this.eligible = eligible;
        this.selected = selected;
        this.reason = reason;
    }

    public String getTeamMemberName() {
        return teamMemberName;
    }

    public void setTeamMemberName(String teamMemberName) {
        this.teamMemberName = teamMemberName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getServiceNowUserSysId() {
        return serviceNowUserSysId;
    }

    public void setServiceNowUserSysId(String serviceNowUserSysId) {
        this.serviceNowUserSysId = serviceNowUserSysId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getMemberGeo() {
        return memberGeo;
    }

    public void setMemberGeo(String memberGeo) {
        this.memberGeo = memberGeo;
    }

    public String getActiveSchedules() {
        return activeSchedules;
    }

    public void setActiveSchedules(String activeSchedules) {
        this.activeSchedules = activeSchedules;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public boolean isOnLeave() {
        return onLeave;
    }

    public void setOnLeave(boolean onLeave) {
        this.onLeave = onLeave;
    }

    public boolean isOnBreak() {
        return onBreak;
    }

    public void setOnBreak(boolean onBreak) {
        this.onBreak = onBreak;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
