package com.projexa.coaching.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The historical Flyway seed migrations create a predictable demo institute.
 * Production deployments must opt in explicitly before that tenant can be used.
 * Demo rows are retained for development recovery; this guard only disables access.
 */
@Component
public class DemoAccountSafetyRunner implements ApplicationRunner {
    private final JdbcTemplate db;
    private final boolean demoAccountsEnabled;

    public DemoAccountSafetyRunner(JdbcTemplate db,
            @Value("${app.demo-accounts.enabled:false}") boolean demoAccountsEnabled) {
        this.db = db;
        this.demoAccountsEnabled = demoAccountsEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoAccountsEnabled) return;
        db.update("""
            update users set status='DISABLED',updated_at=now()
            where tenant_id=(select id from tenants where slug='demo')
              and email in ('owner@demo.projexa.local','student@demo.projexa.local')
              and status='ACTIVE'
            """);
        db.update("""
            update tenants set status='DISABLED',updated_at=now()
            where slug='demo' and status='ACTIVE'
            """);
    }
}
