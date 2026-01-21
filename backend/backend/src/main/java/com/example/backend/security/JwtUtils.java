package com.example.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    // Base64-encoded key from application.properties
    @Value("${jwt.secret}")
    private String jwtSecretBase64;

    @Value("${jwt.expiration-ms}")
    private Long jwtExpirationMs;

    // This will hold the decoded signing key
    private Key signingKey;

    @PostConstruct
    public void init() {
        // Decode the Base64 into raw bytes, then create an HMAC-SHA512 key
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretBase64);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                   .setSubject(username)
                   .claim("role", role)
                   .setIssuedAt(new Date())
                   .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                   .signWith(signingKey, SignatureAlgorithm.HS512)
                   .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(signingKey)
                   .build()
                   .parseClaimsJws(token)
                   .getBody()
                   .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
