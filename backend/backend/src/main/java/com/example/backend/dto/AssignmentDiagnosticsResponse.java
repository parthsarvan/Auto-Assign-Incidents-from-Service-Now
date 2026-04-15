package com.example.backend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDiagnosticsResponse {
    private Instant checkedAt;
    private int incidentCount;
    private int assignableCount;
    private int skippedCount;
    private List<AssignmentDiagnosticItem> incidents = new ArrayList<>();

    public AssignmentDiagnosticsResponse() {}

    public AssignmentDiagnosticsResponse(
            Instant checkedAt,
            int incidentCount,
            int assignableCount,
            int skippedCount,
            List<AssignmentDiagnosticItem> incidents) {
        this.checkedAt = checkedAt;
        this.incidentCount = incidentCount;
        this.assignableCount = assignableCount;
        this.skippedCount = skippedCount;
        this.incidents = incidents;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public int getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(int incidentCount) {
        this.incidentCount = incidentCount;
    }

    public int getAssignableCount() {
        return assignableCount;
    }

    public void setAssignableCount(int assignableCount) {
        this.assignableCount = assignableCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<AssignmentDiagnosticItem> getIncidents() {
        return incidents;
    }

    public void setIncidents(List<AssignmentDiagnosticItem> incidents) {
        this.incidents = incidents;
    }
}
