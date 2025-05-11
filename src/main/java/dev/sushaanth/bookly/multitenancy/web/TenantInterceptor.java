package dev.sushaanth.bookly.multitenancy.web;

import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.multitenancy.exception.InvalidTenantException;
import dev.sushaanth.bookly.multitenancy.resolver.HttpHeaderTenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    
    // List of valid tenants
    private static final List<String> VALID_TENANTS = Arrays.asList("tenant1", "tenant2");

    private final HttpHeaderTenantResolver httpHeaderTenantResolver;

    public TenantInterceptor(HttpHeaderTenantResolver httpHeaderTenantResolver) {
        this.httpHeaderTenantResolver = httpHeaderTenantResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var tenantId = httpHeaderTenantResolver.resolveTenantId(request);
        
        // Check if tenant header is present
        if (!StringUtils.hasText(tenantId)) {
            logger.warn("Tenant header is missing in the request");
            throw new InvalidTenantException("Tenant header is required");
        }
        
        // Validate that tenant is valid
        if (!VALID_TENANTS.contains(tenantId)) {
            logger.warn("Invalid tenant: {}", tenantId);
            throw new InvalidTenantException("Invalid tenant: " + tenantId);
        }
        
        logger.debug("Request is using tenant: {}", tenantId);
        TenantContext.setTenantId(tenantId);
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
