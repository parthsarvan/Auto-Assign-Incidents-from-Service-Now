package com.example.backend.service;

import com.example.backend.dto.MobileDeviceTokenRequest;
import com.example.backend.entity.MobileDeviceToken;
import com.example.backend.entity.User;
import com.example.backend.repository.MobileDeviceTokenRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class MobileDeviceTokenService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final MobileDeviceTokenRepository tokenRepository;

    public MobileDeviceTokenService(
            CurrentWorkspaceService currentWorkspaceService,
            MobileDeviceTokenRepository tokenRepository) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.tokenRepository = tokenRepository;
    }

    public MobileDeviceToken register(MobileDeviceTokenRequest request) {
        User user = currentWorkspaceService.getCurrentUser();
        String deviceToken = normalizeDeviceToken(request != null ? request.getDeviceToken() : null);
        if (deviceToken.isBlank()) {
            throw new IllegalArgumentException("Device token is required.");
        }

        String platform = normalizeChoice(request != null ? request.getPlatform() : null, "ios");
        String environment = normalizeChoice(request != null ? request.getEnvironment() : null, "production");
        Instant now = Instant.now();

        MobileDeviceToken token = tokenRepository.findByUserAndDeviceToken(user, deviceToken)
                .orElseGet(MobileDeviceToken::new);
        if (token.getCreatedAt() == null) {
            token.setCreatedAt(now);
        }
        token.setUser(user);
        token.setDeviceToken(deviceToken);
        token.setPlatform(platform);
        token.setEnvironment(environment);
        token.setActive(true);
        token.setUpdatedAt(now);
        token.setLastSeenAt(now);
        return tokenRepository.save(token);
    }

    public void unregister(MobileDeviceTokenRequest request) {
        User user = currentWorkspaceService.getCurrentUser();
        String deviceToken = normalizeDeviceToken(request != null ? request.getDeviceToken() : null);
        if (deviceToken.isBlank()) {
            return;
        }
        tokenRepository.findByUserAndDeviceToken(user, deviceToken)
                .ifPresent(token -> {
                    token.setActive(false);
                    token.setUpdatedAt(Instant.now());
                    tokenRepository.save(token);
                });
    }

    private String normalizeDeviceToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeChoice(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
