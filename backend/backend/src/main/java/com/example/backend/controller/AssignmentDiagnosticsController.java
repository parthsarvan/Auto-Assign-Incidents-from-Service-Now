package com.example.backend.controller;

import com.example.backend.dto.AssignmentDiagnosticsResponse;
import com.example.backend.service.AssignmentDiagnosticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicenow")
public class AssignmentDiagnosticsController {
    private final AssignmentDiagnosticsService assignmentDiagnosticsService;

    public AssignmentDiagnosticsController(AssignmentDiagnosticsService assignmentDiagnosticsService) {
        this.assignmentDiagnosticsService = assignmentDiagnosticsService;
    }

    @GetMapping("/assignment-diagnostics")
    public AssignmentDiagnosticsResponse runDiagnostics() {
        return assignmentDiagnosticsService.runDiagnostics();
    }
}
