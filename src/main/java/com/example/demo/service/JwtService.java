package com.example.demo.service;

import com.example.demo.model.CustomUserDetail;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {
    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}")
    private long expirationTime; // 24 hours default

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractId(String token) {
        return extractClaim(token, claims -> claims.get("id", Long.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof CustomUserDetail) {
            CustomUserDetail customUserDetail = (CustomUserDetail) userDetails;
            claims.put("id", customUserDetail.getId());
            claims.put("role", customUserDetail.getRole().name());
            claims.put("fullName", customUserDetail.getFullName());
            
            log.info("Generating token for user: {}, ID: {}, Role: {}", 
                     customUserDetail.getUsername(), 
                     customUserDetail.getId(), 
                     customUserDetail.getRole().name());
        }
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            
            if (userDetails instanceof CustomUserDetail) {
                Long tokenId = extractId(token);
                isValid = isValid && tokenId != null && tokenId.equals(((CustomUserDetail) userDetails).getId());
            }
            
            if (!isValid) {
                log.warn("Token validation failed for user: {}", userDetails.getUsername());
                if (isTokenExpired(token)) {
                    log.warn("Token has expired");
                }
            } else {
                log.debug("Token successfully validated for user: {}", userDetails.getUsername());
            }
            
            return isValid;
        } catch (JwtException e) {
            // Log token validation errors without exposing details
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        Date now = new Date();
        boolean isExpired = expiration.before(now);
        if (isExpired) {
            long diffInMillis = now.getTime() - expiration.getTime();
            long diffInMinutes = diffInMillis / (60 * 1000);
            log.debug("Token expired {} minutes ago", diffInMinutes);
        }
        return isExpired;
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 