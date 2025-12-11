package ca.uhn.fhir.jpa.starter.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Primary DataSource configuration for HAPI FHIR JPA Server.
 *
 * This explicitly configures PostgreSQL as the primary datasource,
 * overriding Spring Boot's H2 auto-configuration.
 *
 * Environment variables are loaded via spring-dotenv from .env file.
 */
@Configuration
public class FhirDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(FhirDataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.pool-name:HapiFhirHikariPool}")
    private String poolName;

    /**
     * Creates the primary datasource for HAPI FHIR JPA persistence.
     *
     * This bean is marked @Primary to ensure it's used by Hibernate/JPA
     * instead of H2 auto-configuration.
     */
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        logger.info("=== Configuring Primary FHIR DataSource ===");
        logger.info("JDBC URL: {}", url);
        logger.info("Username: {}", username);
        logger.info("Driver: {}", driverClassName);
        logger.info("Pool Name: {}", poolName);
        logger.info("Max Pool Size: {}", maxPoolSize);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setPoolName(poolName);
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        config.setRegisterMbeans(true);

        HikariDataSource dataSource = new HikariDataSource(config);
        logger.info("=== Primary FHIR DataSource configured successfully ===");
        return dataSource;
    }
}
