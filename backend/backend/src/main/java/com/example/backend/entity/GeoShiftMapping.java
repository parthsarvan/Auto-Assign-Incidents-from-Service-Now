package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "geo_shift_mapping")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
}
