package com.example.backend.service;

import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowIncidentResponse;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.Team;
import com.example.backend.repository.ConfigurationItemRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ServiceNowIncidentClient {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowIncidentClient.class);
    private static final String INCIDENT_FIELDS =
            "sys_id,number,short_description,sys_created_on,state,priority,assigned_to,cmdb_ci,caller_id";

    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;
    private final ConfigurationItemRepository configurationItemRepository;
    private final ObjectMapper objectMapper;
    private final String instanceUrl;
    private final CurrentWorkspaceService currentWorkspaceService;

    public ServiceNowIncidentClient(
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider,
            ConfigurationItemRepository configurationItemRepository,
            ObjectMapper objectMapper,
            CurrentWorkspaceService currentWorkspaceService,
            @Value("${servicenow.instance-url}") String instanceUrl) {
        this.restTemplate = restTemplate;
        this.authHeaderProvider = authHeaderProvider;
        this.configurationItemRepository = configurationItemRepository;
        this.objectMapper = objectMapper;
        this.currentWorkspaceService = currentWorkspaceService;
        this.instanceUrl = instanceUrl;
    }

    public List<ServiceNowIncident> fetchUnassignedIncidents() {
        String query = buildQuery();
        logger.info("ServiceNow incident query: {}", query);
        String url = UriComponentsBuilder.fromHttpUrl(instanceUrl)
                .path("/api/now/table/incident")
                .queryParam("sysparm_query", query)
                .queryParam("sysparm_fields", INCIDENT_FIELDS)
                .queryParam("sysparm_display_value", "true")
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
            incidents = incidents.stream()
                    .filter(this::isUnassigned)
                    .toList();
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

    public boolean assignIncidentBySysId(String incidentSysId, String assigneeSysId) {
        if (incidentSysId == null || incidentSysId.isBlank()) {
            logger.warn("Cannot assign ServiceNow incident: missing sys_id.");
            return false;
        }
        if (assigneeSysId == null || assigneeSysId.isBlank()) {
            logger.warn("Cannot assign ServiceNow incident {}: missing assignee sys_id.", incidentSysId);
            return false;
        }

        String url = UriComponentsBuilder.fromHttpUrl(instanceUrl)
                .pathSegment("api", "now", "table", "incident", incidentSysId)
                .queryParam("sysparm_input_display_value", "false")
                .queryParam("sysparm_exclude_reference_link", "true")
                .toUriString();

        HttpHeaders headers = authHeaderProvider.buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(Map.of("assigned_to", assigneeSysId), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
            logger.info("Assigned ServiceNow incident {} to sys_id {}.", incidentSysId, assigneeSysId);
            return true;
        } catch (HttpStatusCodeException ex) {
            logger.error(
                    "Failed to assign ServiceNow incident {}: status={} body={}",
                    incidentSysId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            return false;
        } catch (RestClientException ex) {
            logger.error("Failed to assign ServiceNow incident {}: {}", incidentSysId, ex.getMessage());
            return false;
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

    private String buildQuery() {
        Team team = currentWorkspaceService.getCurrentTeam();
        List<String> ciSysIds = configurationItemRepository.findAllByTeamOrderByNameAsc(team).stream()
                .map(ConfigurationItem::getServiceNowSysId)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        String baseFilter =
                "assigned_toISEMPTY^assigned_to=NULL^assigned_to=^assigned_to.sys_idISEMPTY^stateNOT IN6,7";
        if (ciSysIds.isEmpty()) {
            logger.warn("No ServiceNow CI sys IDs found for current team; returning no incidents.");
            return String.format("sys_idISEMPTY^%s", baseFilter);
        }
        String joinedIds = String.join(",", ciSysIds);
        return String.format("cmdb_ciIN%s^%s", joinedIds, baseFilter);
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

    private boolean isUnassigned(ServiceNowIncident incident) {
        if (incident == null || incident.getAssigned_to() == null) {
            return true;
        }
        String value = incident.getAssigned_to().getValue();
        String display = incident.getAssigned_to().getDisplayValue();
        return (value == null || value.isBlank()) && (display == null || display.isBlank());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
