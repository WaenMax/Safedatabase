package com.example.datasecurity.service;

import com.example.datasecurity.dto.AgentClassifyResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FieldClassificationAgentService {
    private final JdbcTemplate jdbc;

    public AgentClassifyResultDTO analyze(Long fieldId, Long userId, boolean apply) {
        Map<String, Object> field = jdbc.queryForMap("select * from data_field_asset where id=?", fieldId);
        String name = lower(field.get("FIELD_NAME"));
        String comment = lower(field.get("FIELD_COMMENT"));
        String sample = String.valueOf(field.getOrDefault("SAMPLE_VALUE", ""));
        String combined = name + " " + comment;

        AgentClassifyResultDTO best = null;
        for (Map<String, Object> rule : jdbc.queryForList("select r.*, c.category_name, l.level_code, l.level_name from classification_rule r join classification_category c on c.id=r.category_id join classification_level l on l.id=r.level_id where r.enabled=1 order by r.id")) {
            String pattern = lower(rule.get("MATCH_PATTERN"));
            boolean hit = "regex".equalsIgnoreCase(String.valueOf(rule.get("MATCH_TYPE")))
                    ? Pattern.compile(pattern).matcher(combined).find()
                    : Arrays.stream(pattern.split("[,/|]")).map(String::trim).filter(s -> !s.isBlank()).anyMatch(combined::contains);
            if (hit) {
                best = dto(fieldId, rule, confidenceFor(rule, sample), "字段名或描述命中规则: " + rule.get("RULE_NAME"), apply);
                break;
            }
        }

        if (best == null) {
            if (sample.matches("1[3-9]\\d{9}")) {
                best = byCategoryLevel(fieldId, "个人基本信息", "L3", 0.88, "样例值符合手机号格式", apply);
            } else if (sample.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                best = byCategoryLevel(fieldId, "个人基本信息", "L3", 0.86, "样例值符合邮箱格式", apply);
            } else if (sample.matches("^\\d{17}[0-9Xx]$")) {
                best = byCategoryLevel(fieldId, "个人身份信息", "L4", 0.95, "样例值符合身份证格式", apply);
            } else {
                best = byCategoryLevel(fieldId, "公开数据", "L1", 0.55, "未命中敏感规则，默认建议为公开数据", apply);
            }
        }

        if (apply) {
            jdbc.update("""
                    merge into field_classification(field_id, category_id, level_id, classify_method, classified_by, classified_time, remark)
                    key(field_id) values(?,?,?,?,?,?,?)
                    """, fieldId, best.getCategoryId(), best.getLevelId(), "AGENT", userId, LocalDateTime.now(), best.getReason());
            jdbc.update("update data_field_asset set is_sensitive=(select case when level_order>=3 then 1 else 0 end from classification_level where id=?) where id=?", best.getLevelId(), fieldId);
            best.setApplied(true);
        }
        return best;
    }

    private AgentClassifyResultDTO dto(Long fieldId, Map<String, Object> rule, double confidence, String reason, boolean applied) {
        return new AgentClassifyResultDTO(fieldId, num(rule.get("CATEGORY_ID")), String.valueOf(rule.get("CATEGORY_NAME")),
                num(rule.get("LEVEL_ID")), String.valueOf(rule.get("LEVEL_CODE")), String.valueOf(rule.get("LEVEL_NAME")),
                reason, confidence, "建议应用该分类分级结果，并检查对应脱敏策略是否覆盖。", applied);
    }

    private AgentClassifyResultDTO byCategoryLevel(Long fieldId, String categoryName, String levelCode, double confidence, String reason, boolean applied) {
        Map<String, Object> row = jdbc.queryForMap("""
                select c.id category_id, c.category_name, l.id level_id, l.level_code, l.level_name
                from classification_category c cross join classification_level l
                where c.category_name=? and l.level_code=?
                """, categoryName, levelCode);
        return dto(fieldId, row, confidence, reason, applied);
    }

    private double confidenceFor(Map<String, Object> rule, String sample) {
        String pattern = lower(rule.get("MATCH_PATTERN"));
        double base = pattern.contains("id_card") || pattern.contains("password") || pattern.contains("token") || pattern.contains("secret") ? 0.95 : 0.90;
        if (pattern.contains("email")) base = 0.85;
        if (sample.matches("^\\d{17}[0-9Xx]$") || sample.matches("1[3-9]\\d{9}") || sample.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            base = Math.min(0.98, base + 0.03);
        }
        return base;
    }

    private String lower(Object v) { return v == null ? "" : String.valueOf(v).toLowerCase(Locale.ROOT); }
    private Long num(Object v) { return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v)); }
}
