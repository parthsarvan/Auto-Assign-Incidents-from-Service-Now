package com.example.backend.service;

import com.example.backend.dto.ServiceNowConfigRequest;
import com.example.backend.dto.ServiceNowConfigResponse;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Team;
import com.example.backend.repository.OrganizationRepository;
import java.time.Instant;
import java.util.Locale;
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
    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;

    public OrganizationServiceNowConfigService(
            CurrentWorkspaceService currentWorkspaceService,
            OrganizationRepository organizationRepository,
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.organizationRepository = organizationRepository;
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

        if (instanceUrl.isBlank()) {
            throw new IllegalArgumentException("ServiceNow instance URL is required.");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("ServiceNow username is required.");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("ServiceNow password is required.");
        }

        validateConnection(new ServiceNowConnectionSettings(instanceUrl, username, password));

        Organization organization = getCurrentOrganization();
        organization.setServiceNowInstanceUrl(instanceUrl);
        organization.setServiceNowUsername(username);
        organization.setServiceNowPassword(password);
        organization.setServiceNowConnectedAt(Instant.now());
        organization = organizationRepository.save(organization);
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
        return team != null && isConfigured(team.getOrganization());
    }

    public ServiceNowConnectionSettings requireCurrentOrganizationSettings() {
        return requireSettings(getCurrentOrganization());
    }

    public ServiceNowConnectionSettings requireSettingsForTeam(Team team) {
        if (team == null || team.getOrganization() == null) {
            throw new IllegalStateException("No organization is available for the current team.");
        }
        return requireSettings(team.getOrganization());
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

    private ServiceNowConfigResponse toResponse(Organization organization) {
        return new ServiceNowConfigResponse(
                isConfigured(organization),
                organization != null ? organization.getServiceNowInstanceUrl() : null,
                organization != null ? organization.getServiceNowUsername() : null,
                organization != null ? organization.getServiceNowConnectedAt() : null);
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
}
