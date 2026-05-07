package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNowIncident {
    private String sys_id;
    private String number;
    private String short_description;
    private String sys_created_on;
    private String state;
    private String priority;
    @JsonDeserialize(using = ServiceNowReferenceDeserializer.class)
    private ServiceNowReference assigned_to;
    @JsonDeserialize(using = ServiceNowReferenceDeserializer.class)
    private ServiceNowReference assignment_group;
    @JsonDeserialize(using = ServiceNowReferenceDeserializer.class)
    private ServiceNowReference cmdb_ci;
    @JsonDeserialize(using = ServiceNowReferenceDeserializer.class)
    private ServiceNowReference caller_id;

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

    public String getSys_created_on() {
        return sys_created_on;
    }

    public void setSys_created_on(String sys_created_on) {
        this.sys_created_on = sys_created_on;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public ServiceNowReference getAssigned_to() {
        return assigned_to;
    }

    public void setAssigned_to(ServiceNowReference assigned_to) {
        this.assigned_to = assigned_to;
    }

    public ServiceNowReference getAssignment_group() {
        return assignment_group;
    }

    public void setAssignment_group(ServiceNowReference assignment_group) {
        this.assignment_group = assignment_group;
    }

    public ServiceNowReference getCmdb_ci() {
        return cmdb_ci;
    }

    public void setCmdb_ci(ServiceNowReference cmdb_ci) {
        this.cmdb_ci = cmdb_ci;
    }

    public ServiceNowReference getCaller_id() {
        return caller_id;
    }

    public void setCaller_id(ServiceNowReference caller_id) {
        this.caller_id = caller_id;
    }
}
