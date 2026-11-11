package com.projexa.coaching.auth;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class PhaseOnePostgresMigrationIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("projexa_phase1")
        .withUsername("projexa")
        .withPassword("integration-test-only");

    @Test
    void allMigrationsIncludingPhaseOneAuthMigrationsApplyToFreshPostgres() {
        Flyway flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(true)
            .load();

        var result = flyway.migrate();

        assertTrue(result.success);
        assertTrue(flyway.info().applied().length >= 35);
        assertEquals("35", flyway.info().current().getVersion().getVersion());

        try (var connection = postgres.createConnection("");
             var statement = connection.createStatement()) {
            assertTrue(tableExists(statement, "institute_signup_verifications"));
            assertTrue(tableExists(statement, "account_invitations"));
            assertTrue(tableExists(statement, "password_reset_tokens"));
            assertTrue(columnExists(statement, "account_invitations", "revoked_at"));
            assertTrue(permissionExists(statement, "users.manage"));
            assertTrue(permissionExists(statement, "roles.manage"));
        } catch (Exception exception) {
            fail("Could not verify Phase 1 schema after Flyway migration", exception);
        }
    }

    private boolean tableExists(java.sql.Statement statement, String table) throws Exception {
        try (var result = statement.executeQuery(
            "select exists(select 1 from information_schema.tables where table_schema='public' and table_name='" + table + "')")) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private boolean columnExists(java.sql.Statement statement, String table, String column) throws Exception {
        try (var result = statement.executeQuery(
            "select exists(select 1 from information_schema.columns where table_schema='public' and table_name='" + table + "' and column_name='" + column + "')")) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private boolean permissionExists(java.sql.Statement statement, String code) throws Exception {
        try (var result = statement.executeQuery(
            "select exists(select 1 from permissions where code='" + code + "')")) {
            result.next();
            return result.getBoolean(1);
        }
    }
}
