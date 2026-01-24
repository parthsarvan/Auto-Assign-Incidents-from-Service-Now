package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "configuration_item")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ConfigurationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ci_id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    public ConfigurationItem() {}

    public ConfigurationItem(String name, String description) {
        this.name = name;
        this.description = description;
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
}
