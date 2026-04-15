package com.example.backend.controller;

import com.example.backend.dto.ServiceNowHealthResponse;
import com.example.backend.service.ServiceNowHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicenow")
public class ServiceNowHealthController {
    private final ServiceNowHealthService serviceNowHealthService;

    public ServiceNowHealthController(ServiceNowHealthService serviceNowHealthService) {
        this.serviceNowHealthService = serviceNowHealthService;
    }

    @GetMapping("/health")
    public ServiceNowHealthResponse getHealth() {
        return serviceNowHealthService.checkHealth();
    }
}
