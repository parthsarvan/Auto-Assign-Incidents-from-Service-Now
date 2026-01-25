package com.example.backend.service;

import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowIncidentResponse;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ServiceNowIncidentClient {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentClient.class);
    private static final String INCIDENT_FIELDS = "sys_id,number,short_description,state,assigned_to,cmdb_ci";

    private final RestTemplate restTemplate;
    private final ServiceNowOAuthService oAuthService;
    private final String instanceUrl;
    private final String ciSysId;

    public ServiceNowIncidentClient(
            RestTemplate restTemplate,
            ServiceNowOAuthService oAuthService,
            @Value("${servicenow.instance-url}") String instanceUrl,
            @Value("${servicenow.incident.ci-sys-id}") String ciSysId) {
        this.restTemplate = restTemplate;
        this.oAuthService = oAuthService;
        this.instanceUrl = instanceUrl;
        this.ciSysId = ciSysId;
    }

    public List<ServiceNowIncident> fetchUnassignedIncidents() {
        String token = oAuthService.getAccessToken();
        String query = String.format("cmdb_ci=%s^assigned_toISEMPTY^stateNOT IN6,7", ciSysId);
        String url = UriComponentsBuilder.fromHttpUrl(instanceUrl)
                .path("/api/now/table/incident")
                .queryParam("sysparm_query", query)
                .queryParam("sysparm_fields", INCIDENT_FIELDS)
                .queryParam("sysparm_limit", "100")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<ServiceNowIncidentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    ServiceNowIncidentResponse.class);
            List<ServiceNowIncident> incidents =
                    response.getBody() != null ? response.getBody().getResult() : Collections.emptyList();
            logIncidents(incidents);
            return incidents;
        } catch (RestClientException ex) {
            logger.error("Failed to fetch ServiceNow incidents: {}", ex.getMessage());
            throw ex;
        }
    }

    private void logIncidents(List<ServiceNowIncident> incidents) {
        logger.info("ServiceNow incident fetch returned {} incidents.", incidents.size());
        for (ServiceNowIncident incident : incidents) {
            String cmdb = incident.getCmdb_ci() != null ? incident.getCmdb_ci().getDisplayValue() : "unknown";
            logger.info(
                    "Incident {} (sys_id={}) state={} cmdb_ci={} assigned_to={}",
                    incident.getNumber(),
                    incident.getSys_id(),
                    incident.getState(),
                    cmdb,
                    incident.getAssigned_to() != null ? incident.getAssigned_to().getDisplayValue() : "unassigned");
        }
    }
}
