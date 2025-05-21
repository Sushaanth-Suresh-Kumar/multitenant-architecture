package dev.sushaanth.bookly.security.jwt;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.security.service.UserDetailsServiceImpl;
import dev.sushaanth.bookly.tenant.TenantRepository;
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
import java.util.*;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth",
            "/api/tenants",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/error"
    );

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

        try {
            final String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                chain.doFilter(request, response);
                return;
            }

            String jwt = authorizationHeader.substring(7);
            if (!jwtTokenUtil.validateToken(jwt)) {
                chain.doFilter(request, response);
                return;
            }

            String username = jwtTokenUtil.getUsernameFromToken(jwt);
            String schemaName = jwtTokenUtil.getSchemaNameFromToken(jwt);
            UUID userId = jwtTokenUtil.getUserIdFromToken(jwt);  // Extract userId from token

            // Set tenant context
            if (schemaName != null) {
                TenantContext.setTenantId(schemaName);
            }

            // Set authentication if not already set
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create Authentication with userId in details
                Map<String, Object> details = new HashMap<>();
                details.put("userId", userId);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(details);  // Store userId in authentication details

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error processing JWT token", e);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}