package com.example.backend.service;

import com.example.backend.dto.ServiceNowHealthResponse;
import com.example.backend.dto.ServiceNowRunLog;
import java.time.Instant;
import java.util.List;
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
public class ServiceNowHealthService {
    private final RestTemplate restTemplate;
    private final ServiceNowAuthHeaderProvider authHeaderProvider;
    private final ServiceNowLogService logService;
    private final String instanceUrl;

    public ServiceNowHealthService(
            RestTemplate restTemplate,
            ServiceNowAuthHeaderProvider authHeaderProvider,
            ServiceNowLogService logService,
            @Value("${servicenow.instance-url}") String instanceUrl) {
        this.restTemplate = restTemplate;
        this.authHeaderProvider = authHeaderProvider;
        this.logService = logService;
        this.instanceUrl = instanceUrl;
    }

    public ServiceNowHealthResponse checkHealth() {
        Instant checkedAt = Instant.now();
        ServiceNowRunLog lastPoll = latestPollLog();

        try {
            HttpHeaders headers = authHeaderProvider.buildHeaders();
            String url = UriComponentsBuilder.fromHttpUrl(instanceUrl)
                    .path("/api/now/table/incident")
                    .queryParam("sysparm_limit", "1")
                    .queryParam("sysparm_fields", "sys_id")
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);

            return new ServiceNowHealthResponse(
                    checkedAt,
                    response.getStatusCode().is2xxSuccessful(),
                    response.getStatusCode().toString(),
                    "Successfully connected to ServiceNow.",
                    instanceUrl,
                    lastPoll != null ? lastPoll.getTimestamp() : null,
                    lastPoll != null ? lastPoll.getStatus() : null,
                    lastPoll != null ? lastPoll.getMessage() : null);
        } catch (HttpStatusCodeException ex) {
            return new ServiceNowHealthResponse(
                    checkedAt,
                    false,
                    ex.getStatusCode().toString(),
                    "ServiceNow responded with an error: " + ex.getResponseBodyAsString(),
                    instanceUrl,
                    lastPoll != null ? lastPoll.getTimestamp() : null,
                    lastPoll != null ? lastPoll.getStatus() : null,
                    lastPoll != null ? lastPoll.getMessage() : null);
        } catch (IllegalStateException | RestClientException ex) {
            return new ServiceNowHealthResponse(
                    checkedAt,
                    false,
                    "ERROR",
                    ex.getMessage(),
                    instanceUrl,
                    lastPoll != null ? lastPoll.getTimestamp() : null,
                    lastPoll != null ? lastPoll.getStatus() : null,
                    lastPoll != null ? lastPoll.getMessage() : null);
        }
    }

    private ServiceNowRunLog latestPollLog() {
        List<ServiceNowRunLog> logs = logService.getLogs();
        return logs.stream()
                .filter(log -> "POLL".equals(log.getType()))
                .findFirst()
                .orElse(null);
    }
}
