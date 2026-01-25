package com.example.backend.service;

import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowIncidentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ServiceNowIncidentClient {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentClient.class);
    private static final String INCIDENT_FIELDS = "sys_id,number,short_description,state,assigned_to,cmdb_ci";

    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;
    private final ObjectMapper objectMapper;
    private final String instanceUrl;
    private final String ciSysId;

    public ServiceNowIncidentClient(
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider,
            ObjectMapper objectMapper,
            @Value("${servicenow.instance-url}") String instanceUrl,
            @Value("${servicenow.incident.ci-sys-id}") String ciSysId) {
        this.restTemplate = restTemplate;
        this.authHeaderProvider = authHeaderProvider;
        this.objectMapper = objectMapper;
        this.instanceUrl = instanceUrl;
        this.ciSysId = ciSysId;
    }

    public List<ServiceNowIncident> fetchUnassignedIncidents() {
        String query = String.format("cmdb_ci=%s^assigned_toISEMPTY^stateNOT IN6,7", ciSysId);
        String url = UriComponentsBuilder.fromHttpUrl(instanceUrl)
                .path("/api/now/table/incident")
                .queryParam("sysparm_query", query)
                .queryParam("sysparm_fields", INCIDENT_FIELDS)
                .queryParam("sysparm_limit", "100")
                .toUriString();

        HttpHeaders headers = authHeaderProvider.buildHeaders();

        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class);
            ServiceNowIncidentResponse payload = parseIncidentResponse(response.getBody());
            List<ServiceNowIncident> incidents = payload != null && payload.getResult() != null
                    ? payload.getResult()
                    : Collections.emptyList();
            logIncidents(incidents);
            return incidents;
        } catch (HttpStatusCodeException ex) {
            logger.error(
                    "Failed to fetch ServiceNow incidents: status={} body={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw ex;
        } catch (RestClientException ex) {
            logger.error("Failed to fetch ServiceNow incidents: {}", ex.getMessage());
            throw ex;
        }
    }

    private ServiceNowIncidentResponse parseIncidentResponse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, ServiceNowIncidentResponse.class);
        } catch (JsonProcessingException ex) {
            logger.error("Failed to parse ServiceNow incident response body: {}", body);
            throw new IllegalStateException("Unable to parse ServiceNow incident response.", ex);
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
