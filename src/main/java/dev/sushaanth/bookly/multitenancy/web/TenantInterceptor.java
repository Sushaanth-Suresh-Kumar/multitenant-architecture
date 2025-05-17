package dev.sushaanth.bookly.multitenancy.web;

import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.tenant.exception.InvalidTenantException;
import dev.sushaanth.bookly.multitenancy.resolver.HttpHeaderTenantResolver;
import dev.sushaanth.bookly.tenant.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);

    private final HttpHeaderTenantResolver httpHeaderTenantResolver;
    private final TenantRepository tenantRepository;

    public TenantInterceptor(HttpHeaderTenantResolver httpHeaderTenantResolver, TenantRepository tenantRepository) {
        this.httpHeaderTenantResolver = httpHeaderTenantResolver;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip tenant validation for auth endpoints, tenant management APIs, and error pages
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/auth") ||
                requestPath.startsWith("/api/tenants") ||
                requestPath.startsWith("/swagger-ui") ||
                requestPath.startsWith("/api-docs") ||
                requestPath.startsWith("/v3/api-docs") || // Add this
                requestPath.startsWith("/error")) {
            return true;
        }

        var tenantHeader = httpHeaderTenantResolver.resolveTenantId(request);

        // Check if tenant header is present
        if (!StringUtils.hasText(tenantHeader)) {
            logger.warn("Tenant header is missing in the request");
            throw new InvalidTenantException("Tenant header is required");
        }

        // Find tenant by schema name - avoid logging the tenant identifier
        var tenant = tenantRepository.findBySchemaName(tenantHeader);

        // Validate that tenant exists
        if (tenant.isEmpty()) {
            logger.warn("Invalid tenant specified in request");
            throw new InvalidTenantException("Invalid tenant specified");
        }

        // Use schema name for tenant context - avoid logging sensitive information
        String schemaName = tenant.get().getSchemaName();
        TenantContext.setTenantId(schemaName);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        clear();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        clear();
    }

    private void clear() {
        TenantContext.clear();
    }
}