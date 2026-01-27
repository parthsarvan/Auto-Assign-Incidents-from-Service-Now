package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "team_member")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tm_id;

    @Column(nullable = false)
    private String f_name;

    @Column(nullable = false)
    private String l_name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String sys_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "g_id")
    private Geo geo;

    public TeamMember() {}

    public TeamMember(String f_name, String l_name, String email, Geo geo) {
        this.f_name = f_name;
        this.l_name = l_name;
        this.email = email;
        this.geo = geo;
    }

    public Long getTm_id() {
        return tm_id;
    }

    public void setTm_id(Long tm_id) {
        this.tm_id = tm_id;
    }

    public String getF_name() {
        return f_name;
    }

    public void setF_name(String f_name) {
        this.f_name = f_name;
    }

    public String getL_name() {
        return l_name;
    }

    public void setL_name(String l_name) {
        this.l_name = l_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSys_id() {
        return sys_id;
    }

    public void setSys_id(String sys_id) {
        this.sys_id = sys_id;
    }

    public Geo getGeo() {
        return geo;
    }

    public void setGeo(Geo geo) {
        this.geo = geo;
    }

}
