package com.example.backend.service;

public record ServiceNowConnectionSettings(
        String instanceUrl,
        String username,
        String password) {
}
