package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
        name = "configuration_item",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"team_id", "name"}),
            @UniqueConstraint(columnNames = {"team_id", "service_now_sys_id"})
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "team"})
public class ConfigurationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ci_id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "service_now_sys_id")
    private String serviceNowSysId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public ConfigurationItem() {}

    public ConfigurationItem(String name, String description, String serviceNowSysId) {
        this.name = name;
        this.description = description;
        this.serviceNowSysId = serviceNowSysId;
    }

    public Long getCi_id() {
        return ci_id;
    }

    public void setCi_id(Long ci_id) {
        this.ci_id = ci_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServiceNowSysId() {
        return serviceNowSysId;
    }

    public void setServiceNowSysId(String serviceNowSysId) {
        this.serviceNowSysId = serviceNowSysId;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
