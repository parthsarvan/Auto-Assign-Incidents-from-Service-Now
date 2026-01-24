package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "team_member_schedule")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TeamMemberSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tms_id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tm_id")
    private TeamMember teamMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "g_id")
    private Geo geo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "s_id")
    private Shift shift;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public TeamMemberSchedule() {}

    public TeamMemberSchedule(TeamMember teamMember, Geo geo, Shift shift, LocalDate startDate, LocalDate endDate) {
        this.teamMember = teamMember;
        this.geo = geo;
        this.shift = shift;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getTms_id() {
        return tms_id;
    }

    public void setTms_id(Long tms_id) {
        this.tms_id = tms_id;
    }

    public TeamMember getTeamMember() {
        return teamMember;
    }

    public void setTeamMember(TeamMember teamMember) {
        this.teamMember = teamMember;
    }

    public Geo getGeo() {
        return geo;
    }

    public void setGeo(Geo geo) {
        this.geo = geo;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
