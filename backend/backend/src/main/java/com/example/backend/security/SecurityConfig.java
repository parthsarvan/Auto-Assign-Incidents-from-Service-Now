package com.example.backend.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          // 1) Disable CSRF (we’re using JWTs, not cookies)
          .csrf(csrf -> csrf.disable())

          // 2) Configure CORS by pointing to our CorsConfigurationSource bean
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))

          // 3) Make session management stateless (no HTTP session; use JWT)
          .sessionManagement(session ->
              session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          )

          // 4) Public endpoints: allow POST to /api/auth/**
          .authorizeHttpRequests(auth -> auth
              .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
              .anyRequest().authenticated()
          )

          // 5) Register our custom JwtAuthFilter before the UsernamePasswordAuthenticationFilter
          .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines CORS rules for the entire application.
     * In this example, we allow http://localhost:3000 (React) to call any endpoint.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1) Which origins are allowed?
        config.setAllowedOrigins(List.of("http://localhost:3000"));

        // 2) Which HTTP methods are allowed?
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3) Which headers can the client send?
        config.setAllowedHeaders(List.of("*"));

        // 4) Whether to allow credentials (e.g. cookies). For JWT use, usually false.
        config.setAllowCredentials(true);

        // 5) Apply this configuration to all endpoints (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
