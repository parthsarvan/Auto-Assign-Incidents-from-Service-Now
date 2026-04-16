package com.example.backend.service;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ServiceNowAuthHeaderProvider {
    public HttpHeaders buildHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        applyBasicAuth(headers, username, password);
        headers.set("Accept", "application/json");
        return headers;
    }

    private void applyBasicAuth(HttpHeaders headers, String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("ServiceNow basic auth requires username and password.");
        }
        headers.setBasicAuth(username, password);
    }
}
