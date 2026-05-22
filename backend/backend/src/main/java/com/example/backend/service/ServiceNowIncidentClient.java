package com.example.backend.service;

import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowIncidentResponse;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.entity.Team;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            "sys_id,number,short_description,sys_created_on,state,priority,assigned_to,assignment_group,cmdb_ci,caller_id";

    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;
    private final ObjectMapper objectMapper;
    private final CurrentWorkspaceService currentWorkspaceService;

    public ServiceNowIncidentClient(
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider,
            OrganizationServiceNowConfigService organizationServiceNowConfigService,
            ObjectMapper objectMapper,
            CurrentWorkspaceService currentWorkspaceService) {
        this.restTemplate = restTemplate;
        this.authHeaderProvider = authHeaderProvider;
        this.organizationServiceNowConfigService = organizationServiceNowConfigService;
        this.objectMapper = objectMapper;
        this.currentWorkspaceService = currentWorkspaceService;
    }

    public List<ServiceNowIncident> fetchUnassignedIncidents() {
        Team team = currentWorkspaceService.getCurrentTeam();
        List<String> assignmentGroups =
                organizationServiceNowConfigService.getAssignmentGroupsForTeam(team);
        if (assignmentGroups.isEmpty()) {
            logger.warn("No ServiceNow assignment groups configured for current team; returning no incidents.");
            return Collections.emptyList();
        }

        ServiceNowConnectionSettings settings = organizationServiceNowConfigService
                .requireSettingsForTeam(team);
        List<ServiceNowIncident> incidents = fetchIncidents(settings, buildQuery());
        if (incidents.isEmpty()) {
            return incidents;
        }

        Set<String> normalizedAssignmentGroups = assignmentGroups.stream()
                .map(this::normalizeValue)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        List<ServiceNowIncident> filteredIncidents = incidents.stream()
                .filter(incident -> matchesAssignmentGroup(incident, normalizedAssignmentGroups))
                .toList();

        logger.info(
                "ServiceNow assignment-group filter kept {} of {} fetched incidents for team {}.",
                filteredIncidents.size(),
                incidents.size(),
                team.getName());
        logIncidents(filteredIncidents);
        return filteredIncidents;
    }

    private List<ServiceNowIncident> fetchIncidents(ServiceNowConnectionSettings settings, String query) {
        return fetchIncidents(settings, query, true);
    }

    private List<ServiceNowIncident> fetchIncidents(
            ServiceNowConnectionSettings settings,
            String query,
            boolean requireUnassigned) {
        return fetchIncidents(settings, query, requireUnassigned, "true");
    }

    private List<ServiceNowIncident> fetchIncidents(
            ServiceNowConnectionSettings settings,
            String query,
            boolean requireUnassigned,
            String displayValueMode) {
        logger.info("ServiceNow incident query: {}", query);
        String url = UriComponentsBuilder.fromHttpUrl(settings.instanceUrl())
                .path("/api/now/table/incident")
                .queryParam("sysparm_query", query)
                .queryParam("sysparm_fields", INCIDENT_FIELDS)
                .queryParam("sysparm_display_value", displayValueMode)
                .queryParam("sysparm_exclude_reference_link", "true")
                .queryParam("sysparm_limit", "100")
                .toUriString();
        HttpHeaders headers = authHeaderProvider.buildHeaders(settings.username(), settings.password());

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
            if (requireUnassigned) {
                incidents = incidents.stream()
                        .filter(this::isUnassigned)
                        .toList();
            }
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

    public List<ServiceNowIncident> fetchActiveIncidentsAssignedTo(String assigneeSysId) {
        if (assigneeSysId == null || assigneeSysId.isBlank()) {
            return Collections.emptyList();
        }
        ServiceNowConnectionSettings settings = organizationServiceNowConfigService
                .requireSettingsForTeam(currentWorkspaceService.getCurrentTeam());
        String query = "assigned_to=" + assigneeSysId.trim() + "^stateNOT IN6,7";
        return fetchIncidents(settings, query, false, "false").stream()
                .filter(incident -> isAssignedTo(incident, assigneeSysId))
                .filter(this::isActiveState)
                .toList();
    }

    public List<ServiceNowIncident> fetchActiveCriticalIncidentsAssignedTo(String assigneeSysId) {
        return fetchActiveIncidentsAssignedTo(assigneeSysId).stream()
                .filter(this::isCriticalPriority)
                .toList();
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

        ServiceNowConnectionSettings settings = organizationServiceNowConfigService
                .requireSettingsForTeam(currentWorkspaceService.getCurrentTeam());
        String url = UriComponentsBuilder.fromHttpUrl(settings.instanceUrl())
                .pathSegment("api", "now", "table", "incident", incidentSysId)
                .queryParam("sysparm_input_display_value", "false")
                .queryParam("sysparm_exclude_reference_link", "true")
                .toUriString();

        HttpHeaders headers = authHeaderProvider.buildHeaders(settings.username(), settings.password());
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

    public boolean addWorkNote(String incidentSysId, String workNote) {
        if (incidentSysId == null || incidentSysId.isBlank()) {
            logger.warn("Cannot add ServiceNow work note: missing incident sys_id.");
            return false;
        }
        if (workNote == null || workNote.isBlank()) {
            return true;
        }

        ServiceNowConnectionSettings settings = organizationServiceNowConfigService
                .requireSettingsForTeam(currentWorkspaceService.getCurrentTeam());
        String url = UriComponentsBuilder.fromHttpUrl(settings.instanceUrl())
                .pathSegment("api", "now", "table", "incident", incidentSysId)
                .queryParam("sysparm_input_display_value", "false")
                .queryParam("sysparm_exclude_reference_link", "true")
                .toUriString();

        HttpHeaders headers = authHeaderProvider.buildHeaders(settings.username(), settings.password());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(Map.of("work_notes", workNote), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
            logger.info("Added ServiceNow work note to incident {}.", incidentSysId);
            return true;
        } catch (HttpStatusCodeException ex) {
            logger.error(
                    "Failed to add ServiceNow work note to incident {}: status={} body={}",
                    incidentSysId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            return false;
        } catch (RestClientException ex) {
            logger.error("Failed to add ServiceNow work note to incident {}: {}", incidentSysId, ex.getMessage());
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

    private boolean isUnassigned(ServiceNowIncident incident) {
        if (incident == null || incident.getAssigned_to() == null) {
            return true;
        }
        String value = incident.getAssigned_to().getValue();
        String display = incident.getAssigned_to().getDisplayValue();
        return (value == null || value.isBlank()) && (display == null || display.isBlank());
    }

    private boolean matchesAssignmentGroup(ServiceNowIncident incident, Set<String> normalizedAssignmentGroups) {
        ServiceNowReference assignmentGroup = incident.getAssignment_group();
        if (assignmentGroup == null) {
            return false;
        }

        String displayValue = normalizeValue(assignmentGroup.getDisplayValue());
        String rawValue = normalizeValue(assignmentGroup.getValue());
        return normalizedAssignmentGroups.contains(displayValue)
                || normalizedAssignmentGroups.contains(rawValue);
    }

    private String buildQuery() {
        return "assigned_toISEMPTY^stateNOT IN6,7";
    }

    private boolean isCriticalPriority(ServiceNowIncident incident) {
        String normalized = normalizeValue(incident != null ? incident.getPriority() : null)
                .replaceAll("[^a-z0-9]", "");
        return normalized.startsWith("0")
                || normalized.startsWith("p0")
                || normalized.startsWith("1")
                || normalized.startsWith("p1c")
                || normalized.contains("critical");
    }

    private boolean isAssignedTo(ServiceNowIncident incident, String assigneeSysId) {
        if (incident == null || incident.getAssigned_to() == null || assigneeSysId == null) {
            return false;
        }
        String normalizedAssigneeSysId = normalizeValue(assigneeSysId);
        String assignedToValue = normalizeValue(incident.getAssigned_to().getValue());
        String assignedToDisplay = normalizeValue(incident.getAssigned_to().getDisplayValue());
        return normalizedAssigneeSysId.equals(assignedToValue)
                || normalizedAssigneeSysId.equals(assignedToDisplay);
    }

    private boolean isActiveState(ServiceNowIncident incident) {
        String normalized = normalizeValue(incident != null ? incident.getState() : null)
                .replaceAll("[^a-z0-9]", "");
        return !normalized.equals("6")
                && !normalized.equals("7")
                && !normalized.contains("resolved")
                && !normalized.contains("closed")
                && !normalized.contains("cancelled")
                && !normalized.contains("canceled");
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void logIncidents(List<ServiceNowIncident> incidents) {
        logger.info("ServiceNow incident fetch returned {} incidents.", incidents.size());
        for (ServiceNowIncident incident : incidents) {
            String cmdb = incident.getCmdb_ci() != null ? incident.getCmdb_ci().getDisplayValue() : "unknown";
            logger.info(
                    "Incident {} (sys_id={}) state={} assignment_group={} cmdb_ci={} assigned_to={}",
                    incident.getNumber(),
                    incident.getSys_id(),
                    incident.getState(),
                    incident.getAssignment_group() != null ? incident.getAssignment_group().getDisplayValue() : "unknown",
                    cmdb,
                    incident.getAssigned_to() != null ? incident.getAssigned_to().getDisplayValue() : "unassigned");
        }
    }
}
