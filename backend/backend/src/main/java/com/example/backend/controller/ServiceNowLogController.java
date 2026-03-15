package com.example.backend.controller;

import com.example.backend.dto.ServiceNowRunLog;
import com.example.backend.service.ServiceNowLogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs/servicenow")
public class ServiceNowLogController {
    private final ServiceNowLogService logService;

    public ServiceNowLogController(ServiceNowLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public List<ServiceNowRunLog> getLogs() {
        return logService.getLogs();
    }
}
