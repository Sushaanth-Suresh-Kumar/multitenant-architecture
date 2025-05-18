package dev.sushaanth.bookly.security.jwt;

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

/**
 * Utility class for handling JWT tokens in a multi-tenant system.
 * Uses ES256 (ECDSA with P-256 curve and SHA-256) for digital signatures.
 */
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
        logger.info("JWT key pair generated successfully");
    }

    /**
     * Generates a JWT token containing user details and tenant information.
     *
     * @param username Username of the authenticated user
     * @param tenantId UUID of the tenant the user belongs to
     * @param schemaName Database schema name for the tenant
     * @param role Role of the user
     * @return JWT token as string
     */
    public String generateToken(String username, UUID tenantId, String schemaName, String role) {
        logger.debug("Generating token for user {} with tenant {} and schema {}",
                username, tenantId, schemaName);

        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId.toString())
                .claim("schema", schemaName)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    /**
     * Overloaded method that accepts Role enum directly.
     */
    public String generateToken(String username, UUID tenantId, String schemaName, Role role) {
        return generateToken(username, tenantId, schemaName, role.name());
    }

    /**
     * Extracts username from token.
     *
     * @param token JWT token
     * @return Username
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extracts tenant ID from token.
     *
     * @param token JWT token
     * @return Tenant ID as string
     */
    public String getTenantIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("tenantId", String.class));
    }

    /**
     * Extracts tenant ID as UUID from token.
     *
     * @param token JWT token
     * @return Tenant ID as UUID
     */
    public UUID getTenantUuidFromToken(String token) {
        String tenantId = getTenantIdFromToken(token);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    /**
     * Extracts schema name from token.
     *
     * @param token JWT token
     * @return Schema name
     */
    public String getSchemaNameFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("schema", String.class));
    }

    /**
     * Extracts role from token.
     *
     * @param token JWT token
     * @return Role as string
     */
    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("role", String.class));
    }

    /**
     * Validates a JWT token.
     *
     * @param token JWT token
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (JwtException e) {
            logger.error("Invalid JWT: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error validating JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a token has expired.
     *
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Gets expiration date from token.
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Gets issued date from token.
     *
     * @param token JWT token
     * @return Issued date
     */
    public Date getIssuedAtDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    /**
     * Extracts a claim from token.
     *
     * @param token JWT token
     * @param claimsResolver Function to extract specific claim
     * @return Claim value
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.error("Error extracting claim from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets all claims from token.
     *
     * @param token JWT token
     * @return All claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets the private key used for signing.
     *
     * @return EC private key
     */
    private ECPrivateKey getPrivateKey() {
        return (ECPrivateKey) keyPair.getPrivate();
    }

    /**
     * Gets the public key used for verification.
     *
     * @return EC public key
     */
    private ECPublicKey getPublicKey() {
        return (ECPublicKey) keyPair.getPublic();
    }
}