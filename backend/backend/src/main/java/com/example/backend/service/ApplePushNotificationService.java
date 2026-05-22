package com.example.backend.service;

import com.example.backend.dto.IncidentPushPayload;
import com.example.backend.entity.MobileDeviceToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApplePushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(ApplePushNotificationService.class);
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final boolean enabled;
    private final String teamId;
    private final String keyId;
    private final String bundleId;
    private final String privateKeyPem;
    private final String privateKeyPath;
    private PrivateKey privateKey;

    public ApplePushNotificationService(
            ObjectMapper objectMapper,
            @Value("${inciteam.apns.enabled:false}") boolean enabled,
            @Value("${inciteam.apns.team-id:}") String teamId,
            @Value("${inciteam.apns.key-id:}") String keyId,
            @Value("${inciteam.apns.bundle-id:}") String bundleId,
            @Value("${inciteam.apns.private-key:}") String privateKeyPem,
            @Value("${inciteam.apns.private-key-path:}") String privateKeyPath) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.teamId = teamId;
        this.keyId = keyId;
        this.bundleId = bundleId;
        this.privateKeyPem = privateKeyPem;
        this.privateKeyPath = privateKeyPath;
    }

    public void sendIncidentAssigned(MobileDeviceToken token, IncidentPushPayload payload) {
        if (!enabled) {
            logger.debug("APNs is disabled; skipping incident assignment notification.");
            return;
        }
        if (!isConfigured()) {
            logger.warn("APNs is enabled but not fully configured; skipping notification.");
            return;
        }
        if (token == null || token.getDeviceToken() == null || token.getDeviceToken().isBlank()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(apnsUri(token))
                    .header("authorization", "bearer " + buildProviderToken())
                    .header("apns-topic", bundleId)
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn(
                        "APNs rejected notification for user {} with status {}: {}",
                        token.getUser() != null ? token.getUser().getU_id() : null,
                        response.statusCode(),
                        response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("APNs notification was interrupted: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.warn("Failed to send APNs notification: {}", ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return !teamId.isBlank()
                && !keyId.isBlank()
                && !bundleId.isBlank()
                && (!privateKeyPem.isBlank() || !privateKeyPath.isBlank());
    }

    private URI apnsUri(MobileDeviceToken token) {
        String host = "production".equalsIgnoreCase(token.getEnvironment())
                ? "https://api.push.apple.com"
                : "https://api.sandbox.push.apple.com";
        return URI.create(host + "/3/device/" + token.getDeviceToken());
    }

    private String buildPayload(IncidentPushPayload payload) throws JsonProcessingException {
        String incidentNumber = payload.getIncidentNumber() != null ? payload.getIncidentNumber() : "Incident";
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("title", "Incident Assigned");
        alert.put("body", incidentNumber + " assigned to you");

        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", alert);
        aps.put("sound", "default");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("aps", aps);
        putIfPresent(root, "incidentNumber", payload.getIncidentNumber());
        putIfPresent(root, "ci", payload.getConfigurationItem());
        putIfPresent(root, "priority", payload.getPriority());
        putIfPresent(root, "title", payload.getTitle());
        return objectMapper.writeValueAsString(root);
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String buildProviderToken() throws GeneralSecurityException, IOException {
        String header = base64Url(objectMapper.writeValueAsBytes(Map.of(
                "alg", "ES256",
                "kid", keyId)));
        String claims = base64Url(objectMapper.writeValueAsBytes(Map.of(
                "iss", teamId,
                "iat", Instant.now().getEpochSecond())));
        String signingInput = header + "." + claims;

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(loadPrivateKey());
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + base64Url(derSignatureToJose(signature.sign()));
    }

    private PrivateKey loadPrivateKey() throws GeneralSecurityException, IOException {
        if (privateKey != null) {
            return privateKey;
        }
        String pem = !privateKeyPem.isBlank() ? privateKeyPem : Files.readString(Path.of(privateKeyPath));
        pem = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        return privateKey;
    }

    private String base64Url(byte[] value) {
        return BASE64_URL_ENCODER.encodeToString(value);
    }

    private byte[] derSignatureToJose(byte[] derSignature) {
        int offset = 0;
        if (derSignature[offset++] != 0x30) {
            throw new IllegalArgumentException("Invalid ECDSA signature.");
        }
        offset = skipDerLength(derSignature, offset);
        if (derSignature[offset++] != 0x02) {
            throw new IllegalArgumentException("Invalid ECDSA signature.");
        }
        int rLength = readDerLength(derSignature, offset);
        offset += derLengthByteCount(derSignature[offset]);
        byte[] r = Arrays.copyOfRange(derSignature, offset, offset + rLength);
        offset += rLength;
        if (derSignature[offset++] != 0x02) {
            throw new IllegalArgumentException("Invalid ECDSA signature.");
        }
        int sLength = readDerLength(derSignature, offset);
        offset += derLengthByteCount(derSignature[offset]);
        byte[] s = Arrays.copyOfRange(derSignature, offset, offset + sLength);

        byte[] jose = new byte[64];
        System.arraycopy(toUnsignedFixedLength(r, 32), 0, jose, 0, 32);
        System.arraycopy(toUnsignedFixedLength(s, 32), 0, jose, 32, 32);
        return jose;
    }

    private int skipDerLength(byte[] bytes, int offset) {
        return offset + derLengthByteCount(bytes[offset]);
    }

    private int readDerLength(byte[] bytes, int offset) {
        int first = bytes[offset] & 0xff;
        if (first < 128) {
            return first;
        }
        int count = first & 0x7f;
        int length = 0;
        for (int index = 1; index <= count; index++) {
            length = (length << 8) + (bytes[offset + index] & 0xff);
        }
        return length;
    }

    private int derLengthByteCount(byte firstLengthByte) {
        int first = firstLengthByte & 0xff;
        return first < 128 ? 1 : 1 + (first & 0x7f);
    }

    private byte[] toUnsignedFixedLength(byte[] value, int length) {
        int offset = 0;
        while (offset < value.length - 1 && value[offset] == 0) {
            offset++;
        }
        int byteCount = value.length - offset;
        if (byteCount > length) {
            throw new IllegalArgumentException("ECDSA signature value is too long.");
        }
        byte[] result = new byte[length];
        System.arraycopy(value, offset, result, length - byteCount, byteCount);
        return result;
    }
}
