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

    public TeamMember() {}

    public TeamMember(String f_name, String l_name) {
        this.f_name = f_name;
        this.l_name = l_name;
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
}
