package com.example.backend.service;

import com.example.backend.dto.ServiceNowLookupResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class ServiceNowLookupService {
    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;
    private final ObjectMapper objectMapper;

    public ServiceNowLookupService(
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider,
            OrganizationServiceNowConfigService organizationServiceNowConfigService,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.authHeaderProvider = authHeaderProvider;
        this.organizationServiceNowConfigService = organizationServiceNowConfigService;
        this.objectMapper = objectMapper;
    }

    public List<ServiceNowLookupResult> searchUsers(String query) {
        String normalizedQuery = normalizeLookupText(query);
        if (normalizedQuery.length() < 2) {
            return List.of();
        }
        String fields = "sys_id,name,first_name,last_name,email,user_name,active";
        Map<String, ServiceNowLookupResult> combinedResults = new LinkedHashMap<>();
        List<String> searchTerms = buildUserSearchTerms(normalizedQuery);

        for (String searchTerm : searchTerms) {
            addLookupResults(combinedResults, fetchLookupResults(
                    "sys_user",
                    buildActiveUserSearchQuery(searchTerm, "="),
                    fields,
                    true,
                    "20"));
            addLookupResults(combinedResults, fetchLookupResults(
                    "sys_user",
                    buildActiveUserSearchQuery(searchTerm, "STARTSWITH"),
                    fields,
                    true,
                    "50"));
            addLookupResults(combinedResults, fetchLookupResults(
                    "sys_user",
                    buildActiveUserSearchQuery(searchTerm, "LIKE"),
                    fields,
                    true,
                    "75"));
            addLookupResults(combinedResults, fetchLookupResults(
                    "sys_user",
                    "active=true^123TEXTQUERY321=" + searchTerm + "^ORDERBYname",
                    fields,
                    true,
                    "75"));
        }

        List<ServiceNowLookupResult> directResults = refineUserLookupResults(
                searchTerms,
                new ArrayList<>(combinedResults.values()));
        if (!directResults.isEmpty()) {
            return directResults;
        }

        // Some ServiceNow instances apply stricter behavior to table API filters
        // than their UI reference picker. As a fallback, pull a bounded active
        // user sample and apply the same matching logic inside InciTeam.
        addLookupResults(combinedResults, fetchLookupResults(
                "sys_user",
                "active=true^ORDERBYname",
                fields,
                true,
                "1000"));
        return refineUserLookupResults(searchTerms, new ArrayList<>(combinedResults.values()));
    }

    public List<ServiceNowLookupResult> searchConfigurationItems(String query) {
        String normalizedQuery = normalizeLookupText(query);
        if (normalizedQuery.length() < 2) {
            return List.of();
        }
        String fields = "sys_id,name,sys_class_name,asset_tag,serial_number,operational_status";
        Map<String, ServiceNowLookupResult> combinedResults = new LinkedHashMap<>();

        addLookupResults(combinedResults, fetchLookupResults(
                "cmdb_ci",
                "name=" + normalizedQuery + "^ORDERBYname",
                fields,
                false,
                "20"));
        addLookupResults(combinedResults, fetchLookupResults(
                "cmdb_ci",
                "nameSTARTSWITH" + normalizedQuery + "^ORDERBYname",
                fields,
                false,
                "50"));
        addLookupResults(combinedResults, fetchLookupResults(
                "cmdb_ci",
                "nameLIKE" + normalizedQuery + "^ORDERBYname",
                fields,
                false,
                "75"));
        addLookupResults(combinedResults, fetchLookupResults(
                "cmdb_ci",
                buildCiFieldSearchQuery(normalizedQuery),
                fields,
                false,
                "50"));

        // ServiceNow's reference picker can search across the broader reference
        // list using its text index. This mirrors that behavior better than only
        // checking a few specific CMDB fields.
        addLookupResults(combinedResults, fetchLookupResults(
                "cmdb_ci",
                "123TEXTQUERY321=" + normalizedQuery + "^ORDERBYname",
                fields,
                false,
                "75"));

        List<ServiceNowLookupResult> directResults = refineCiLookupResults(
                normalizedQuery,
                new ArrayList<>(combinedResults.values()));
        if (directResults.size() >= 5) {
            return directResults;
        }

        addLookupResults(combinedResults, fetchIncidentConfigurationItemResults(normalizedQuery));
        return refineCiLookupResults(normalizedQuery, new ArrayList<>(combinedResults.values()));
    }

    private String buildActiveUserSearchQuery(String query, String operator) {
        return "active=true^name" + operator + query
                + "^NQactive=true^email" + operator + query
                + "^NQactive=true^user_name" + operator + query
                + "^NQactive=true^first_name" + operator + query
                + "^NQactive=true^last_name" + operator + query
                + "^ORDERBYname";
    }

    private List<String> buildUserSearchTerms(String query) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        addUserSearchTerm(terms, query);

        int atIndex = query.indexOf('@');
        if (atIndex > 0) {
            addUserSearchTerm(terms, query.substring(0, atIndex));
        }

        addUserSearchTerm(terms, toNameLikeTerm(query));
        return new ArrayList<>(terms);
    }

    private void addUserSearchTerm(LinkedHashSet<String> terms, String value) {
        String normalized = normalizeLookupText(value);
        if (normalized.length() >= 2) {
            terms.add(normalized);
        }
    }

    private String toNameLikeTerm(String value) {
        if (value == null) {
            return "";
        }
        String candidate = value;
        int atIndex = candidate.indexOf('@');
        if (atIndex > 0) {
            candidate = candidate.substring(0, atIndex);
        }
        return candidate
                .replaceAll("[._-]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private void addLookupResults(
            Map<String, ServiceNowLookupResult> combinedResults,
            List<ServiceNowLookupResult> results) {
        if (results == null) {
            return;
        }
        for (ServiceNowLookupResult result : results) {
            if (result == null || !hasText(result.getSysId())) {
                continue;
            }
            combinedResults.putIfAbsent(result.getSysId(), result);
        }
    }

    private List<ServiceNowLookupResult> refineUserLookupResults(
            List<String> queries,
            List<ServiceNowLookupResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<String> normalizedQueries = queries.stream()
                .map(this::normalizeForComparison)
                .filter(this::hasText)
                .toList();
        return results.stream()
                .filter(result -> containsAnySearchTerm(searchableUserText(result), normalizedQueries))
                .sorted(Comparator
                        .comparingInt((ServiceNowLookupResult result) -> bestUserResultScore(result, normalizedQueries))
                        .thenComparing(result -> safeLower(result.getDisplayName()))
                        .thenComparing(result -> safeLower(result.getEmail())))
                .limit(20)
                .toList();
    }

    private boolean containsAnySearchTerm(String searchableText, List<String> normalizedQueries) {
        if (!hasText(searchableText) || normalizedQueries == null || normalizedQueries.isEmpty()) {
            return false;
        }
        return normalizedQueries.stream().anyMatch(searchableText::contains);
    }

    private int bestUserResultScore(ServiceNowLookupResult result, List<String> normalizedQueries) {
        if (normalizedQueries == null || normalizedQueries.isEmpty()) {
            return 99;
        }
        return normalizedQueries.stream()
                .mapToInt(query -> userResultScore(result, query))
                .min()
                .orElse(99);
    }

    private int userResultScore(ServiceNowLookupResult result, String normalizedQuery) {
        String displayName = normalizeForComparison(result.getDisplayName());
        String email = normalizeForComparison(result.getEmail());
        String userName = normalizeForComparison(result.getUserName());
        if (email.equals(normalizedQuery) || userName.equals(normalizedQuery) || displayName.equals(normalizedQuery)) {
            return 0;
        }
        if (displayName.startsWith(normalizedQuery)) {
            return 1;
        }
        if (email.startsWith(normalizedQuery) || userName.startsWith(normalizedQuery)) {
            return 2;
        }
        if (displayName.contains(normalizedQuery)) {
            return 3;
        }
        if (email.contains(normalizedQuery) || userName.contains(normalizedQuery)) {
            return 4;
        }
        return 5;
    }

    private String searchableUserText(ServiceNowLookupResult result) {
        return normalizeForComparison(String.join(
                " ",
                firstNonBlank(result.getDisplayName(), "", ""),
                firstNonBlank(result.getEmail(), "", ""),
                firstNonBlank(result.getUserName(), "", ""),
                firstNonBlank(result.getDetail(), "", ""),
                firstNonBlank(result.getSecondaryDetail(), "", "")));
    }

    private List<ServiceNowLookupResult> refineCiLookupResults(
            String query,
            List<ServiceNowLookupResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = normalizeForComparison(query);
        return results.stream()
                .filter(result -> searchableCiText(result).contains(normalizedQuery))
                .sorted(Comparator
                        .comparingInt((ServiceNowLookupResult result) -> ciResultScore(result, normalizedQuery))
                        .thenComparing(result -> safeLower(result.getDisplayName())))
                .limit(20)
                .toList();
    }

    private int ciResultScore(ServiceNowLookupResult result, String normalizedQuery) {
        String displayName = normalizeForComparison(result.getDisplayName());
        String secondaryDetail = normalizeForComparison(result.getSecondaryDetail());
        String detail = normalizeForComparison(result.getDetail());
        if (displayName.equals(normalizedQuery)) {
            return 0;
        }
        if (displayName.startsWith(normalizedQuery)) {
            return 1;
        }
        if (displayName.contains(normalizedQuery)) {
            return 2;
        }
        if (secondaryDetail.contains(normalizedQuery)) {
            return 3;
        }
        if (detail.contains(normalizedQuery)) {
            return 4;
        }
        return 5;
    }

    private String searchableCiText(ServiceNowLookupResult result) {
        return normalizeForComparison(String.join(
                " ",
                firstNonBlank(result.getDisplayName(), "", ""),
                firstNonBlank(result.getDetail(), "", ""),
                firstNonBlank(result.getSecondaryDetail(), "", "")));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String buildCiFieldSearchQuery(String query) {
        return "nameLIKE" + query
                + "^ORasset_tagLIKE" + query
                + "^ORserial_numberLIKE" + query
                + "^ORDERBYname";
    }

    private List<ServiceNowLookupResult> fetchLookupResults(
            String tableName,
            String encodedQuery,
            String fields,
            boolean userLookup) {
        return fetchLookupResults(tableName, encodedQuery, fields, userLookup, "20");
    }

    private List<ServiceNowLookupResult> fetchLookupResults(
            String tableName,
            String encodedQuery,
            String fields,
            boolean userLookup,
            String limit) {
        ServiceNowConnectionSettings settings = organizationServiceNowConfigService
                .requireCurrentOrganizationSettings();
        HttpHeaders headers = authHeaderProvider.buildHeaders(settings.username(), settings.password());
        String url = UriComponentsBuilder.fromHttpUrl(settings.instanceUrl())
                .path("/api/now/table/" + tableName)
                .queryParam("sysparm_query", encodedQuery)
                .queryParam("sysparm_fields", fields)
                .queryParam("sysparm_display_value", "false")
                .queryParam("sysparm_exclude_reference_link", "true")
                .queryParam("sysparm_limit", limit)
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            return parseLookupResponse(response.getBody(), userLookup);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("ServiceNow lookup failed: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("ServiceNow lookup failed: " + ex.getMessage(), ex);
        }
    }

    private List<ServiceNowLookupResult> fetchIncidentConfigurationItemResults(String query) {
        String fields = "number,cmdb_ci";
        String encodedQuery = "cmdb_ciISNOTEMPTY^cmdb_ci.nameLIKE" + query;
        try {
            List<ServiceNowLookupResult> results = fetchIncidentConfigurationItemResults(query, encodedQuery, fields);
            if (!results.isEmpty()) {
                return results;
            }
            return fetchIncidentConfigurationItemResults(query, "cmdb_ciISNOTEMPTY", fields);
        } catch (IllegalStateException ex) {
            // Some ServiceNow instances restrict dot-walk filtering. Fall back to a small
            // incident sample and filter the display values in InciTeam.
            return fetchIncidentConfigurationItemResults(query, "cmdb_ciISNOTEMPTY", fields);
        }
    }

    private List<ServiceNowLookupResult> fetchIncidentConfigurationItemResults(
            String query,
            String encodedQuery,
            String fields) {
        ServiceNowConnectionSettings settings = organizationServiceNowConfigService
                .requireCurrentOrganizationSettings();
        HttpHeaders headers = authHeaderProvider.buildHeaders(settings.username(), settings.password());
        String url = UriComponentsBuilder.fromHttpUrl(settings.instanceUrl())
                .path("/api/now/table/incident")
                .queryParam("sysparm_query", encodedQuery)
                .queryParam("sysparm_fields", fields)
                .queryParam("sysparm_display_value", "all")
                .queryParam("sysparm_exclude_reference_link", "true")
                .queryParam("sysparm_limit", "100")
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            return parseIncidentConfigurationItemResponse(response.getBody(), query);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("ServiceNow incident CI lookup failed: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("ServiceNow incident CI lookup failed: " + ex.getMessage(), ex);
        }
    }

    private List<ServiceNowLookupResult> parseLookupResponse(String body, boolean userLookup) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("result");
            List<ServiceNowLookupResult> lookupResults = new ArrayList<>();
            if (!results.isArray()) {
                return lookupResults;
            }
            for (JsonNode item : results) {
                String sysId = readText(item, "sys_id");
                if (!hasText(sysId)) {
                    continue;
                }
                String active = readText(item, "active");
                if (userLookup && hasText(active) && !isTruthy(active)) {
                    continue;
                }
                lookupResults.add(userLookup ? toUserResult(item, sysId) : toCiResult(item, sysId));
            }
            return lookupResults;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse ServiceNow lookup response.", ex);
        }
    }

    private ServiceNowLookupResult toUserResult(JsonNode item, String sysId) {
        String name = readText(item, "name");
        String firstName = readText(item, "first_name");
        String lastName = readText(item, "last_name");
        String email = readText(item, "email");
        String userName = readText(item, "user_name");
        String fullName = (firstName + " " + lastName).trim();
        String displayName = firstNonBlank(name, fullName, firstNonBlank(email, userName, sysId));
        String detail = hasText(email) ? email : userName;
        return new ServiceNowLookupResult(sysId, displayName, email, userName, detail, "ServiceNow user");
    }

    private ServiceNowLookupResult toCiResult(JsonNode item, String sysId) {
        String name = readText(item, "name");
        String className = readText(item, "sys_class_name");
        String assetTag = readText(item, "asset_tag");
        String serialNumber = readText(item, "serial_number");
        String displayName = hasText(name) ? name : firstNonBlank(assetTag, serialNumber, sysId);
        String secondaryDetail = firstNonBlank(assetTag, serialNumber, null);
        return new ServiceNowLookupResult(sysId, displayName, null, null, className, secondaryDetail);
    }

    private List<ServiceNowLookupResult> parseIncidentConfigurationItemResponse(String body, String query) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("result");
            Map<String, ServiceNowLookupResult> uniqueResults = new LinkedHashMap<>();
            if (!results.isArray()) {
                return List.of();
            }
            String normalizedQuery = query.toLowerCase();
            for (JsonNode item : results) {
                JsonNode ciNode = item.path("cmdb_ci");
                String sysId = readReferenceValue(ciNode);
                String displayName = readReferenceDisplayValue(ciNode);
                if (!hasText(sysId) || !hasText(displayName)) {
                    continue;
                }
                if (!displayName.toLowerCase().contains(normalizedQuery)) {
                    continue;
                }
                String incidentNumber = readText(item, "number");
                uniqueResults.putIfAbsent(sysId, new ServiceNowLookupResult(
                        sysId,
                        displayName,
                        null,
                        null,
                        "Used on incident",
                        hasText(incidentNumber) ? incidentNumber : null));
            }
            return new ArrayList<>(uniqueResults.values());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse ServiceNow incident CI lookup response.", ex);
        }
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private String readReferenceValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isObject()) {
            return node.path("value").asText("").trim();
        }
        return node.asText("").trim();
    }

    private String readReferenceDisplayValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isObject()) {
            String displayValue = node.path("display_value").asText("").trim();
            return hasText(displayValue) ? displayValue : node.path("value").asText("").trim();
        }
        return node.asText("").trim();
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (hasText(first)) {
            return first;
        }
        if (hasText(second)) {
            return second;
        }
        return fallback;
    }

    private String normalizeLookupText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .replace("^", "")
                .replace("=", "")
                .replaceAll("\\s{2,}", " ");
    }

    private String normalizeForComparison(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s{2,}", " ").toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isTruthy(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
