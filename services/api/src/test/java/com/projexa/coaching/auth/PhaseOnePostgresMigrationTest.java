package com.projexa.coaching.auth;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PhaseOnePostgresMigrationTest {
    @Test
    void allMigrationsIncludingPhaseOneAuthMigrationsApplyToFreshPostgres() throws Exception {
        String url = System.getenv("PHASE1_TEST_DATABASE_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL integration service is configured in CI.");

        String username = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_USERNAME", "projexa");
        String password = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_PASSWORD", "integration-test-only");
        Flyway flyway = Flyway.configure()
            .dataSource(url, username, password)
            .locations("classpath:db/migration")
            .cleanDisabled(true)
            .load();

        var result = flyway.migrate();
        assertTrue(result.success);
        assertTrue(flyway.info().applied().length >= 35);
        assertEquals("35", flyway.info().current().getVersion().getVersion());

        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            assertTrue(exists(statement, "select exists(select 1 from information_schema.tables where table_schema='public' and table_name='institute_signup_verifications')"));
            assertTrue(exists(statement, "select exists(select 1 from information_schema.tables where table_schema='public' and table_name='account_invitations')"));
            assertTrue(exists(statement, "select exists(select 1 from information_schema.tables where table_schema='public' and table_name='password_reset_tokens')"));
            assertTrue(exists(statement, "select exists(select 1 from information_schema.columns where table_schema='public' and table_name='account_invitations' and column_name='revoked_at')"));
            assertTrue(exists(statement, "select exists(select 1 from permissions where code='users.manage')"));
            assertTrue(exists(statement, "select exists(select 1 from permissions where code='roles.manage')"));
        }
    }

    private boolean exists(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getBoolean(1);
        }
    }
}
