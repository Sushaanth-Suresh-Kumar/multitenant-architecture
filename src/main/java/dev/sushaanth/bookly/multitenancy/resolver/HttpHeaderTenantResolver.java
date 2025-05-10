package dev.sushaanth.bookly.multitenancy.resolver;

import dev.sushaanth.bookly.multitenancy.TenantHttpProperties;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderTenantResolver implements TenantResolver<HttpServletRequest>{

    private final TenantHttpProperties tenantHttpProperties;

    public HttpHeaderTenantResolver(TenantHttpProperties tenantHttpProperties) {
        this.tenantHttpProperties = tenantHttpProperties;
    }

    @Override
    public String resolveTenantId(HttpServletRequest request) {
        return request.getHeader(tenantHttpProperties.headerName());
    }

}
