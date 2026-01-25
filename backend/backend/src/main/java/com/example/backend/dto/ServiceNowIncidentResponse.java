package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNowIncidentResponse {
    private List<ServiceNowIncident> result;

    public List<ServiceNowIncident> getResult() {
        return result;
    }

    public void setResult(List<ServiceNowIncident> result) {
        this.result = result;
    }
}
