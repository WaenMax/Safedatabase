package com.example.datasecurity.controller;

import com.example.datasecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WorkflowController {
    private final JdbcTemplate jdbc;
    private final AuthService auth;

    @GetMapping("/api/access-requests")
    public Object requests() {
        return jdbc.queryForList("""
                select ar.*, u.username, f.field_name, l.level_code
                from access_request ar
                join sys_user u on u.id=ar.user_id
                join data_field_asset f on f.id=ar.field_id
                left join field_classification fc on fc.field_id=f.id
                left join classification_level l on l.id=fc.level_id
                order by ar.id desc
                """);
    }

    @PostMapping("/api/access-requests")
    public Object create(@RequestBody Map<String, Object> b, HttpServletRequest request) {
        Long userId = auth.currentUserId(request);
        if (userId == null && b.get("user_id") != null) userId = num(b.get("user_id"));
        jdbc.update("insert into access_request(user_id,field_id,reason,status,request_time,valid_until) values(?,?,?,?,?,?)",
                userId, b.get("field_id"), b.get("reason"), "PENDING", LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        auth.audit(userId, "CREATE_ACCESS_REQUEST", "data_field_asset", num(b.get("field_id")), auth.ip(request), "SUCCESS", "提交访问申请");
        return Map.of("success", true);
    }

    @PutMapping("/api/access-requests/{id}/approve")
    public Object approve(@PathVariable Long id, HttpServletRequest request) {
        jdbc.update("update access_request set status='APPROVED', valid_until=? where id=?", LocalDateTime.now().plusDays(7), id);
        jdbc.update("insert into approval_record(request_id,approver_id,approval_result,approval_comment,approval_time) values(?,?,?,?,?)",
                id, auth.currentUserId(request), "APPROVED", "审批通过", LocalDateTime.now());
        auth.audit(auth.currentUserId(request), "APPROVE_REQUEST", "access_request", id, auth.ip(request), "SUCCESS", "审批通过");
        return Map.of("success", true);
    }

    @PutMapping("/api/access-requests/{id}/reject")
    public Object reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        jdbc.update("update access_request set status='REJECTED' where id=?", id);
        jdbc.update("insert into approval_record(request_id,approver_id,approval_result,approval_comment,approval_time) values(?,?,?,?,?)",
                id, auth.currentUserId(request), "REJECTED", body == null ? "审批驳回" : body.getOrDefault("comment", "审批驳回"), LocalDateTime.now());
        auth.audit(auth.currentUserId(request), "REJECT_REQUEST", "access_request", id, auth.ip(request), "SUCCESS", "审批驳回");
        return Map.of("success", true);
    }

    @GetMapping("/api/audit-logs")
    public Object auditLogs() {
        return jdbc.queryForList("""
                select a.*, u.username
                from audit_log a left join sys_user u on u.id=a.user_id
                order by a.operation_time desc, a.id desc
                """);
    }

    @GetMapping("/api/dashboard/summary")
    public Object dashboard() {
        return Map.of(
                "sourceCount", jdbc.queryForObject("select count(*) from data_source", Integer.class),
                "tableCount", jdbc.queryForObject("select count(*) from data_table_asset", Integer.class),
                "fieldCount", jdbc.queryForObject("select count(*) from data_field_asset", Integer.class),
                "sensitiveFieldCount", jdbc.queryForObject("select count(*) from data_field_asset where is_sensitive=1", Integer.class),
                "levelStats", jdbc.queryForList("""
                        select l.level_code name, count(fc.id) cnt from classification_level l
                        left join field_classification fc on fc.level_id=l.id group by l.level_code, l.level_order order by l.level_order
                        """),
                "categoryStats", jdbc.queryForList("""
                        select c.category_name name, count(fc.id) cnt from classification_category c
                        left join field_classification fc on fc.category_id=c.id group by c.category_name order by c.category_name
                        """),
                "recentAuditLogs", jdbc.queryForList("""
                        select a.*, u.username from audit_log a left join sys_user u on u.id=a.user_id
                        order by a.operation_time desc, a.id desc limit 8
                        """)
        );
    }

    private Long num(Object v) { return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v)); }
}
