package dev.sushaanth.bookly.security;

import dev.sushaanth.bookly.security.model.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtTokenUtil implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    private KeyPair keyPair;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Override
    public void afterPropertiesSet() {
        // Generate ES256 key pair (ECDSA using P-256 curve and SHA-256)
        this.keyPair = Keys.keyPairFor(io.jsonwebtoken.SignatureAlgorithm.ES256);
    }

    public String generateToken(String username, UUID tenantId, Role role) {
        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId.toString())
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getTenantIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("tenantId", String.class));
    }

    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("role", String.class));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getPublicKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            logger.error("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private ECPrivateKey getPrivateKey() {
        return (ECPrivateKey) keyPair.getPrivate();
    }

    private ECPublicKey getPublicKey() {
        return (ECPublicKey) keyPair.getPublic();
    }
}