package com.projexa.coaching.finance.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.projexa.coaching.common.security.ResourceAccess;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/finance")
public class FinanceController {
  private final JdbcTemplate db; private final ResourceAccess access;
  public FinanceController(JdbcTemplate db,ResourceAccess access){this.db=db;this.access=access;}

  @GetMapping("/invoices")
  @PreAuthorize("hasAnyAuthority('finance.manage','dashboard.read') or hasAnyRole('INSTITUTE_OWNER','INSTITUTE_ADMIN','STUDENT','PARENT')")
  public List<Map<String,Object>> invoices(@RequestParam(required=false) UUID studentId,Authentication auth){
    UUID t=TenantContextHolder.getRequired();
    if(studentId==null&&!access.isStaff(auth))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"studentId is required");
    if(studentId!=null&&!access.canAccessStudent(t,studentId,auth))throw new org.springframework.security.access.AccessDeniedException("Student resource is not accessible");
    if(studentId==null)return db.queryForList("select i.*,s.first_name,s.last_name from invoices i join students s on s.id=i.student_id where i.tenant_id=? order by i.due_date desc",t);
    return db.queryForList("select * from invoices where tenant_id=? and student_id=? order by due_date desc",t,studentId);
  }

  @PostMapping("/invoices")
  @PreAuthorize("hasAuthority('finance.manage') or hasAnyRole('INSTITUTE_OWNER','INSTITUTE_ADMIN')")
  public Map<String,Object> createInvoice(@RequestBody InvoiceRequest req){
    UUID t=TenantContextHolder.getRequired();
    if(req==null||req.studentId()==null)throw new IllegalArgumentException("Student is required");
    if(req.amount()<=0)throw new IllegalArgumentException("Amount must be greater than zero");
    if(req.dueDate()==null)throw new IllegalArgumentException("Due date is required");
    if(db.queryForObject("select count(*) from students where id=? and tenant_id=?",Integer.class,req.studentId(),t)==0)throw new IllegalArgumentException("Student is invalid for this institute");
    if(req.installmentId()!=null&&db.queryForObject("select count(*) from fee_installments fi join fee_plans fp on fp.id=fi.fee_plan_id where fi.id=? and fp.tenant_id=?",Integer.class,req.installmentId(),t)==0)throw new IllegalArgumentException("Installment is invalid for this institute");
    UUID id=UUID.randomUUID();
    String number=req.invoiceNumber()==null||req.invoiceNumber().isBlank()?nextInvoiceNumber(t):req.invoiceNumber();
    db.update("insert into invoices(id,tenant_id,student_id,installment_id,invoice_number,amount,paid_amount,due_date,status) values(?,?,?,?,?,?,0,?,?)",id,t,req.studentId(),req.installmentId(),number,req.amount(),req.dueDate(),"UNPAID");
    return db.queryForMap("select * from invoices where id=? and tenant_id=?",id,t);
  }

  @PostMapping("/payments")
  @PreAuthorize("hasAuthority('finance.manage') or hasAnyRole('INSTITUTE_OWNER','INSTITUTE_ADMIN','STUDENT','PARENT')")
  @org.springframework.transaction.annotation.Transactional
  public Map<String,Object> pay(@RequestBody PaymentRequest req,Authentication auth){
    UUID t=TenantContextHolder.getRequired();
    Map<String,Object> inv=db.queryForMap("select id,student_id,amount,paid_amount,status from invoices where id=? and tenant_id=? for update",req.invoiceId(),t);
    UUID invoiceStudent=(UUID)inv.get("student_id");
    if(!access.isStaff(auth)&&!access.canAccessStudent(t,invoiceStudent,auth))throw new org.springframework.security.access.AccessDeniedException("Invoice is not accessible");
    if(req.idempotencyKey()!=null&&!req.idempotencyKey().isBlank()){
      List<Map<String,Object>> existing=db.queryForList("select id,invoice_id,status from payments where tenant_id=? and idempotency_key=?",t,req.idempotencyKey());
      if(!existing.isEmpty())return Map.of("paymentId",existing.get(0).get("id"),"invoiceId",existing.get(0).get("invoice_id"),"verified","SUCCESS".equals(existing.get(0).get("status")),"idempotent",true);
    }
    double amount=req.amount(); double due=((Number)inv.get("amount")).doubleValue()-((Number)inv.get("paid_amount")).doubleValue();
    if(amount<=0||amount>due)throw new IllegalArgumentException("Invalid payment amount");
    UUID payment=UUID.randomUUID();
    db.update("insert into payments(id,tenant_id,invoice_id,amount,gateway,status,paid_at,idempotency_key) values(?,?,?,?,?,?,?,?)",payment,t,req.invoiceId(),amount,req.gateway()==null?"MANUAL":req.gateway(),"SUCCESS",LocalDateTime.now(),req.idempotencyKey());
    db.update("update invoices set paid_amount=paid_amount+?,status=case when paid_amount+?>=amount then 'PAID' else 'PARTIALLY_PAID' end where id=? and tenant_id=?",amount,amount,req.invoiceId(),t);
    return Map.of("paymentId",payment,"invoiceId",req.invoiceId(),"verified",true,"idempotent",false);
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyAuthority('finance.manage','dashboard.read')")
  public Map<String,Object> summary(){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForMap("select count(*) invoices,coalesce(sum(amount),0) billed,coalesce(sum(paid_amount),0) collected,coalesce(sum(amount-paid_amount),0) outstanding from invoices where tenant_id=?",t);
  }

  private String nextInvoiceNumber(UUID t){
    Long n=db.queryForObject("select count(*)+1 from invoices where tenant_id=?",Long.class,t);
    return "INV-"+LocalDate.now().getYear()+"-"+String.format("%05d",n);
  }
  public record InvoiceRequest(UUID studentId,UUID installmentId,String invoiceNumber,double amount,LocalDate dueDate){}
  public record PaymentRequest(UUID invoiceId,double amount,String gateway,String idempotencyKey){}
}
