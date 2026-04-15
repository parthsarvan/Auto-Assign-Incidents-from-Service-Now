package com.example.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CoverageSummaryResponse {
    private Instant checkedAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalGeoShiftDays;
    private int coveredGeoShiftDays;
    private int gapCount;
    private int ciRiskCount;
    private List<CoverageIssue> issues = new ArrayList<>();

    public CoverageSummaryResponse() {}

    public CoverageSummaryResponse(
            Instant checkedAt,
            LocalDate startDate,
            LocalDate endDate,
            int totalGeoShiftDays,
            int coveredGeoShiftDays,
            int gapCount,
            int ciRiskCount,
            List<CoverageIssue> issues) {
        this.checkedAt = checkedAt;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalGeoShiftDays = totalGeoShiftDays;
        this.coveredGeoShiftDays = coveredGeoShiftDays;
        this.gapCount = gapCount;
        this.ciRiskCount = ciRiskCount;
        this.issues = issues;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
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

    public int getTotalGeoShiftDays() {
        return totalGeoShiftDays;
    }

    public void setTotalGeoShiftDays(int totalGeoShiftDays) {
        this.totalGeoShiftDays = totalGeoShiftDays;
    }

    public int getCoveredGeoShiftDays() {
        return coveredGeoShiftDays;
    }

    public void setCoveredGeoShiftDays(int coveredGeoShiftDays) {
        this.coveredGeoShiftDays = coveredGeoShiftDays;
    }

    public int getGapCount() {
        return gapCount;
    }

    public void setGapCount(int gapCount) {
        this.gapCount = gapCount;
    }

    public int getCiRiskCount() {
        return ciRiskCount;
    }

    public void setCiRiskCount(int ciRiskCount) {
        this.ciRiskCount = ciRiskCount;
    }

    public List<CoverageIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<CoverageIssue> issues) {
        this.issues = issues;
    }
}
