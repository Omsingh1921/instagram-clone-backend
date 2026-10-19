package com.instagramclone.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            // If secret is hex/base64 encoded
            keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        } catch (IllegalArgumentException e) {
            // Fallback to raw utf-8 bytes
            keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID sid = userPrincipal.getSessionId() != null ? userPrincipal.getSessionId() : UUID.randomUUID();
        return generateAccessToken(authentication, sid);
    }

    public String generateAccessToken(Authentication authentication, UUID sessionId) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateTokenForUser(userPrincipal.getId(), userPrincipal.getUsername(), userPrincipal.getEmail(), sessionId);
    }

    public String generateTokenForUser(UUID userId, String username, String email) {
        return generateTokenForUser(userId, username, email, UUID.randomUUID());
    }

    public String generateTokenForUser(UUID userId, String username, String email, UUID sessionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessExpirationMs());

        var builder = Jwts.builder()
                .subject(username)
                .claim("userId", userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key);

        if (sessionId != null) {
            builder.claim("sid", sessionId.toString());
        }

        return builder.compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object userId = claims.get("userId");
        if (userId instanceof String str) {
            return UUID.fromString(str);
        }
        return UUID.fromString(userId.toString());
    }

    public UUID getSessionIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object sid = claims.get("sid");
            if (sid == null) {
                return null;
            }
            if (sid instanceof String str) {
                return UUID.fromString(str);
            }
            return UUID.fromString(sid.toString());
        } catch (Exception ex) {
            log.debug("Could not parse sid claim from token: {}", ex.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public Date getIssuedAtFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getIssuedAt();
    }

    public Date getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }

    public long getRemainingTtlMs(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            if (expiration != null) {
                return Math.max(0, expiration.getTime() - System.currentTimeMillis());
            }
        } catch (Exception ex) {
            log.debug("Could not calculate remaining TTL for token: {}", ex.getMessage());
        }
        return jwtProperties.getAccessExpirationMs();
    }
}
