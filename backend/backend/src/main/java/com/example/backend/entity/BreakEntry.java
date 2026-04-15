package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "break_time")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "team"})
public class BreakEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long break_id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tm_id")
    private TeamMember teamMember;

    @Column(name = "start_ts", nullable = false)
    private Instant startTs;

    @Column(name = "end_ts", nullable = false)
    private Instant endTs;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public BreakEntry() {}

    public BreakEntry(TeamMember teamMember, Instant startTs, Instant endTs, String reason) {
        this.teamMember = teamMember;
        this.startTs = startTs;
        this.endTs = endTs;
        this.reason = reason;
    }

    public Long getBreak_id() {
        return break_id;
    }

    public void setBreak_id(Long break_id) {
        this.break_id = break_id;
    }

    public TeamMember getTeamMember() {
        return teamMember;
    }

    public void setTeamMember(TeamMember teamMember) {
        this.teamMember = teamMember;
    }

    public Instant getStartTs() {
        return startTs;
    }

    public void setStartTs(Instant startTs) {
        this.startTs = startTs;
    }

    public Instant getEndTs() {
        return endTs;
    }

    public void setEndTs(Instant endTs) {
        this.endTs = endTs;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
