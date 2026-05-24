package com.example.backend.dto;

import java.time.LocalDate;

/**
 * A simple DTO that holds:
 *   - geoName    (e.g. “AMR”)
 *   - shiftName  (e.g. “General”)
 *   - date       (e.g. 2025-06-02)
 *   - fullName   (e.g. “Parth Sarvan”)
 */
public class AvailabilityRecord {
    private Long tmId;
    private String geoName;
    private String shiftName;
    private LocalDate date;
    private String fullName;

    public AvailabilityRecord(String geoName, String shiftName, LocalDate date, String fullName) {
        this(null, geoName, shiftName, date, fullName);
    }

    public AvailabilityRecord(Long tmId, String geoName, String shiftName, LocalDate date, String fullName) {
        this.tmId      = tmId;
        this.geoName   = geoName;
        this.shiftName = shiftName;
        this.date      = date;
        this.fullName  = fullName;
    }

    public Long getTmId() {
        return tmId;
    }

    public void setTmId(Long tmId) {
        this.tmId = tmId;
    }

    public String getGeoName() {
        return geoName;
    }

    public void setGeoName(String geoName) {
        this.geoName = geoName;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
