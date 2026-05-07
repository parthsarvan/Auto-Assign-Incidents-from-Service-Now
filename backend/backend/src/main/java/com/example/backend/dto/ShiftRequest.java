package com.example.backend.dto;

import java.time.LocalTime;
import java.util.List;

public class ShiftRequest {
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<Long> geoIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public List<Long> getGeoIds() {
        return geoIds;
    }

    public void setGeoIds(List<Long> geoIds) {
        this.geoIds = geoIds;
    }
}
