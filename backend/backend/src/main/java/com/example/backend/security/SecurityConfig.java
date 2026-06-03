package com.example.backend.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${spring.web.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${spring.web.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${spring.web.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ApiRequestSizeLimitFilter apiRequestSizeLimitFilter;

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
              .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
              .requestMatchers(HttpMethod.POST, "/api/auth/organization-discovery").permitAll()
              .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
              .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
              .anyRequest().authenticated()
          )

          .exceptionHandling(exceptions -> exceptions
              .authenticationEntryPoint((request, response, authException) -> {
                  response.setStatus(401);
                  response.setContentType("text/plain;charset=UTF-8");
                  response.getWriter().write("Please sign in to continue.");
              })
          )

          .headers(headers -> headers
              .httpStrictTransportSecurity(hsts -> hsts
                  .includeSubDomains(true)
                  .preload(true)
                  .maxAgeInSeconds(31536000)
              )
              .frameOptions(frameOptions -> frameOptions.deny())
              .contentTypeOptions(contentTypeOptions -> {})
              .referrerPolicy(referrer -> referrer
                  .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
              )
              .contentSecurityPolicy(csp -> csp
                  .policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'")
              )
              .permissionsPolicyHeader(permissions -> permissions
                  .policy("camera=(), geolocation=(), microphone=(), payment=(), usb=()")
              )
          )

          // 5) Reject oversized API requests before auth/controller processing.
          .addFilterBefore(apiRequestSizeLimitFilter, UsernamePasswordAuthenticationFilter.class)

          // 6) Register our custom JwtAuthFilter before the UsernamePasswordAuthenticationFilter
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(parseCsv(allowedOrigins));
        config.setAllowedMethods(parseCsv(allowedMethods));
        config.setAllowedHeaders(parseCsv(allowedHeaders));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setMaxAge(3600L);
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseCsv(String rawValue) {
        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }
}
