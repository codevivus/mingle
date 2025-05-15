package com.mingle.a2srv_service.security;

import com.mingle.a2srv_service.exception.type.TokenValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long expirationMillis;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    public String resolveToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new TokenValidationException("Missing or invalid Bearer token");
        }
        return bearerToken.substring(7);
    }

    public Jws<Claims> validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenValidationException("Invalid or expired token: " + ex.getMessage());
        }
    }

    public String getUserIdFromToken(String token) {
        return validateToken(token).getBody().getSubject();
    }

    public long getRemainingValidity(String token) {
        Date expiration = validateToken(token).getBody().getExpiration();
        return (expiration.getTime() - System.currentTimeMillis()) / 1000;
    }

}
