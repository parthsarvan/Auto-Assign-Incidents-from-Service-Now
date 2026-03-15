package com.example.backend.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Primary
public class MetaWhatsAppService implements SmsService {
    private static final Logger logger = LoggerFactory.getLogger(MetaWhatsAppService.class);

    private final RestTemplate restTemplate;
    private final boolean enabled;
    private final String apiVersion;
    private final String phoneNumberId;
    private final String accessToken;

    public MetaWhatsAppService(
            RestTemplate restTemplate,
            @Value("${whatsapp.enabled:false}") boolean enabled,
            @Value("${whatsapp.api-version:v22.0}") String apiVersion,
            @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${whatsapp.access-token:}") String accessToken) {
        this.restTemplate = restTemplate;
        this.enabled = enabled;
        this.apiVersion = apiVersion;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    @Override
    public boolean sendSms(String toPhoneNumber, String message) {
        if (!enabled) {
            logger.info("WhatsApp notifications disabled; skipping message to {}.", toPhoneNumber);
            return false;
        }
        if (phoneNumberId.isBlank() || accessToken.isBlank()) {
            logger.warn("WhatsApp settings missing; cannot send message to {}.", toPhoneNumber);
            return false;
        }
        String normalizedTo = normalizeNumber(toPhoneNumber);
        if (normalizedTo.isBlank()) {
            logger.warn("Invalid recipient phone number; WhatsApp message not sent.");
            return false;
        }

        String url = String.format("https://graph.facebook.com/%s/%s/messages", apiVersion, phoneNumberId);
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", normalizedTo,
                "type", "text",
                "text", Map.of("preview_url", false, "body", message));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        try {
            restTemplate.postForObject(url, new HttpEntity<>(payload, headers), String.class);
            logger.info("WhatsApp notification sent to {}.", normalizedTo);
            return true;
        } catch (RestClientException ex) {
            logger.error("Failed to send WhatsApp message to {}: {}", normalizedTo, ex.getMessage());
            return false;
        }
    }

    private String normalizeNumber(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }
}
