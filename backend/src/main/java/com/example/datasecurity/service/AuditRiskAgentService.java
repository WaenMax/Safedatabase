package com.example.datasecurity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuditRiskAgentService {
    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> analyze(Long userId) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        for (Map<String, Object> row : jdbc.queryForList("""
                select user_id, count(*) cnt from audit_log
                where operation_type='VIEW_RAW_VALUE' and operation_time >= dateadd(minute, -10, current_timestamp)
                group by user_id having count(*) > 5
                """)) {
            alerts.add(create("FREQUENT_SENSITIVE_VIEW", "medium", num(row.get("USER_ID")), "audit_log", null,
                    "10 分钟内访问敏感字段超过 5 次", "建议核查访问目的并限制批量查看。"));
        }
        for (Map<String, Object> row : jdbc.queryForList("""
                select a.user_id, count(*) cnt from audit_log a
                join data_field_asset f on f.id=a.target_id
                join field_classification fc on fc.field_id=f.id
                join classification_level l on l.id=fc.level_id
                where a.operation_type='VIEW_RAW_VALUE' and l.level_code in ('L4','L5')
                  and a.operation_time >= dateadd(minute, -10, current_timestamp)
                group by a.user_id having count(*) > 3
                """)) {
            alerts.add(create("FREQUENT_HIGH_SENSITIVE_VIEW", "high", num(row.get("USER_ID")), "data_field_asset", null,
                    "10 分钟内访问 L4/L5 字段超过 3 次", "建议暂停高敏数据授权并人工复核。"));
        }
        for (Map<String, Object> row : jdbc.queryForList("""
                select user_id, count(*) cnt from audit_log
                where operation_type='LOGIN' and result='FAIL' and operation_time >= dateadd(hour, -1, current_timestamp)
                group by user_id having count(*) > 5
                """)) {
            alerts.add(create("LOGIN_FAIL_TOO_MANY", "high", num(row.get("USER_ID")), "sys_user", num(row.get("USER_ID")),
                    "同一用户 1 小时内登录失败超过 5 次", "建议锁定账号并通知管理员。"));
        }
        for (Map<String, Object> row : jdbc.queryForList("""
                select ip_address, count(distinct user_id) cnt from audit_log
                where operation_type='LOGIN' and result='SUCCESS' and operation_time >= dateadd(hour, -1, current_timestamp)
                group by ip_address having count(distinct user_id) >= 3
                """)) {
            alerts.add(create("MULTI_ACCOUNT_SAME_IP", "medium", null, "ip_address", null,
                    "同一 IP 1 小时内出现 3 个以上账号登录: " + row.get("IP_ADDRESS"), "建议核查是否为共享终端或异常登录。"));
        }
        for (Map<String, Object> row : jdbc.queryForList("""
                select id, user_id from audit_log
                where operation_type='UPDATE_CLASSIFICATION' and user_id not in (
                  select ur.user_id from sys_user_role ur join sys_role r on r.id=ur.role_id where r.role_code in ('admin','security_admin')
                )
                """)) {
            alerts.add(create("NON_ADMIN_CLASSIFICATION_CHANGE", "high", num(row.get("USER_ID")), "audit_log", num(row.get("ID")),
                    "非管理员修改分类分级结果", "建议回滚并检查权限配置。"));
        }
        jdbc.update("insert into agent_task(task_type,task_status,input_data,output_data,created_by,created_time,finished_time) values(?,?,?,?,?,?,?)",
                "AUDIT_RISK_ANALYZE", "SUCCESS", "audit_log", "generated alerts: " + alerts.size(), userId, LocalDateTime.now(), LocalDateTime.now());
        return alerts;
    }

    private Map<String, Object> create(String type, String level, Long userId, String targetType, Long targetId, String desc, String suggestion) {
        Integer exists = jdbc.queryForObject("""
                select count(*) from risk_alert
                where risk_type=? and coalesce(user_id,-1)=coalesce(?,-1) and target_type=? and coalesce(target_id,-1)=coalesce(?,-1)
                  and status in ('OPEN','GENERATED')
                """, Integer.class, type, userId, targetType, targetId);
        if (exists != null && exists > 0) {
            return Map.of("alertId", 0, "riskType", type, "riskLevel", level, "description", desc, "suggestion", suggestion, "duplicate", true);
        }
        jdbc.update("insert into risk_alert(risk_type,risk_level,user_id,target_type,target_id,description,suggestion,status,created_time) values(?,?,?,?,?,?,?,?,?)",
                type, level, userId, targetType, targetId, desc, suggestion, "GENERATED", LocalDateTime.now());
        Long id = jdbc.queryForObject("select max(alert_id) from risk_alert", Long.class);
        return Map.of("alertId", id, "riskType", type, "riskLevel", level, "description", desc, "suggestion", suggestion);
    }

    private Long num(Object v) { return v == null ? null : (v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v))); }
}
