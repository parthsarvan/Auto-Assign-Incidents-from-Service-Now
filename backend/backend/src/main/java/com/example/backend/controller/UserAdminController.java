package com.example.backend.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.dto.UserRoleUpdateRequest;
import com.example.backend.dto.UserSummary;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserAdminController {

    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserSummary> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> new UserSummary(user.getU_id(), user.getUsername(), user.getRole()))
            .toList();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(
        @PathVariable Long id,
        @RequestBody UserRoleUpdateRequest request
    ) {
        if (request == null || request.getRole() == null || request.getRole().isBlank()) {
            return ResponseEntity.badRequest().body("Role is required.");
        }

        String normalizedRole = normalizeRole(request.getRole());
        if (normalizedRole == null) {
            return ResponseEntity.badRequest().body("Role must be User or Admin.");
        }

        return userRepository.findById(id)
            .map(user -> {
                user.setRole(normalizedRole);
                userRepository.save(user);
                return ResponseEntity.ok(
                    new UserSummary(user.getU_id(), user.getUsername(), user.getRole())
                );
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String normalizeRole(String input) {
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        return switch (trimmed) {
            case "admin" -> "Admin";
            case "user" -> "User";
            default -> null;
        };
    }
}
