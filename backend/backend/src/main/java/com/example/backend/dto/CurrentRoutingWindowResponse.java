package com.example.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class CurrentRoutingWindowResponse {
    private String status;
    private String message;
    private String timezone;
    private String teamLocalDateTime;
    private boolean hasActiveWindow;
    private List<CurrentRoutingWindowItem> activeWindows = new ArrayList<>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTeamLocalDateTime() {
        return teamLocalDateTime;
    }

    public void setTeamLocalDateTime(String teamLocalDateTime) {
        this.teamLocalDateTime = teamLocalDateTime;
    }

    public boolean isHasActiveWindow() {
        return hasActiveWindow;
    }

    public void setHasActiveWindow(boolean hasActiveWindow) {
        this.hasActiveWindow = hasActiveWindow;
    }

    public List<CurrentRoutingWindowItem> getActiveWindows() {
        return activeWindows;
    }

    public void setActiveWindows(List<CurrentRoutingWindowItem> activeWindows) {
        this.activeWindows = activeWindows;
    }
}
