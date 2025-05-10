package dev.sushaanth.bookly.multitenancy.resolver;

import org.springframework.lang.NonNull;

@FunctionalInterface
public interface TenantResolver<T> {
    String resolveTenantId(@NonNull T object);
}
