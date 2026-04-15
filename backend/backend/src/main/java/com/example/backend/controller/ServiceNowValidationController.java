package com.example.backend.controller;

import com.example.backend.dto.ServiceNowValidationResponse;
import com.example.backend.service.ServiceNowValidationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicenow")
public class ServiceNowValidationController {
    private final ServiceNowValidationService serviceNowValidationService;

    public ServiceNowValidationController(ServiceNowValidationService serviceNowValidationService) {
        this.serviceNowValidationService = serviceNowValidationService;
    }

    @GetMapping("/validation")
    public ServiceNowValidationResponse getValidation() {
        return serviceNowValidationService.validateRecords();
    }
}
