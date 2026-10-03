package com.projexa.coaching.common.tenant;

import java.util.UUID;

public final class TenantContextHolder {
    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void set(UUID tenantId) { TENANT.set(tenantId); }
    public static UUID getRequired() {
        UUID tenantId = TENANT.get();
        if (tenantId == null) throw new IllegalStateException("Tenant context not initialized");
        return tenantId;
    }
    public static void clear() { TENANT.remove(); }
}
