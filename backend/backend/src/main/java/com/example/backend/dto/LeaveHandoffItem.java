package com.example.backend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LeaveHandoffItem {
    private String teamMemberName;
    private String email;
    private Instant leaveStart;
    private Instant leaveEnd;
    private String reason;
    private List<LeaveHandoffIncident> incidents = new ArrayList<>();

    public LeaveHandoffItem() {}

    public LeaveHandoffItem(
            String teamMemberName,
            String email,
            Instant leaveStart,
            Instant leaveEnd,
            String reason,
            List<LeaveHandoffIncident> incidents) {
        this.teamMemberName = teamMemberName;
        this.email = email;
        this.leaveStart = leaveStart;
        this.leaveEnd = leaveEnd;
        this.reason = reason;
        this.incidents = incidents;
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

    public Instant getLeaveStart() {
        return leaveStart;
    }

    public void setLeaveStart(Instant leaveStart) {
        this.leaveStart = leaveStart;
    }

    public Instant getLeaveEnd() {
        return leaveEnd;
    }

    public void setLeaveEnd(Instant leaveEnd) {
        this.leaveEnd = leaveEnd;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<LeaveHandoffIncident> getIncidents() {
        return incidents;
    }

    public void setIncidents(List<LeaveHandoffIncident> incidents) {
        this.incidents = incidents;
    }
}
