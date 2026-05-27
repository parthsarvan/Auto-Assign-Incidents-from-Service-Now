package com.example.backend.service;

import com.example.backend.dto.ServiceNowConfigRequest;
import com.example.backend.dto.ServiceNowConfigResponse;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Team;
import com.example.backend.repository.OrganizationRepository;
import com.example.backend.repository.TeamRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
public class OrganizationServiceNowConfigService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;

    public OrganizationServiceNowConfigService(
            CurrentWorkspaceService currentWorkspaceService,
            OrganizationRepository organizationRepository,
            TeamRepository teamRepository,
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.organizationRepository = organizationRepository;
        this.teamRepository = teamRepository;
        this.restTemplate = restTemplate;
        this.authHeaderProvider = authHeaderProvider;
    }

    public ServiceNowConfigResponse getCurrentOrganizationConfig() {
        Organization organization = getCurrentOrganization();
        return toResponse(organization);
    }

    public ServiceNowConfigResponse saveCurrentOrganizationConfig(ServiceNowConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ServiceNow configuration is required.");
        }

        String instanceUrl = normalizeUrl(request.getInstanceUrl());
        String username = normalizeCompactText(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword().trim();
        List<String> assignmentGroups = normalizeAssignmentGroups(request.getAssignmentGroups());

        if (instanceUrl.isBlank()) {
            throw new IllegalArgumentException("ServiceNow instance URL is required.");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("ServiceNow username is required.");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("ServiceNow password is required.");
        }
        if (assignmentGroups.isEmpty()) {
            throw new IllegalArgumentException("At least one ServiceNow assignment group is required for this team.");
        }

        Organization organization = getCurrentOrganization();
        Team team = getCurrentTeam();
        validateAssignmentGroupOwnership(team, assignmentGroups);
        validateConnection(new ServiceNowConnectionSettings(instanceUrl, username, password));

        organization.setServiceNowInstanceUrl(instanceUrl);
        organization.setServiceNowUsername(username);
        organization.setServiceNowPassword(password);
        organization.setServiceNowConnectedAt(Instant.now());
        organization = organizationRepository.save(organization);
        team.setServiceNowAssignmentGroups(String.join("\n", assignmentGroups));
        teamRepository.save(team);
        return toResponse(organization);
    }

    public boolean isConfigured(Organization organization) {
        return organization != null
                && hasText(organization.getServiceNowInstanceUrl())
                && hasText(organization.getServiceNowUsername())
                && hasText(organization.getServiceNowPassword())
                && organization.getServiceNowConnectedAt() != null;
    }

    public boolean isConfiguredForCurrentOrganization() {
        return isConfigured(getCurrentOrganization());
    }

    public boolean isConfiguredForTeam(Team team) {
        Team resolvedTeam = resolveTeam(team);
        return resolvedTeam != null
                && isConfigured(resolveOrganizationForTeam(resolvedTeam))
                && !getAssignmentGroupsForTeam(resolvedTeam).isEmpty();
    }

    public ServiceNowConnectionSettings requireCurrentOrganizationSettings() {
        return requireSettings(getCurrentOrganization());
    }

    public ServiceNowConnectionSettings requireSettingsForTeam(Team team) {
        Team resolvedTeam = resolveTeam(team);
        if (resolvedTeam == null) {
            throw new IllegalStateException("No organization is available for the current team.");
        }
        return requireSettings(resolveOrganizationForTeam(resolvedTeam));
    }

    private ServiceNowConnectionSettings requireSettings(Organization organization) {
        if (!isConfigured(organization)) {
            throw new IllegalStateException("Connect ServiceNow for this organization before continuing setup.");
        }
        return new ServiceNowConnectionSettings(
                organization.getServiceNowInstanceUrl(),
                organization.getServiceNowUsername(),
                organization.getServiceNowPassword());
    }

    private void validateConnection(ServiceNowConnectionSettings settings) {
        HttpHeaders headers = authHeaderProvider.buildHeaders(settings.username(), settings.password());
        String url = UriComponentsBuilder.fromHttpUrl(settings.instanceUrl())
                .path("/api/now/table/incident")
                .queryParam("sysparm_limit", "1")
                .queryParam("sysparm_fields", "sys_id")
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("ServiceNow connection could not be verified.");
            }
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("ServiceNow validation failed: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("ServiceNow validation failed: " + ex.getMessage(), ex);
        }
    }

    private Organization getCurrentOrganization() {
        var user = currentWorkspaceService.getCurrentUser();
        if (user.getCurrentOrganization() == null) {
            throw new IllegalStateException("No active organization is available.");
        }
        return organizationRepository.findById(user.getCurrentOrganization().getOrg_id())
                .orElse(user.getCurrentOrganization());
    }

    private Team getCurrentTeam() {
        Team team = currentWorkspaceService.getCurrentTeam();
        if (team == null) {
            throw new IllegalStateException("No active team is available.");
        }
        return teamRepository.findById(team.getTeam_id()).orElse(team);
    }

    private ServiceNowConfigResponse toResponse(Organization organization) {
        Team team = getCurrentTeam();
        return new ServiceNowConfigResponse(
                isConfigured(organization),
                organization != null ? organization.getServiceNowInstanceUrl() : null,
                organization != null ? organization.getServiceNowUsername() : null,
                organization != null ? organization.getServiceNowConnectedAt() : null,
                getAssignmentGroupsForTeam(team));
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (!normalized.isBlank() && !normalized.toLowerCase(Locale.ROOT).startsWith("http")) {
            normalized = "https://" + normalized;
        }
        return normalized.replaceAll("/+$", "");
    }

    private String normalizeCompactText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public List<String> getAssignmentGroupsForTeam(Team team) {
        Team resolvedTeam = resolveTeam(team);
        if (resolvedTeam == null || resolvedTeam.getServiceNowAssignmentGroups() == null) {
            return List.of();
        }
        return normalizeAssignmentGroups(List.of(resolvedTeam.getServiceNowAssignmentGroups().split("[\\n,]+")));
    }

    public List<String> findAssignmentGroupConflicts(Team team, List<String> assignmentGroups) {
        Team resolvedTeam = resolveTeam(team);
        if (resolvedTeam == null || resolvedTeam.getOrganization() == null) {
            return List.of();
        }
        Organization organization = resolveOrganizationForTeam(resolvedTeam);
        Set<String> requestedKeys = assignmentGroups.stream()
                .map(this::assignmentGroupKey)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (requestedKeys.isEmpty()) {
            return List.of();
        }

        List<String> conflicts = new ArrayList<>();
        for (Team otherTeam : teamRepository.findAllByOrganizationOrderByNameAsc(organization)) {
            if (otherTeam.getTeam_id() == null || otherTeam.getTeam_id().equals(resolvedTeam.getTeam_id())) {
                continue;
            }
            for (String otherGroup : getAssignmentGroupsForTeam(otherTeam)) {
                if (requestedKeys.contains(assignmentGroupKey(otherGroup))) {
                    conflicts.add(String.format("%s is already monitored by %s", otherGroup, otherTeam.getName()));
                }
            }
        }
        return conflicts;
    }

    private Team resolveTeam(Team team) {
        if (team == null || team.getTeam_id() == null) {
            return null;
        }
        return teamRepository.findById(team.getTeam_id()).orElse(team);
    }

    private Organization resolveOrganizationForTeam(Team team) {
        if (team == null || team.getOrganization() == null || team.getOrganization().getOrg_id() == null) {
            throw new IllegalStateException("No organization is available for the current team.");
        }
        return organizationRepository.findById(team.getOrganization().getOrg_id())
                .orElse(team.getOrganization());
    }

    private void validateAssignmentGroupOwnership(Team team, List<String> assignmentGroups) {
        List<String> conflicts = findAssignmentGroupConflicts(team, assignmentGroups);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Each ServiceNow assignment group can be monitored by only one team in an organization. "
                            + String.join("; ", conflicts)
                            + ".");
        }
    }

    private List<String> normalizeAssignmentGroups(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String candidate = normalizeCompactText(value);
            if (!candidate.isBlank()) {
                normalized.add(candidate);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String assignmentGroupKey(String value) {
        return normalizeCompactText(value).toLowerCase(Locale.ROOT);
    }
}
