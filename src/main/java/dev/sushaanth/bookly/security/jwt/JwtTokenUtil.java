package dev.sushaanth.bookly.security.jwt;

import dev.sushaanth.bookly.security.model.Role;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationTime;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    public String generateToken(String username, UUID tenantId, String schemaName, Role role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId.toString());
        claims.put("schema", schemaName);
        claims.put("role", role.getAuthority());

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            logger.error("Error parsing JWT: {}", e.getMessage());
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getSchemaNameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims != null ? claims.get("schema", String.class) : null;
    }

    public UUID getTenantIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String tenantId = claims != null ? claims.get("tenantId", String.class) : null;
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    public Role getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String roleString = claims != null ? claims.get("role", String.class) : null;
        return roleString != null ? Role.valueOf(roleString) : null;
    }

    public boolean validateToken(String token) {
        return getAllClaimsFromToken(token) != null;
    }
}