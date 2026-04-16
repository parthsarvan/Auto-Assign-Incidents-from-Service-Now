package com.example.backend.service;

import com.example.backend.dto.ServiceNowValidationIssue;
import com.example.backend.dto.ServiceNowValidationResponse;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.ConfigurationItemRepository;
import com.example.backend.repository.TeamMemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ServiceNowValidationService {
    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;
    private final ConfigurationItemRepository configurationItemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ObjectMapper objectMapper;
    private final CurrentWorkspaceService currentWorkspaceService;

    public ServiceNowValidationService(
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider,
            OrganizationServiceNowConfigService organizationServiceNowConfigService,
            ConfigurationItemRepository configurationItemRepository,
            TeamMemberRepository teamMemberRepository,
            ObjectMapper objectMapper,
            CurrentWorkspaceService currentWorkspaceService) {
        this.restTemplate = restTemplate;
        this.authHeaderProvider = authHeaderProvider;
        this.organizationServiceNowConfigService = organizationServiceNowConfigService;
        this.configurationItemRepository = configurationItemRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.objectMapper = objectMapper;
        this.currentWorkspaceService = currentWorkspaceService;
    }

    public ServiceNowValidationResponse validateRecords() {
        Instant checkedAt = Instant.now();
        Team team = currentWorkspaceService.getCurrentTeam();
        if (!organizationServiceNowConfigService.isConfiguredForTeam(team)) {
            return new ServiceNowValidationResponse(
                    checkedAt,
                    false,
                    "Connect ServiceNow for this organization before validating records.",
                    0,
                    0,
                    0,
                    0,
                    List.of());
        }
        List<ConfigurationItem> configurationItems = configurationItemRepository.findAllByTeamOrderByNameAsc(team).stream()
                .filter(ci -> hasText(ci.getServiceNowSysId()))
                .toList();
        List<TeamMember> teamMembers = teamMemberRepository.findAllByTeamOrderByName(team).stream()
                .filter(member -> hasText(member.getSys_id()))
                .toList();

        try {
            Set<String> validCiIds = fetchValidSysIds(team, "cmdb_ci", configurationItems.stream()
                    .map(ConfigurationItem::getServiceNowSysId)
                    .toList());
            Set<String> validUserIds = fetchValidSysIds(team, "sys_user", teamMembers.stream()
                    .map(TeamMember::getSys_id)
                    .toList());

            List<ServiceNowValidationIssue> issues = new ArrayList<>();
            for (ConfigurationItem item : configurationItems) {
                if (!validCiIds.contains(item.getServiceNowSysId())) {
                    issues.add(new ServiceNowValidationIssue(
                            "CONFIGURATION_ITEM",
                            item.getName(),
                            item.getServiceNowSysId(),
                            "CI sys_id was not found in ServiceNow."));
                }
            }
            for (TeamMember member : teamMembers) {
                if (!validUserIds.contains(member.getSys_id())) {
                    String fullName = String.format("%s %s", member.getF_name(), member.getL_name());
                    issues.add(new ServiceNowValidationIssue(
                            "TEAM_MEMBER",
                            fullName,
                            member.getSys_id(),
                            "User sys_id was not found in ServiceNow."));
                }
            }

            boolean valid = issues.isEmpty();
            String message = valid
                    ? "All stored ServiceNow sys IDs were validated successfully."
                    : "Some stored ServiceNow sys IDs could not be found.";
            return new ServiceNowValidationResponse(
                    checkedAt,
                    valid,
                    message,
                    configurationItems.size(),
                    validCiIds.size(),
                    teamMembers.size(),
                    validUserIds.size(),
                    issues);
        } catch (IllegalStateException | RestClientException ex) {
            return new ServiceNowValidationResponse(
                    checkedAt,
                    false,
                    "Validation could not be completed: " + ex.getMessage(),
                    configurationItems.size(),
                    0,
                    teamMembers.size(),
                    0,
                    List.of());
        }
    }

    private Set<String> fetchValidSysIds(Team team, String tableName, List<String> sysIds) {
        if (sysIds.isEmpty()) {
            return Set.of();
        }

        ServiceNowConnectionSettings settings = organizationServiceNowConfigService.requireSettingsForTeam(team);
        HttpHeaders headers = authHeaderProvider.buildHeaders(settings.username(), settings.password());
        String url = UriComponentsBuilder.fromHttpUrl(settings.instanceUrl())
                .path("/api/now/table/" + tableName)
                .queryParam("sysparm_query", "sys_idIN" + String.join(",", sysIds))
                .queryParam("sysparm_fields", "sys_id")
                .queryParam("sysparm_limit", Math.max(sysIds.size(), 1))
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        return extractSysIds(response.getBody());
    }

    private Set<String> extractSysIds(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("result");
            Set<String> sysIds = new HashSet<>();
            if (results.isArray()) {
                for (JsonNode item : results) {
                    String sysId = item.path("sys_id").asText();
                    if (hasText(sysId)) {
                        sysIds.add(sysId);
                    }
                }
            }
            return sysIds;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse ServiceNow validation response.", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
