package com.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ServiceNowAuthHeaderProvider {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNowAuthHeaderProvider.class);

    private final ServiceNowOAuthService oAuthService;
    private final String authMode;
    private final String username;
    private final String password;
    private final String clientId;
    private final String clientSecret;

    public ServiceNowAuthHeaderProvider(
            ServiceNowOAuthService oAuthService,
            @Value("${servicenow.auth-mode:oauth}") String authMode,
            @Value("${servicenow.oauth.username:}") String username,
            @Value("${servicenow.oauth.password:}") String password,
            @Value("${servicenow.oauth.client-id:}") String clientId,
            @Value("${servicenow.oauth.client-secret:}") String clientSecret) {
        this.oAuthService = oAuthService;
        this.authMode = authMode;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if ("basic".equalsIgnoreCase(authMode)) {
            applyBasicAuth(headers);
        } else if ("oauth".equalsIgnoreCase(authMode)) {
            if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
                if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                    logger.warn("ServiceNow OAuth client credentials missing; falling back to basic auth.");
                    applyBasicAuth(headers);
                } else {
                    throw new IllegalStateException(
                            "ServiceNow OAuth requires client id/secret or basic auth username/password.");
                }
            } else {
                String token = oAuthService.getAccessToken();
                headers.setBearerAuth(token);
            }
        } else {
            String token = oAuthService.getAccessToken();
            headers.setBearerAuth(token);
        }
        headers.set("Accept", "application/json");
        return headers;
    }

    private void applyBasicAuth(HttpHeaders headers) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("ServiceNow basic auth requires username and password.");
        }
        headers.setBasicAuth(username, password);
    }
}
