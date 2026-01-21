package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {
        if (userRepo.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.status(409).body("Error: Username is already taken!");
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole("User");                     // default role
        user.setCreated_at(Instant.now());
        user.setCreated_by(null);

        userRepo.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest authRequest) {
        return userRepo.findByUsername(authRequest.getUsername())
            .map(user -> {
                if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
                    return ResponseEntity.status(401).body("Error: Invalid credentials");
                }
                String jwt = jwtUtils.generateToken(user.getUsername(), user.getRole());
                AuthResponse resp = new AuthResponse(
                    jwt,
                    user.getU_id(),
                    user.getUsername(),
                    user.getRole()
                );
                return ResponseEntity.ok(resp);
            })
            .orElseGet(() -> ResponseEntity.status(404).body("Error: User not found"));
    }
}
