package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long org_id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "email_domain", unique = true)
    private String emailDomain;

    @Column(name = "servicenow_instance_url")
    private String serviceNowInstanceUrl;

    @Column(name = "servicenow_username")
    private String serviceNowUsername;

    @Column(name = "servicenow_password")
    private String serviceNowPassword;

    @Column(name = "servicenow_connected_at")
    private Instant serviceNowConnectedAt;

    @Column(nullable = false, updatable = false)
    private Instant created_at;

    public Long getOrg_id() {
        return org_id;
    }

    public void setOrg_id(Long org_id) {
        this.org_id = org_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public void setEmailDomain(String emailDomain) {
        this.emailDomain = emailDomain;
    }

    public Instant getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Instant created_at) {
        this.created_at = created_at;
    }

    public String getServiceNowInstanceUrl() {
        return serviceNowInstanceUrl;
    }

    public void setServiceNowInstanceUrl(String serviceNowInstanceUrl) {
        this.serviceNowInstanceUrl = serviceNowInstanceUrl;
    }

    public String getServiceNowUsername() {
        return serviceNowUsername;
    }

    public void setServiceNowUsername(String serviceNowUsername) {
        this.serviceNowUsername = serviceNowUsername;
    }

    public String getServiceNowPassword() {
        return serviceNowPassword;
    }

    public void setServiceNowPassword(String serviceNowPassword) {
        this.serviceNowPassword = serviceNowPassword;
    }

    public Instant getServiceNowConnectedAt() {
        return serviceNowConnectedAt;
    }

    public void setServiceNowConnectedAt(Instant serviceNowConnectedAt) {
        this.serviceNowConnectedAt = serviceNowConnectedAt;
    }
}
