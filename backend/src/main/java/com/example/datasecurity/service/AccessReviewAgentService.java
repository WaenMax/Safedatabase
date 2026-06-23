package com.example.datasecurity.service;

import com.example.datasecurity.dto.AgentAccessReviewResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccessReviewAgentService {
    private final JdbcTemplate jdbc;

    public AgentAccessReviewResultDTO review(Long requestId) {
        Map<String, Object> r = jdbc.queryForMap("""
                select ar.*, u.username, sr.role_code, f.field_name, coalesce(l.level_code,'L1') level_code
                from access_request ar
                join sys_user u on u.id=ar.user_id
                left join sys_user_role ur on ur.user_id=u.id
                left join sys_role sr on sr.id=ur.role_id
                join data_field_asset f on f.id=ar.field_id
                left join field_classification fc on fc.field_id=f.id
                left join classification_level l on l.id=fc.level_id
                where ar.id=?
                """, requestId);
        String role = String.valueOf(r.get("ROLE_CODE"));
        String level = String.valueOf(r.get("LEVEL_CODE"));
        String reasonText = String.valueOf(r.getOrDefault("REASON", ""));
        Long userId = num(r.get("USER_ID"));
        int rejected = jdbc.queryForObject("""
                select count(*) from access_request
                where user_id=? and status='REJECTED' and request_time >= date_sub(current_timestamp, interval 7 day)
                """, Integer.class, userId);

        String risk = "low";
        String recommendation = "approve";
        double confidence = 0.75;
        StringBuilder reason = new StringBuilder();

        if ("L5".equals(level) && "user".equals(role)) {
            risk = "high";
            recommendation = "reject";
            confidence = 0.92;
            reason.append("普通用户申请 L5 核心数据；");
        } else if ("L4".equals(level) && "user".equals(role)) {
            risk = "medium";
            recommendation = "manual_review";
            confidence = 0.78;
            reason.append("普通用户申请 L4 高敏感数据；");
        } else if ((role.equals("admin") || role.equals("security_admin")) && "L3".equals(level)) {
            risk = "low";
            recommendation = "approve";
            confidence = 0.88;
            reason.append("管理员或安全管理员申请 L3 数据；");
        }
        if (reasonText.trim().length() < 10) {
            if ("low".equals(risk)) risk = "medium";
            if (!"reject".equals(recommendation)) recommendation = "manual_review";
            reason.append("申请理由为空或少于 10 个字；");
        }
        if (rejected >= 3) {
            risk = "high";
            recommendation = "reject";
            confidence = 0.94;
            reason.append("最近 7 天存在 3 次以上驳回记录；");
        }
        if (reason.isEmpty()) reason.append("未发现明显异常，按低风险处理；");
        String suggestion = switch (recommendation) {
            case "reject" -> "建议驳回该申请，并要求申请人补充必要性说明。";
            case "manual_review" -> "建议审批人员结合业务背景人工复核，可缩短授权有效期。";
            default -> "建议通过，并继续保留审计日志。";
        };
        return new AgentAccessReviewResultDTO(requestId, recommendation, risk, confidence, reason.toString(), suggestion);
    }

    private Long num(Object v) { return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v)); }
}
