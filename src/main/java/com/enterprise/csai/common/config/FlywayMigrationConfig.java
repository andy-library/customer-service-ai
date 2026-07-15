package com.enterprise.csai.common.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures Flyway migrations run at startup.
 * Framework database-starter may expose a Flyway bean without Boot's migration initializer.
 */
@Configuration
public class FlywayMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationConfig.class);

    @Bean
    @ConditionalOnBean(Flyway.class)
    ApplicationRunner flywayMigrateRunner(Flyway flyway) {
        return args -> {
            log.info("Running Flyway migrations from {}", String.join(",", flyway.getConfiguration().getLocations()[0].getDescriptor()));
            var result = flyway.migrate();
            log.info("Flyway migrate done: migrationsExecuted={}", result.migrationsExecuted);
        };
    }
}
