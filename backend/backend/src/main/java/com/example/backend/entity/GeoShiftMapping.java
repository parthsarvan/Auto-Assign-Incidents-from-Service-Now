package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(
        name = "geo_shift_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "g_id", "s_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "team"})
public class GeoShiftMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gsm_id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "g_id")
    private Geo geo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "s_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    public GeoShiftMapping() {}

    public GeoShiftMapping(Geo geo, Shift shift) {
        this.geo = geo;
        this.shift = shift;
    }

    public Long getGsm_id() {
        return gsm_id;
    }

    public void setGsm_id(Long gsm_id) {
        this.gsm_id = gsm_id;
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

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void syncTimesFromShift() {
        this.startTime = shift != null ? shift.getStartTime() : null;
        this.endTime = shift != null ? shift.getEndTime() : null;
    }
}
