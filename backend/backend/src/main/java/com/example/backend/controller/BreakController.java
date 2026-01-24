package com.example.backend.controller;

import com.example.backend.dto.BreakRecord;
import com.example.backend.service.BreakService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/break")
public class BreakController {

    private final BreakService breakService;

    public BreakController(BreakService breakService) {
        this.breakService = breakService;
    }

    @GetMapping("/next")
    public ResponseEntity<List<BreakRecord>> getNextBreaks(
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
        List<BreakRecord> breaks = breakService.getBreaks(startDate, endDate);
        return ResponseEntity.ok(breaks);
    }
}
