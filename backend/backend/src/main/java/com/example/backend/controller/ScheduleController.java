package com.example.backend.controller;

import com.example.backend.dto.AvailabilityRecord;
import com.example.backend.service.AvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
//import java.util.Map;

/**
 * This controller provides an endpoint:
 *   GET /api/schedule/next
 * taking two query parameters:
 *   - startDate (ISO format YYYY-MM-DD)
 *   - days      (integer, usually 7 or 1)
 *
 * It returns a JSON array of AvailabilityRecord for all schedules
 * whose date is between startDate and startDate+days-1.
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final AvailabilityService availabilityService;

    @Autowired
    public ScheduleController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Example URL:
     *   GET /api/schedule/next?startDate=2025-06-02&days=7
     *
     * @param startDate  LocalDate, parsed from ISO string (yyyy-MM-dd)
     * @param days       number of days (1 or 7)
     * @return List of AvailabilityRecord
     */
    @GetMapping("/next")
    public ResponseEntity<List<AvailabilityRecord>> getNext(
      @RequestParam("startDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate startDate,

      @RequestParam("days")
      int days
    ) {
        if (days < 1) {
            days = 1;
        }
        // Compute endDate = startDate.plusDays(days - 1)
        LocalDate endDate = startDate.plusDays(days - 1);

        List<AvailabilityRecord> records = availabilityService.getAvailability(startDate, endDate);
        return ResponseEntity.ok(records);
    }
}
