package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ServiceNowAuthHeaderProvider {
    private final ServiceNowOAuthService oAuthService;
    private final String authMode;
    private final String username;
    private final String password;

    public ServiceNowAuthHeaderProvider(
            ServiceNowOAuthService oAuthService,
            @Value("${servicenow.auth-mode:oauth}") String authMode,
            @Value("${servicenow.oauth.username:}") String username,
            @Value("${servicenow.oauth.password:}") String password) {
        this.oAuthService = oAuthService;
        this.authMode = authMode;
        this.username = username;
        this.password = password;
    }

    public HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if ("basic".equalsIgnoreCase(authMode)) {
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                throw new IllegalStateException("ServiceNow basic auth requires username and password.");
            }
            headers.setBasicAuth(username, password);
        } else {
            String token = oAuthService.getAccessToken();
            headers.setBearerAuth(token);
        }
        headers.set("Accept", "application/json");
        return headers;
    }
}
