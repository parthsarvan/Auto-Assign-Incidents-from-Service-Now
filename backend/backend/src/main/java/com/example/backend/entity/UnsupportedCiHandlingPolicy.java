package com.example.backend.entity;

public enum UnsupportedCiHandlingPolicy {
    ROUTE_TO_CI_OWNER,
    FALLBACK_TRIAGE_OWNER,
    SKIP_AND_LOG
}
