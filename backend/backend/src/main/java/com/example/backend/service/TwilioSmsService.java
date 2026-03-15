package com.example.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
public class TwilioSmsService implements SmsService {
    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsService.class);

    private final RestTemplate restTemplate;
    private final boolean enabled;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsService(
            RestTemplate restTemplate,
            @Value("${sms.enabled:false}") boolean enabled,
            @Value("${sms.twilio.account-sid:}") String accountSid,
            @Value("${sms.twilio.auth-token:}") String authToken,
            @Value("${sms.twilio.from-number:}") String fromNumber) {
        this.restTemplate = restTemplate;
        this.enabled = enabled;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public boolean sendSms(String toPhoneNumber, String message) {
        if (!enabled) {
            logger.info("SMS notifications disabled; skipping SMS to {}.", toPhoneNumber);
            return false;
        }
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            logger.warn("SMS settings missing; cannot send SMS to {}.", toPhoneNumber);
            return false;
        }
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            logger.warn("Missing recipient phone number; SMS not sent.");
            return false;
        }

        String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", toPhoneNumber);
        body.add("From", fromNumber);
        body.add("Body", message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", basicAuthHeader(accountSid, authToken));

        try {
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            logger.info("SMS notification sent to {}.", toPhoneNumber);
            return true;
        } catch (RestClientException ex) {
            logger.error("Failed to send SMS to {}: {}", toPhoneNumber, ex.getMessage());
            return false;
        }
    }

    private String basicAuthHeader(String username, String password) {
        String token = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
