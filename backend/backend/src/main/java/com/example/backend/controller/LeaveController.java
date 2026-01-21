package com.example.backend.controller;

import com.example.backend.dto.LeaveRecord;
import com.example.backend.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * LeaveController exposes:
 *   GET /api/leave/next?startDate=YYYY-MM-DD&days=N
 */
@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    @Autowired
    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/next")
    public ResponseEntity<List<LeaveRecord>> getNextLeaves(
      @RequestParam("startDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate startDate,

      @RequestParam("days")
      int days
    ) {
        if (days < 1) {
            days = 1;
        }
        LocalDate endDate = startDate.plusDays(days - 1);
        List<LeaveRecord> leaves = leaveService.getLeaves(startDate, endDate);
        return ResponseEntity.ok(leaves);
    }
}

