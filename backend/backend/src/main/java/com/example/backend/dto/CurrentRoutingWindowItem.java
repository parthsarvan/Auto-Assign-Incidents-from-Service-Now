package com.example.backend.dto;

public class CurrentRoutingWindowItem {
    private String geo;
    private String shift;
    private String startTime;
    private String endTime;

    public CurrentRoutingWindowItem(String geo, String shift, String startTime, String endTime) {
        this.geo = geo;
        this.shift = shift;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
