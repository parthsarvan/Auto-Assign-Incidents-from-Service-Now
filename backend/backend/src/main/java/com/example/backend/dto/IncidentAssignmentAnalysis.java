package com.example.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class IncidentAssignmentAnalysis {
    private IncidentAssignmentDecision decision;
    private List<AssignmentCandidateCheck> candidates = new ArrayList<>();

    public IncidentAssignmentAnalysis() {}

    public IncidentAssignmentAnalysis(
            IncidentAssignmentDecision decision,
            List<AssignmentCandidateCheck> candidates) {
        this.decision = decision;
        this.candidates = candidates;
    }

    public IncidentAssignmentDecision getDecision() {
        return decision;
    }

    public void setDecision(IncidentAssignmentDecision decision) {
        this.decision = decision;
    }

    public List<AssignmentCandidateCheck> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<AssignmentCandidateCheck> candidates) {
        this.candidates = candidates;
    }
}
