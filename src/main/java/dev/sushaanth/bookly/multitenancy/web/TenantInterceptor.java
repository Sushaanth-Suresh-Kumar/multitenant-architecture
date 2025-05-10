package dev.sushaanth.bookly.multitenancy.web;

import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.multitenancy.resolver.HttpHeaderTenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final HttpHeaderTenantResolver httpHeaderTenantResolver;

    public TenantInterceptor(HttpHeaderTenantResolver httpHeaderTenantResolver) {
        this.httpHeaderTenantResolver = httpHeaderTenantResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var tenantId = httpHeaderTenantResolver.resolveTenantId(request);
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
