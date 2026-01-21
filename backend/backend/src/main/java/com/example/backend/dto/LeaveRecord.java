package com.example.backend.dto;

import java.time.LocalDateTime;

/**
 * DTO for leave entries. Now includes fullName instead of separate f_name/l_name.
 */
public class LeaveRecord {
    private String fullName;
    private String geoName;
    private String shiftName;
    private LocalDateTime startTs;
    private LocalDateTime endTs;
    private String reason;

    public LeaveRecord(
        String fullName,
        String geoName,
        String shiftName,
        LocalDateTime startTs,
        LocalDateTime endTs,
        String reason
    ) {
        this.fullName  = fullName;
        this.geoName   = geoName;
        this.shiftName = shiftName;
        this.startTs   = startTs;
        this.endTs     = endTs;
        this.reason    = reason;
    }

    // Getters & setters:
    public String getFullName()           { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGeoName()            { return geoName; }
    public void setGeoName(String geoName) { this.geoName = geoName; }

    public String getShiftName()          { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public LocalDateTime getStartTs()       { return startTs; }
    public void setStartTs(LocalDateTime startTs) { this.startTs = startTs; }

    public LocalDateTime getEndTs()         { return endTs; }
    public void setEndTs(LocalDateTime endTs)   { this.endTs = endTs; }

    public String getReason()             { return reason; }
    public void setReason(String reason)   { this.reason = reason; }
}
