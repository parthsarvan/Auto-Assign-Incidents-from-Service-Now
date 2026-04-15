package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
        name = "ci_user_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "ci_id", "tm_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "team"})
public class CiUserMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mapping_id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ci_id")
    private ConfigurationItem configurationItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tm_id")
    private TeamMember teamMember;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public CiUserMapping() {}

    public CiUserMapping(ConfigurationItem configurationItem, TeamMember teamMember, Integer sortOrder) {
        this.configurationItem = configurationItem;
        this.teamMember = teamMember;
        this.sortOrder = sortOrder;
    }

    public Long getMapping_id() {
        return mapping_id;
    }

    public void setMapping_id(Long mapping_id) {
        this.mapping_id = mapping_id;
    }

    public ConfigurationItem getConfigurationItem() {
        return configurationItem;
    }

    public void setConfigurationItem(ConfigurationItem configurationItem) {
        this.configurationItem = configurationItem;
    }

    public TeamMember getTeamMember() {
        return teamMember;
    }

    public void setTeamMember(TeamMember teamMember) {
        this.teamMember = teamMember;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
