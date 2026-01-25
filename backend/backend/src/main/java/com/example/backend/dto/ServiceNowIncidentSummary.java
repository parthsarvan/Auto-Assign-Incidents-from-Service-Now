package com.example.backend.dto;

public class ServiceNowIncidentSummary {
    private String sysId;
    private String number;
    private String shortDescription;
    private String state;

    public ServiceNowIncidentSummary() {}

    public ServiceNowIncidentSummary(String sysId, String number, String shortDescription, String state) {
        this.sysId = sysId;
        this.number = number;
        this.shortDescription = shortDescription;
        this.state = state;
    }

    public String getSysId() {
        return sysId;
    }

    public void setSysId(String sysId) {
        this.sysId = sysId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
