package com.example.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {
    private static final int MAX_KEY_LENGTH = 140;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final int loginIpLimit;
    private final int loginUserLimit;
    private final int signupIpLimit;
    private final int discoveryIpLimit;
    private final Duration loginWindow;
    private final Duration signupWindow;
    private final Duration discoveryWindow;

    public AuthRateLimiter(
            @Value("${inciteam.security.rate-limit.login.ip-limit:25}") int loginIpLimit,
            @Value("${inciteam.security.rate-limit.login.user-limit:10}") int loginUserLimit,
            @Value("${inciteam.security.rate-limit.login.window-seconds:900}") long loginWindowSeconds,
            @Value("${inciteam.security.rate-limit.signup.ip-limit:8}") int signupIpLimit,
            @Value("${inciteam.security.rate-limit.signup.window-seconds:3600}") long signupWindowSeconds,
            @Value("${inciteam.security.rate-limit.discovery.ip-limit:30}") int discoveryIpLimit,
            @Value("${inciteam.security.rate-limit.discovery.window-seconds:900}") long discoveryWindowSeconds) {
        this.loginIpLimit = loginIpLimit;
        this.loginUserLimit = loginUserLimit;
        this.signupIpLimit = signupIpLimit;
        this.discoveryIpLimit = discoveryIpLimit;
        this.loginWindow = Duration.ofSeconds(loginWindowSeconds);
        this.signupWindow = Duration.ofSeconds(signupWindowSeconds);
        this.discoveryWindow = Duration.ofSeconds(discoveryWindowSeconds);
    }

    public RateLimitResult consumeLoginAttempt(String ipAddress, String username) {
        RateLimitResult ipResult = consume("login:ip:" + normalizeKey(ipAddress), loginIpLimit, loginWindow);
        if (!ipResult.allowed()) {
            return ipResult;
        }
        return consume("login:user:" + normalizeKey(username), loginUserLimit, loginWindow);
    }

    public RateLimitResult consumeSignupAttempt(String ipAddress) {
        return consume("signup:ip:" + normalizeKey(ipAddress), signupIpLimit, signupWindow);
    }

    public RateLimitResult consumeDiscoveryAttempt(String ipAddress) {
        return consume("discovery:ip:" + normalizeKey(ipAddress), discoveryIpLimit, discoveryWindow);
    }

    public void clearLoginAttempts(String ipAddress, String username) {
        counters.remove("login:user:" + normalizeKey(username));
    }

    private RateLimitResult consume(String key, int limit, Duration window) {
        if (limit <= 0) {
            return RateLimitResult.permitted();
        }

        long now = Instant.now().toEpochMilli();
        long windowMillis = Math.max(window.toMillis(), 1000);
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now + windowMillis));

        synchronized (counter) {
            if (now >= counter.resetAtMillis) {
                counter.count = 0;
                counter.resetAtMillis = now + windowMillis;
            }
            if (counter.count >= limit) {
                return RateLimitResult.blocked(Math.max(1, (counter.resetAtMillis - now + 999) / 1000));
            }
            counter.count++;
            return RateLimitResult.permitted();
        }
    }

    private String normalizeKey(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            return "unknown";
        }
        String normalized = rawKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() <= MAX_KEY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_KEY_LENGTH);
    }

    private static class WindowCounter {
        private int count;
        private long resetAtMillis;

        private WindowCounter(long resetAtMillis) {
            this.resetAtMillis = resetAtMillis;
        }
    }

    public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
        public static RateLimitResult permitted() {
            return new RateLimitResult(true, 0);
        }

        public static RateLimitResult blocked(long retryAfterSeconds) {
            return new RateLimitResult(false, retryAfterSeconds);
        }
    }
}
