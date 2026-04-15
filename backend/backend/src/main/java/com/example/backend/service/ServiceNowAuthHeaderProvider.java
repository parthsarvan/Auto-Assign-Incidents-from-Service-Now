package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ServiceNowAuthHeaderProvider {
    private final String username;
    private final String password;

    public ServiceNowAuthHeaderProvider(
            @Value("${servicenow.username:}") String username,
            @Value("${servicenow.password:}") String password) {
        this.username = username;
        this.password = password;
    }

    public HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        applyBasicAuth(headers);
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
