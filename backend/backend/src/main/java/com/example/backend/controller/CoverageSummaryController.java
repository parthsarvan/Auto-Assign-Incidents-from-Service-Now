package com.example.backend.controller;

import com.example.backend.dto.CoverageSummaryResponse;
import com.example.backend.service.CoverageSummaryService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coverage")
public class CoverageSummaryController {
    private final CoverageSummaryService coverageSummaryService;

    public CoverageSummaryController(CoverageSummaryService coverageSummaryService) {
        this.coverageSummaryService = coverageSummaryService;
    }

    @GetMapping("/summary")
    public CoverageSummaryResponse getSummary(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(value = "days", defaultValue = "7") int days) {
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        return coverageSummaryService.buildSummary(effectiveStartDate, days);
    }
}
