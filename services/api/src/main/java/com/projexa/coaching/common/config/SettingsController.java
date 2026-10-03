package com.projexa.coaching.common.config;
import com.projexa.coaching.common.tenant.TenantContextHolder; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/settings") public class SettingsController {private final JdbcTemplate db;public SettingsController(JdbcTemplate db){this.db=db;}
 @GetMapping public Map<String,Object> get(){List<Map<String,Object>> x=db.queryForList("select settings from tenant_settings where tenant_id=?",TenantContextHolder.getRequired());return x.isEmpty()?Map.of(): Map.of("settings", x.get(0).get("settings"));}
 @PutMapping @PreAuthorize("hasRole('INSTITUTE_OWNER') or hasAuthority('settings.manage')") public Map<String,Object> put(@RequestBody Map<String,Object> settings){UUID t=TenantContextHolder.getRequired();db.update("insert into tenant_settings(tenant_id,settings) values(?,cast(? as jsonb)) on conflict(tenant_id) do update set settings=excluded.settings",t,new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(settings).toString());return settings;}
}
