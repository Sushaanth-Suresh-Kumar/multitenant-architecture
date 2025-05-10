package dev.sushaanth.bookly.multitenancy.data.hibernate;

import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Component
public class ConnectionProvider implements MultiTenantConnectionProvider, HibernatePropertiesCustomizer {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionProvider.class);
    private static final String DEFAULT_TENANT = "public";

    private final DataSource dataSource;

    public ConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        logger.debug("Get connection for tenant {}", tenantIdentifier);

        final Connection connection = getAnyConnection();

        // Set the schema to the tenant schema or use the default schema if no tenant provided
        if (tenantIdentifier != null && !tenantIdentifier.toString().isEmpty() && !DEFAULT_TENANT.equals(tenantIdentifier.toString())) {
            connection.setSchema(tenantIdentifier.toString());
        }

        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        logger.debug("Release connection for tenant {}", tenantIdentifier);

        // Reset connection to default schema before returning to pool
        if (tenantIdentifier != null && !tenantIdentifier.toString().isEmpty() && !DEFAULT_TENANT.equals(tenantIdentifier.toString())) {
            connection.setSchema(DEFAULT_TENANT);
        }

        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}