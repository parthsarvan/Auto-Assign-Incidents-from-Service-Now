package com.example.backend.dto;

public class IncidentAssignmentDecision {
    private IncidentAssignmentSuggestion suggestion;
    private String reason;

    public IncidentAssignmentDecision() {}

    public IncidentAssignmentDecision(IncidentAssignmentSuggestion suggestion, String reason) {
        this.suggestion = suggestion;
        this.reason = reason;
    }

    public static IncidentAssignmentDecision assigned(IncidentAssignmentSuggestion suggestion) {
        return new IncidentAssignmentDecision(suggestion, null);
    }

    public static IncidentAssignmentDecision skipped(String reason) {
        return new IncidentAssignmentDecision(null, reason);
    }

    public IncidentAssignmentSuggestion getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(IncidentAssignmentSuggestion suggestion) {
        this.suggestion = suggestion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean hasSuggestion() {
        return suggestion != null;
    }
}
