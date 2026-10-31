package com.billora.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            if (databaseUrl.startsWith("jdbc:postgresql://")) {
                try {
                    log.info("Detected direct JDBC DATABASE_URL, configuring Hikari pool...");
                    HikariConfig config = new HikariConfig();
                    config.setPoolName("BilloraRenderHikariCP");
                    config.setDriverClassName("org.postgresql.Driver");
                    config.setJdbcUrl(databaseUrl);
                    if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
                        config.setUsername(properties.getUsername());
                    }
                    if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
                        config.setPassword(properties.getPassword());
                    }
                    config.setMaximumPoolSize(10);
                    config.setMinimumIdle(2);
                    config.setIdleTimeout(300000);
                    config.setConnectionTimeout(20000);
                    config.setMaxLifetime(1200000);
                    return new HikariDataSource(config);
                } catch (Exception ex) {
                    log.error("Failed to configure from JDBC DATABASE_URL: {}", ex.getMessage());
                }
            } else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                try {
                    log.info("Detected Render/Cloud DATABASE_URL, configuring Hikari JDBC parameters...");
                URI uri = new URI(databaseUrl);
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String userInfo = uri.getUserInfo();

                String username = properties.getUsername();
                String password = properties.getPassword();

                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8);
                    password = java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
                }

                String query = uri.getQuery();
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null && !query.isBlank() ? "?" + query : "");

                HikariConfig config = new HikariConfig();
                config.setPoolName("BilloraRenderHikariCP");
                config.setDriverClassName("org.postgresql.Driver");
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setIdleTimeout(300000);
                config.setConnectionTimeout(20000);
                config.setMaxLifetime(1200000);

                return new HikariDataSource(config);
            } catch (Exception ex) {
                log.error("Failed to parse DATABASE_URL, falling back to default configuration: {}", ex.getMessage());
            }
        }

        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
