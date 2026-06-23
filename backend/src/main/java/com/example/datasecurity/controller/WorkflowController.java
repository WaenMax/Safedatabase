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
    public Object requests(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        String base = """
                select ar.*, u.username, f.field_name, l.level_code
                from access_request ar
                join sys_user u on u.id=ar.user_id
                join data_field_asset f on f.id=ar.field_id
                left join field_classification fc on fc.field_id=f.id
                left join classification_level l on l.id=fc.level_id
                """;
        int offset = (page - 1) * pageSize;
        if ("user".equals(auth.currentRole(request))) {
            Long userId = auth.currentUserId(request);
            int total = jdbc.queryForObject(
                    "select count(*) from access_request where user_id=?", Integer.class, userId);
            var rows = jdbc.queryForList(base + " where ar.user_id=? order by ar.id desc limit ? offset ?",
                    userId, pageSize, offset);
            return Map.of("rows", rows, "total", total);
        }
        int total = jdbc.queryForObject("select count(*) from access_request", Integer.class);
        var rows = jdbc.queryForList(base + " order by ar.id desc limit ? offset ?", pageSize, offset);
        return Map.of("rows", rows, "total", total);
    }

    @GetMapping("/api/access-requests/{id}/detail")
    public Object requestDetail(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> detail = jdbc.queryForMap("""
                select ar.*, u.username, f.field_name, f.field_type, f.field_comment, f.sample_value,
                       c.category_name, l.level_code, l.level_name, fc.classify_method, fc.remark classify_reason
                from access_request ar
                join sys_user u on u.id=ar.user_id
                join data_field_asset f on f.id=ar.field_id
                left join field_classification fc on fc.field_id=f.id
                left join classification_category c on c.id=fc.category_id
                left join classification_level l on l.id=fc.level_id
                where ar.id=?
                """, id);
        if ("user".equals(auth.currentRole(request)) && !auth.currentUserId(request).equals(num(detail.get("USER_ID")))) {
            throw new IllegalArgumentException("只能查看本人的访问申请");
        }
        Long fieldId = num(detail.get("FIELD_ID"));
        return Map.of(
                "request", detail,
                "approvalRecords", jdbc.queryForList("""
                        select ar.*, u.username approver_name from approval_record ar
                        left join sys_user u on u.id=ar.approver_id where ar.request_id=? order by ar.approval_time desc
                        """, id),
                "historyAccess", jdbc.queryForList("""
                        select top 8 a.*, u.username from audit_log a left join sys_user u on u.id=a.user_id
                        where a.target_type='data_field_asset' and a.target_id=? and a.operation_type in ('VIEW_RAW_VALUE','CREATE_ACCESS_REQUEST')
                        order by a.operation_time desc
                        """, fieldId),
                "maskingPolicies", jdbc.queryForList("select * from masking_policy where enabled=1 order by id"),
                "riskAlerts", jdbc.queryForList("""
                        select top 5 * from risk_alert
                        where target_id=? or user_id=? order by created_time desc, alert_id desc
                        """, fieldId, detail.get("USER_ID"))
        );
    }

    @PostMapping("/api/access-requests")
    public Object create(@RequestBody Map<String, Object> b, HttpServletRequest request) {
        Long userId = auth.currentUserId(request);
        if (userId == null && b.get("user_id") != null) userId = num(b.get("user_id"));
        Long fieldId = num(b.get("field_id"));
        Integer active = jdbc.queryForObject("""
                select count(*) from access_request
                where user_id=? and field_id=? and status in ('PENDING','APPROVED') and valid_until > CURRENT_TIMESTAMP
                """, Integer.class, userId, fieldId);
        if (active != null && active > 0) {
            auth.audit(userId, "CREATE_ACCESS_REQUEST", "data_field_asset", fieldId, auth.ip(request), "DENIED", "重复提交有效访问申请");
            return Map.of("success", false, "duplicate", true, "message", "该字段已有待审批或有效期内的访问申请。");
        }
        jdbc.update("insert into access_request(user_id,field_id,reason,status,request_time,valid_until) values(?,?,?,?,?,?)",
                userId, fieldId, b.get("reason"), "PENDING", LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        auth.audit(userId, "CREATE_ACCESS_REQUEST", "data_field_asset", fieldId, auth.ip(request), "SUCCESS", "提交访问申请");
        return Map.of("success", true);
    }

    @PutMapping("/api/access-requests/{id}/approve")
    public Object approve(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String comment = requiredComment(body);
        ensurePending(id);
        jdbc.update("update access_request set status='APPROVED', valid_until=? where id=?", LocalDateTime.now().plusDays(7), id);
        jdbc.update("insert into approval_record(request_id,approver_id,approval_result,approval_comment,approval_time) values(?,?,?,?,?)",
                id, auth.currentUserId(request), "APPROVED", comment, LocalDateTime.now());
        auth.audit(auth.currentUserId(request), "APPROVE_REQUEST", "access_request", id, auth.ip(request), "SUCCESS", comment);
        return Map.of("success", true);
    }

    @PutMapping("/api/access-requests/{id}/reject")
    public Object reject(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String comment = requiredComment(body);
        ensurePending(id);
        jdbc.update("update access_request set status='REJECTED' where id=?", id);
        jdbc.update("insert into approval_record(request_id,approver_id,approval_result,approval_comment,approval_time) values(?,?,?,?,?)",
                id, auth.currentUserId(request), "REJECTED", comment, LocalDateTime.now());
        auth.audit(auth.currentUserId(request), "REJECT_REQUEST", "access_request", id, auth.ip(request), "SUCCESS", comment);
        return Map.of("success", true);
    }

    @GetMapping("/api/audit-logs")
    public Object auditLogs(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        StringBuilder where = new StringBuilder(" where 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (user != null && !user.isBlank()) {
            where.append(" and u.username like ?");
            params.add("%" + user + "%");
        }
        if (operation != null && !operation.isBlank()) {
            where.append(" and a.operation_type like ?");
            params.add("%" + operation + "%");
        }
        if (result != null && !result.isBlank()) {
            where.append(" and a.result = ?");
            params.add(result);
        }
        if (startTime != null && !startTime.isBlank()) {
            where.append(" and a.operation_time >= ?");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            where.append(" and a.operation_time <= ?");
            params.add(endTime);
        }
        String base = " from audit_log a left join sys_user u on u.id=a.user_id" + where;
        int total = jdbc.queryForObject("select count(*)" + base, Integer.class, params.toArray());
        int offset = (page - 1) * pageSize;
        var rows = jdbc.queryForList("select a.*, u.username" + base + " order by a.operation_time desc, a.id desc limit ? offset ?",
                addParams(params, pageSize, offset));
        return Map.of("rows", rows, "total", total);
    }

    @GetMapping("/api/dashboard/summary")
    public Object dashboard() {
        return Map.of(
                "sourceCount", jdbc.queryForObject("select count(*) from data_source", Integer.class),
                "tableCount", jdbc.queryForObject("select count(*) from data_table_asset", Integer.class),
                "fieldCount", jdbc.queryForObject("select count(*) from data_field_asset", Integer.class),
                "sensitiveFieldCount", jdbc.queryForObject("select count(*) from data_field_asset where is_sensitive=1", Integer.class),
                "highSensitiveFieldCount", jdbc.queryForObject("""
                        select count(*) from field_classification fc
                        join classification_level l on l.id=fc.level_id
                        where l.level_code in ('L3','L4','L5')
                        """, Integer.class),
                "pendingRequestCount", jdbc.queryForObject("select count(*) from access_request where status='PENDING'", Integer.class),
                "openRiskAlertCount", jdbc.queryForObject("select count(*) from risk_alert where status in ('OPEN','GENERATED')", Integer.class),
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

    private String requiredComment(Map<String, Object> body) {
        String comment = body == null ? "" : String.valueOf(body.getOrDefault("comment", "")).trim();
        if (comment.isBlank()) throw new IllegalArgumentException("审批意见不能为空");
        return comment;
    }

    private void ensurePending(Long id) {
        String status = jdbc.queryForObject("select status from access_request where id=?", String.class, id);
        if (!"PENDING".equals(status)) throw new IllegalArgumentException("只能处理待审批申请");
    }

    private Object[] addParams(java.util.List<Object> params, Object... more) {
        Object[] result = new Object[params.size() + more.length];
        for (int i = 0; i < params.size(); i++) result[i] = params.get(i);
        for (int i = 0; i < more.length; i++) result[params.size() + i] = more[i];
        return result;
    }
}
