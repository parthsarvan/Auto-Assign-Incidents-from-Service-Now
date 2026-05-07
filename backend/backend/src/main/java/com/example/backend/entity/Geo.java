package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
        name = "geo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "name"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "team"})
public class Geo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long g_id;

    @Column(nullable = false)
    private String name;

    @Column(name = "time_zone")
    private String timeZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Geo() {}

    public Geo(String name) {
        this.name = name;
    }

    public Long getG_id() {
        return g_id;
    }

    public void setG_id(Long g_id) {
        this.g_id = g_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
