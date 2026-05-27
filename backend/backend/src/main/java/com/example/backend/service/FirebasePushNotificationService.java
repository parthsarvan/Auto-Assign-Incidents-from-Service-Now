package com.example.backend.service;

import com.example.backend.dto.IncidentPushPayload;
import com.example.backend.entity.MobileDeviceToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(FirebasePushNotificationService.class);
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String FIREBASE_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final boolean enabled;
    private final String projectId;
    private final String clientEmail;
    private final String privateKeyPem;
    private final String privateKeyPath;
    private final String tokenUri;
    private PrivateKey privateKey;
    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public FirebasePushNotificationService(
            ObjectMapper objectMapper,
            @Value("${inciteam.fcm.enabled:false}") boolean enabled,
            @Value("${inciteam.fcm.project-id:}") String projectId,
            @Value("${inciteam.fcm.client-email:}") String clientEmail,
            @Value("${inciteam.fcm.private-key:}") String privateKeyPem,
            @Value("${inciteam.fcm.private-key-path:}") String privateKeyPath,
            @Value("${inciteam.fcm.token-uri:https://oauth2.googleapis.com/token}") String tokenUri) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.projectId = projectId;
        this.clientEmail = clientEmail;
        this.privateKeyPem = privateKeyPem;
        this.privateKeyPath = privateKeyPath;
        this.tokenUri = tokenUri;
    }

    public void sendIncidentAssigned(MobileDeviceToken token, IncidentPushPayload payload) {
        if (!enabled) {
            logger.debug("FCM is disabled; skipping Android incident assignment notification.");
            return;
        }
        if (!isConfigured()) {
            logger.warn("FCM is enabled but not fully configured; skipping Android notification.");
            return;
        }
        if (token == null || token.getDeviceToken() == null || token.getDeviceToken().isBlank()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send"))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(token, payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn(
                        "FCM rejected notification for user {} with status {}: {}",
                        token.getUser() != null ? token.getUser().getU_id() : null,
                        response.statusCode(),
                        response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("FCM notification was interrupted: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.warn("Failed to send FCM notification: {}", ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return !projectId.isBlank()
                && !clientEmail.isBlank()
                && (!privateKeyPem.isBlank() || !privateKeyPath.isBlank());
    }

    private String buildPayload(MobileDeviceToken token, IncidentPushPayload payload) throws JsonProcessingException {
        String incidentNumber = payload.getIncidentNumber() != null ? payload.getIncidentNumber() : "Incident";
        String title = payload.getTitle() != null && !payload.getTitle().isBlank()
                ? payload.getTitle()
                : "Incident assigned to you";

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", "Incident Assigned");
        notification.put("body", incidentNumber + " assigned to you");

        Map<String, String> data = new LinkedHashMap<>();
        putIfPresent(data, "incidentNumber", payload.getIncidentNumber());
        putIfPresent(data, "ci", payload.getConfigurationItem());
        putIfPresent(data, "priority", payload.getPriority());
        data.put("title", title);

        Map<String, Object> androidNotification = new LinkedHashMap<>();
        androidNotification.put("channel_id", "incident_assignments");
        androidNotification.put("sound", "default");

        Map<String, Object> android = new LinkedHashMap<>();
        android.put("priority", "HIGH");
        android.put("notification", androidNotification);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", token.getDeviceToken());
        message.put("notification", notification);
        message.put("data", data);
        message.put("android", android);

        return objectMapper.writeValueAsString(Map.of("message", message));
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private synchronized String accessToken() throws IOException, GeneralSecurityException, InterruptedException {
        if (accessToken != null && Instant.now().plusSeconds(60).isBefore(accessTokenExpiresAt)) {
            return accessToken;
        }

        String assertion = buildJwtAssertion();
        String body = "grant_type=" + urlEncode("urn:ietf:params:oauth:grant-type:jwt-bearer")
                + "&assertion=" + urlEncode(assertion);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google OAuth token request failed with status " + response.statusCode());
        }

        JsonNode json = objectMapper.readTree(response.body());
        accessToken = json.path("access_token").asText();
        long expiresIn = json.path("expires_in").asLong(3600);
        accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn));
        return accessToken;
    }

    private String buildJwtAssertion() throws GeneralSecurityException, IOException {
        long now = Instant.now().getEpochSecond();
        String header = base64Url(objectMapper.writeValueAsBytes(Map.of(
                "alg", "RS256",
                "typ", "JWT")));
        String claims = base64Url(objectMapper.writeValueAsBytes(Map.of(
                "iss", clientEmail,
                "scope", FIREBASE_SCOPE,
                "aud", tokenUri,
                "iat", now,
                "exp", now + 3600)));
        String signingInput = header + "." + claims;

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(loadPrivateKey());
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + base64Url(signature.sign());
    }

    private PrivateKey loadPrivateKey() throws GeneralSecurityException, IOException {
        if (privateKey != null) {
            return privateKey;
        }
        String pem = privateKeyPem != null && !privateKeyPem.isBlank()
                ? privateKeyPem
                : Files.readString(Path.of(privateKeyPath));
        pem = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        return privateKey;
    }

    private String base64Url(byte[] bytes) {
        return BASE64_URL_ENCODER.encodeToString(bytes);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
