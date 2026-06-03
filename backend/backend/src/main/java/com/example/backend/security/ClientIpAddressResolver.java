package com.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class ClientIpAddressResolver {
    private static final int MAX_ADDRESS_LENGTH = 64;

    public String resolve(HttpServletRequest request) {
        String forwardedFor = firstForwardedAddress(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor;
        }

        String realIp = sanitizeAddress(request.getHeader("X-Real-IP"));
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }

        String remoteAddress = sanitizeAddress(request.getRemoteAddr());
        return StringUtils.hasText(remoteAddress) ? remoteAddress : "unknown";
    }

    private String firstForwardedAddress(String rawHeader) {
        if (!StringUtils.hasText(rawHeader)) {
            return "";
        }
        return sanitizeAddress(rawHeader.split(",", 2)[0]);
    }

    private String sanitizeAddress(String rawAddress) {
        if (!StringUtils.hasText(rawAddress)) {
            return "";
        }

        String address = rawAddress.trim();
        if (address.length() > MAX_ADDRESS_LENGTH) {
            return "";
        }
        if (!address.matches("[A-Fa-f0-9:.\\-_%]+")) {
            return "";
        }
        return address.toLowerCase(Locale.ROOT);
    }
}
