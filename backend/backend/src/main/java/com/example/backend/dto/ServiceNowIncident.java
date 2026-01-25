package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNowIncident {
    private String sys_id;
    private String number;
    private String short_description;
    private String state;
    private ServiceNowReference assigned_to;
    private ServiceNowReference cmdb_ci;

    public String getSys_id() {
        return sys_id;
    }

    public void setSys_id(String sys_id) {
        this.sys_id = sys_id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getShort_description() {
        return short_description;
    }

    public void setShort_description(String short_description) {
        this.short_description = short_description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ServiceNowReference getAssigned_to() {
        return assigned_to;
    }

    public void setAssigned_to(ServiceNowReference assigned_to) {
        this.assigned_to = assigned_to;
    }

    public ServiceNowReference getCmdb_ci() {
        return cmdb_ci;
    }

    public void setCmdb_ci(ServiceNowReference cmdb_ci) {
        this.cmdb_ci = cmdb_ci;
    }
}
