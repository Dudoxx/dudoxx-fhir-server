package ca.uhn.fhir.jpa.starter.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration for the secondary datasource used by TenantRegistryService.
 *
 * This connects to the NestJS database (ddx_clinic_fhir) to read tenant
 * configuration from the organizations, global_config, and partition_sequence tables.
 */
@Configuration
public class TenantDataSourceConfig {

    @Value("${spring.tenant-datasource.url}")
    private String url;

    @Value("${spring.tenant-datasource.username}")
    private String username;

    @Value("${spring.tenant-datasource.password}")
    private String password;

    @Value("${spring.tenant-datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.tenant-datasource.hikari.maximum-pool-size:3}")
    private int maxPoolSize;

    @Value("${spring.tenant-datasource.hikari.minimum-idle:1}")
    private int minIdle;

    @Value("${spring.tenant-datasource.hikari.connection-timeout:10000}")
    private long connectionTimeout;

    @Value("${spring.tenant-datasource.hikari.pool-name:TenantRegistryPool}")
    private String poolName;

    /**
     * Creates the tenant registry datasource for reading from NestJS database.
     */
    @Bean(name = "tenantDataSource")
    public DataSource tenantDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setPoolName(poolName);
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    /**
     * Creates a JdbcTemplate specifically for tenant registry operations.
     */
    @Bean(name = "tenantJdbcTemplate")
    public JdbcTemplate tenantJdbcTemplate() {
        return new JdbcTemplate(tenantDataSource());
    }
}
