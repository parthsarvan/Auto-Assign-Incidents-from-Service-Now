package com.example.backend.dto;

import java.time.LocalDate;
import java.util.List;

public class TeamMemberScheduleRequest {
    private Long teamMemberId;
    private List<Long> teamMemberIds;
    private Long geoId;
    private Long shiftId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> coverageDays;

    public Long getTeamMemberId() {
        return teamMemberId;
    }

    public void setTeamMemberId(Long teamMemberId) {
        this.teamMemberId = teamMemberId;
    }

    public List<Long> getTeamMemberIds() {
        return teamMemberIds;
    }

    public void setTeamMemberIds(List<Long> teamMemberIds) {
        this.teamMemberIds = teamMemberIds;
    }

    public Long getGeoId() {
        return geoId;
    }

    public void setGeoId(Long geoId) {
        this.geoId = geoId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<String> getCoverageDays() {
        return coverageDays;
    }

    public void setCoverageDays(List<String> coverageDays) {
        this.coverageDays = coverageDays;
    }
}
