package dev.sushaanth.bookly.security.jwt;

import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.security.service.UserDetailsServiceImpl;
import dev.sushaanth.bookly.tenant.TenantRepository;
import dev.sushaanth.bookly.tenant.exception.InvalidTenantException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final TenantRepository tenantRepository;

    public JwtRequestFilter(UserDetailsServiceImpl userDetailsService,
                            JwtTokenUtil jwtTokenUtil,
                            TenantRepository tenantRepository) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Skip filter for public endpoints
        if (isPublicEndpoint(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            final String authorizationHeader = request.getHeader("Authorization");

            String username = null;
            String jwt = null;
            String schemaName = null;

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);

                if (jwtTokenUtil.validateToken(jwt)) {
                    username = jwtTokenUtil.getUsernameFromToken(jwt);
                    schemaName = jwtTokenUtil.getSchemaNameFromToken(jwt);

                    // Validate that the tenant/schema exists
                    if (schemaName != null && !schemaName.equals("public")) {
                        boolean tenantExists = tenantRepository.existsBySchemaName(schemaName);
                        if (!tenantExists) {
                            throw new InvalidTenantException("Invalid tenant specified in token");
                        }
                    }
                }
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Set tenant context based on token
                if (schemaName != null) {
                    TenantContext.setTenantId(schemaName);
                    logger.debug("Set tenant context to {}", schemaName);
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            chain.doFilter(request, response);
        } catch (InvalidTenantException e) {
            logger.error("Invalid tenant in JWT token", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid tenant specified");
        } catch (Exception e) {
            logger.error("Error processing JWT token", e);
            chain.doFilter(request, response);
        } finally {
            // Always clear tenant context after request completes
            TenantContext.clear();
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth") ||
                path.startsWith("/api/tenants") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/error");
    }
}