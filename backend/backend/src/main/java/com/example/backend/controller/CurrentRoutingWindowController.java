package com.example.backend.controller;

import com.example.backend.dto.CurrentRoutingWindowResponse;
import com.example.backend.service.CurrentRoutingWindowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routing")
public class CurrentRoutingWindowController {
    private final CurrentRoutingWindowService currentRoutingWindowService;

    public CurrentRoutingWindowController(CurrentRoutingWindowService currentRoutingWindowService) {
        this.currentRoutingWindowService = currentRoutingWindowService;
    }

    @GetMapping("/current-window")
    public CurrentRoutingWindowResponse getCurrentWindow() {
        return currentRoutingWindowService.getCurrentWindow();
    }
}
