package com.projexa.coaching.dashboard.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  private final JdbcTemplate db;
  public DashboardController(JdbcTemplate db){this.db=db;}

  @GetMapping("/summary")
  @PreAuthorize("hasAuthority('dashboard.read')")
  public Map<String,Object> summary(){
    UUID t=TenantContextHolder.getRequired();
    LocalDate today=LocalDate.now();
    Map<String,Object> r=new LinkedHashMap<>();
    r.put("students", number("select count(*) from students where tenant_id=? and status='ACTIVE'",t));
    r.put("attendance", number("select coalesce(round(100.0*sum(case when ar.status='PRESENT' then 1 else 0 end)/nullif(count(ar.id),0),1),0) from attendance_records ar join attendance_sessions s on s.id=ar.session_id where ar.tenant_id=? and s.session_date=?",t,today));
    r.put("collected", number("select coalesce(sum(paid_amount),0) from invoices where tenant_id=?",t));
    r.put("outstanding", number("select coalesce(sum(amount-paid_amount),0) from invoices where tenant_id=? and status in ('UNPAID','PARTIALLY_PAID')",t));
    r.put("overdue", number("select count(*) from invoices where tenant_id=? and status in ('UNPAID','PARTIALLY_PAID') and due_date<?",t,today));
    r.put("batches", number("select count(*) from batches where tenant_id=? and status='ACTIVE'",t));
    r.put("todaySessions", number("select count(*) from attendance_sessions where tenant_id=? and session_date=?",t,today));
    r.put("alerts", number("select count(*) from tickets where tenant_id=? and status in ('OPEN','IN_PROGRESS')",t));
    r.put("recentPayments", db.queryForList("select p.id,p.amount,p.paid_at,i.invoice_number,s.first_name,s.last_name from payments p join invoices i on i.id=p.invoice_id join students s on s.id=i.student_id where p.tenant_id=? and p.status='SUCCESS' order by p.paid_at desc limit 8",t));
    r.put("recentAdmissions", db.queryForList("select id,name,phone,stage,created_at from leads where tenant_id=? order by created_at desc limit 8",t));
    return r;
  }
  private Object number(String sql,Object... args){return db.queryForObject(sql,Object.class,args);}
}
