package dev.sushaanth.bookly.multitenancy;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "multitenancy.http")
public record TenantHttpProperties(String headerName) {
}