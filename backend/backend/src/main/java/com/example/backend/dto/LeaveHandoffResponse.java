package com.example.backend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LeaveHandoffResponse {
    private Instant checkedAt;
    private int impactedMemberCount;
    private int activeIncidentCount;
    private List<LeaveHandoffItem> items = new ArrayList<>();

    public LeaveHandoffResponse() {}

    public LeaveHandoffResponse(
            Instant checkedAt,
            int impactedMemberCount,
            int activeIncidentCount,
            List<LeaveHandoffItem> items) {
        this.checkedAt = checkedAt;
        this.impactedMemberCount = impactedMemberCount;
        this.activeIncidentCount = activeIncidentCount;
        this.items = items;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public int getImpactedMemberCount() {
        return impactedMemberCount;
    }

    public void setImpactedMemberCount(int impactedMemberCount) {
        this.impactedMemberCount = impactedMemberCount;
    }

    public int getActiveIncidentCount() {
        return activeIncidentCount;
    }

    public void setActiveIncidentCount(int activeIncidentCount) {
        this.activeIncidentCount = activeIncidentCount;
    }

    public List<LeaveHandoffItem> getItems() {
        return items;
    }

    public void setItems(List<LeaveHandoffItem> items) {
        this.items = items;
    }
}
