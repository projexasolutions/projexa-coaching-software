package com.projexa.coaching.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoAccountSafetyRunnerTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);

    @Test
    void demoTenantAndSeededAccountsAreDisabledByDefault() throws Exception {
        var runner = new DemoAccountSafetyRunner(db, false);
        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments(new String[0])));
        verify(db).update(contains("update users set status='DISABLED'"));
        verify(db).update(contains("update tenants set status='DISABLED'"));
    }

    @Test
    void explicitlyEnabledLocalDemoAccountsAreNotChanged() throws Exception {
        var runner = new DemoAccountSafetyRunner(db, true);
        runner.run(new DefaultApplicationArguments(new String[0]));
        verifyNoInteractions(db);
    }
}
