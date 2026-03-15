package com.example.backend.service;

import com.example.backend.dto.ServiceNowOAuthTokenResponse;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ServiceNowOAuthService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowOAuthService.class);
    private static final Duration EXPIRY_SAFETY_BUFFER = Duration.ofMinutes(1);

    private final RestTemplate restTemplate;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;

    private String cachedToken;
    private Instant tokenExpiresAt;

    public ServiceNowOAuthService(
            RestTemplate restTemplate,
            @Value("${servicenow.oauth.token-url}") String tokenUrl,
            @Value("${servicenow.oauth.client-id}") String clientId,
            @Value("${servicenow.oauth.client-secret}") String clientSecret,
            @Value("${servicenow.oauth.username}") String username,
            @Value("${servicenow.oauth.password}") String password) {
        this.restTemplate = restTemplate;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
    }

    public synchronized String getAccessToken() {
        if (cachedToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        ServiceNowOAuthTokenResponse response = requestToken();
        if (response == null || response.getAccessToken() == null) {
            throw new IllegalStateException("Failed to obtain ServiceNow OAuth token.");
        }

        cachedToken = response.getAccessToken();
        tokenExpiresAt = Instant.now()
                .plusSeconds(response.getExpiresIn())
                .minus(EXPIRY_SAFETY_BUFFER);
        logger.info("Fetched ServiceNow OAuth token, expires at {}", tokenExpiresAt);
        return cachedToken;
    }

    private ServiceNowOAuthTokenResponse requestToken() {
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(username) || isBlank(password)) {
            throw new IllegalStateException(
                    "ServiceNow OAuth requires client id/secret and username/password. "
                            + "Provide SERVICENOW_CLIENT_ID, SERVICENOW_CLIENT_SECRET, "
                            + "SERVICENOW_USERNAME, and SERVICENOW_PASSWORD.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        try {
            return restTemplate.postForObject(tokenUrl, request, ServiceNowOAuthTokenResponse.class);
        } catch (RestClientException ex) {
            logger.error("Failed to request ServiceNow OAuth token: {}", ex.getMessage());
            throw ex;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
