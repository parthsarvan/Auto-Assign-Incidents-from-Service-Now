package com.example.backend.controller;

import com.example.backend.dto.SetupStatusResponse;
import com.example.backend.service.SetupStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/setup")
public class SetupController {
    private final SetupStatusService setupStatusService;

    public SetupController(SetupStatusService setupStatusService) {
        this.setupStatusService = setupStatusService;
    }

    @GetMapping("/status")
    public SetupStatusResponse getStatus() {
        return setupStatusService.getStatus();
    }
}
