package dev.sushaanth.bookly.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TenantContext {
    private static final Logger logger = LoggerFactory.getLogger(TenantContext.class);

    private static final ThreadLocal<String> tenantId = new InheritableThreadLocal<>();

    public static String getTenantId() {
        return tenantId.get();
    }

    public static void setTenantId(String tenant) {
        logger.debug("Setting current tenant to {}", tenant);
        tenantId.set(tenant);
    }

    public static void clear() {
        tenantId.remove();
    }
}
