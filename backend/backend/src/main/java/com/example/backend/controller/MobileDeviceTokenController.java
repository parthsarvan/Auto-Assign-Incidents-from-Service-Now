package com.example.backend.controller;

import com.example.backend.dto.MobileDeviceTokenRequest;
import com.example.backend.service.MobileDeviceTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/device-token")
public class MobileDeviceTokenController {
    private final MobileDeviceTokenService tokenService;

    public MobileDeviceTokenController(MobileDeviceTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<?> register(@RequestBody MobileDeviceTokenRequest request) {
        try {
            tokenService.register(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> unregister(@RequestBody MobileDeviceTokenRequest request) {
        tokenService.unregister(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<Void> unregisterWithPost(@RequestBody MobileDeviceTokenRequest request) {
        tokenService.unregister(request);
        return ResponseEntity.noContent().build();
    }
}
