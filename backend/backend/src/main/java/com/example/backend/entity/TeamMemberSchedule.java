package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "team_member_schedule")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "team"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "coverage_days", nullable = false, length = 128)
    private String coverageDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";

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

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
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

    public String getCoverageDays() {
        return coverageDays;
    }

    public void setCoverageDays(String coverageDays) {
        this.coverageDays = coverageDays;
    }

    public boolean isActiveOn(LocalDate date) {
        if (date == null || date.isBefore(startDate) || date.isAfter(endDate)) {
            return false;
        }
        return getCoverageDaySet().contains(date.getDayOfWeek().name());
    }

    public Set<String> getCoverageDaySet() {
        if (coverageDays == null || coverageDays.isBlank()) {
            return Set.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        }
        return Arrays.stream(coverageDays.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }
}
