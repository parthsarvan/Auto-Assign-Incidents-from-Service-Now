package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "servicenow_run_log")
public class ServiceNowRunLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long log_id;

    @Column(nullable = false)
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private int incidentCount;

    @Column(name = "incidents_json", columnDefinition = "text")
    private String incidentsJson;

    @Column(name = "assignment_selections_json", columnDefinition = "text")
    private String assignmentSelectionsJson;

    @Column(name = "assignment_results_json", columnDefinition = "text")
    private String assignmentResultsJson;

    @Column(length = 1000)
    private String assignmentConfirmation;

    public Long getLog_id() {
        return log_id;
    }

    public void setLog_id(Long log_id) {
        this.log_id = log_id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(int incidentCount) {
        this.incidentCount = incidentCount;
    }

    public String getIncidentsJson() {
        return incidentsJson;
    }

    public void setIncidentsJson(String incidentsJson) {
        this.incidentsJson = incidentsJson;
    }

    public String getAssignmentSelectionsJson() {
        return assignmentSelectionsJson;
    }

    public void setAssignmentSelectionsJson(String assignmentSelectionsJson) {
        this.assignmentSelectionsJson = assignmentSelectionsJson;
    }

    public String getAssignmentResultsJson() {
        return assignmentResultsJson;
    }

    public void setAssignmentResultsJson(String assignmentResultsJson) {
        this.assignmentResultsJson = assignmentResultsJson;
    }

    public String getAssignmentConfirmation() {
        return assignmentConfirmation;
    }

    public void setAssignmentConfirmation(String assignmentConfirmation) {
        this.assignmentConfirmation = assignmentConfirmation;
    }
}
