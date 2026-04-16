package com.example.backend.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long u_id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "work_email", unique = true)
    private String workEmail;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

     @Column(nullable = false, updatable = false)
    private Instant created_at;

    private Long created_by;
    private Instant updated_at;
    private Long updated_by;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_org_id")
    private Organization currentOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_team_id")
    private Team currentTeam;

    // Constructors
    public User() { }

    // You can add a convenience constructor if needed:
    public User(String username, String password, String role, Instant created_at) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.created_at = created_at;
    }

    // Getters and Setters
    public Long getU_id() {
        return u_id;
    }

    public void setU_id(Long u_id) {
        this.u_id = u_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWorkEmail() {
        return workEmail;
    }

    public void setWorkEmail(String workEmail) {
        this.workEmail = workEmail;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Instant created_at) {
        this.created_at = created_at;
    }

    public Long getCreated_by() {
        return created_by;
    }

    public void setCreated_by(Long created_by) {
        this.created_by = created_by;
    }

    public Instant getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Instant updated_at) {
        this.updated_at = updated_at;
    }

    public Long getUpdated_by() {
        return updated_by;
    }

    public void setUpdated_by(Long updated_by) {
        this.updated_by = updated_by;
    }

    public Organization getCurrentOrganization() {
        return currentOrganization;
    }

    public void setCurrentOrganization(Organization currentOrganization) {
        this.currentOrganization = currentOrganization;
    }

    public Team getCurrentTeam() {
        return currentTeam;
    }

    public void setCurrentTeam(Team currentTeam) {
        this.currentTeam = currentTeam;
    }

}
