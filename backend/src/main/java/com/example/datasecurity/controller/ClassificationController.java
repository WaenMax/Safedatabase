package com.example.datasecurity.controller;

import com.example.datasecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
public class ClassificationController {
    private final JdbcTemplate jdbc;
    private final AuthService auth;

    @GetMapping("/api/categories")
    public Object categories() { return jdbc.queryForList("select * from classification_category order by id"); }

    @GetMapping("/api/levels")
    public Object levels() { return jdbc.queryForList("select * from classification_level order by level_order"); }

    @GetMapping("/api/field-classifications")
    public Object classifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int total = jdbc.queryForObject("select count(*) from field_classification", Integer.class);
        int offset = (page - 1) * pageSize;
        var rows = jdbc.queryForList("""
                select fc.*, f.field_name, f.sample_value, f.field_comment, c.category_name, l.level_code, l.level_name
                from field_classification fc
                join data_field_asset f on f.id=fc.field_id
                join classification_category c on c.id=fc.category_id
                join classification_level l on l.id=fc.level_id
                order by fc.id
                limit ? offset ?
                """, pageSize, offset).stream().peek(this::attachEvidence).toList();
        return Map.of("rows", rows, "total", total);
    }

    @PostMapping("/api/field-classifications")
    public Object createClassification(@RequestBody Map<String, Object> b, HttpServletRequest request) {
        jdbc.update("""
                merge into field_classification(field_id, category_id, level_id, classify_method, classified_by, classified_time, remark)
                key(field_id) values(?, ?, ?, ?, ?, ?, ?)
                """, b.get("field_id"), b.get("category_id"), b.get("level_id"), "MANUAL", auth.currentUserId(request), LocalDateTime.now(), b.get("remark"));
        jdbc.update("update data_field_asset set is_sensitive=(select case when level_order>=3 then 1 else 0 end from classification_level where id=?) where id=?",
                b.get("level_id"), b.get("field_id"));
        auth.audit(auth.currentUserId(request), "UPDATE_CLASSIFICATION", "data_field_asset", num(b.get("field_id")), auth.ip(request), "SUCCESS", "人工分类分级");
        return Map.of("success", true);
    }

    @PutMapping("/api/field-classifications/{id}")
    public Object updateClassification(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpServletRequest request) {
        jdbc.update("update field_classification set category_id=?, level_id=?, classify_method='MANUAL', classified_by=?, classified_time=?, remark=? where id=?",
                b.get("category_id"), b.get("level_id"), auth.currentUserId(request), LocalDateTime.now(), b.get("remark"), id);
        auth.audit(auth.currentUserId(request), "UPDATE_CLASSIFICATION", "field_classification", id, auth.ip(request), "SUCCESS", "修改分类分级");
        return Map.of("success", true);
    }

    @PostMapping("/api/field-classifications/auto-classify")
    public Object autoClassify(HttpServletRequest request) {
        List<Map<String, Object>> fields = jdbc.queryForList("select id, field_name from data_field_asset");
        List<Map<String, Object>> rules = jdbc.queryForList("select * from classification_rule where enabled=1 order by id");
        int count = 0;
        for (Map<String, Object> field : fields) {
            String name = String.valueOf(field.get("field_name")).toLowerCase();
            for (Map<String, Object> rule : rules) {
                String pattern = String.valueOf(rule.get("match_pattern")).toLowerCase();
                boolean hit = "regex".equalsIgnoreCase(String.valueOf(rule.get("match_type")))
                        ? Pattern.compile(pattern).matcher(name).find()
                        : List.of(pattern.split("[,/|]")).stream().anyMatch(name::contains);
                if (hit) {
                    jdbc.update("""
                            merge into field_classification(field_id, category_id, level_id, classify_method, classified_by, classified_time, remark)
                            key(field_id) values(?,?,?,?,?,?,?)
                            """,
                            field.get("id"), rule.get("category_id"), rule.get("level_id"), "AUTO", auth.currentUserId(request), LocalDateTime.now(), "命中规则: " + rule.get("rule_name"));
                    jdbc.update("update data_field_asset set is_sensitive=(select case when level_order>=3 then 1 else 0 end from classification_level where id=?) where id=?",
                            rule.get("level_id"), field.get("id"));
                    count++;
                    break;
                }
            }
        }
        auth.audit(auth.currentUserId(request), "AUTO_CLASSIFY", "data_field_asset", null, auth.ip(request), "SUCCESS", "自动分类字段数: " + count);
        return Map.of("classified", count);
    }

    @GetMapping("/api/rules")
    public Object rules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int total = jdbc.queryForObject("select count(*) from classification_rule", Integer.class);
        int offset = (page - 1) * pageSize;
        var rows = jdbc.queryForList("""
                select r.*, c.category_name, l.level_code, l.level_name
                from classification_rule r
                left join classification_category c on c.id=r.category_id
                left join classification_level l on l.id=r.level_id
                order by r.id
                limit ? offset ?
                """, pageSize, offset);
        return Map.of("rows", rows, "total", total);
    }

    @PostMapping("/api/rules")
    public Object createRule(@RequestBody Map<String, Object> b) {
        jdbc.update("insert into classification_rule(rule_name,match_type,match_pattern,category_id,level_id,enabled,created_time) values(?,?,?,?,?,?,?)",
                b.get("rule_name"), b.get("match_type"), b.get("match_pattern"), b.get("category_id"), b.get("level_id"), bool(b.get("enabled")), LocalDateTime.now());
        return Map.of("success", true);
    }

    @PutMapping("/api/rules/{id}")
    public Object updateRule(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        jdbc.update("update classification_rule set rule_name=?, match_type=?, match_pattern=?, category_id=?, level_id=?, enabled=? where id=?",
                b.get("rule_name"), b.get("match_type"), b.get("match_pattern"), b.get("category_id"), b.get("level_id"), bool(b.get("enabled")), id);
        return Map.of("success", true);
    }

    @DeleteMapping("/api/rules/{id}")
    public Object deleteRule(@PathVariable Long id) { jdbc.update("delete from classification_rule where id=?", id); return Map.of("success", true); }

    @GetMapping("/api/masking-policies")
    public Object policies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int total = jdbc.queryForObject("select count(*) from masking_policy", Integer.class);
        int offset = (page - 1) * pageSize;
        var rows = jdbc.queryForList("select * from masking_policy order by id limit ? offset ?", pageSize, offset);
        return Map.of("rows", rows, "total", total);
    }

    @PostMapping("/api/masking-policies")
    public Object createPolicy(@RequestBody Map<String, Object> b, HttpServletRequest request) {
        jdbc.update("insert into masking_policy(policy_name,policy_type,example_before,example_after,description,enabled) values(?,?,?,?,?,?)",
                b.get("policy_name"), b.get("policy_type"), b.get("example_before"), b.get("example_after"), b.get("description"), bool(b.getOrDefault("enabled", true)));
        auth.audit(auth.currentUserId(request), "CREATE_MASKING_POLICY", "masking_policy", null, auth.ip(request), "SUCCESS", "新增脱敏策略: " + b.get("policy_name"));
        return Map.of("success", true);
    }

    @PutMapping("/api/masking-policies/{id}")
    public Object updatePolicy(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpServletRequest request) {
        jdbc.update("update masking_policy set policy_name=?, policy_type=?, example_before=?, example_after=?, description=?, enabled=? where id=?",
                b.get("policy_name"), b.get("policy_type"), b.get("example_before"), b.get("example_after"), b.get("description"), bool(b.getOrDefault("enabled", true)), id);
        auth.audit(auth.currentUserId(request), "UPDATE_MASKING_POLICY", "masking_policy", id, auth.ip(request), "SUCCESS", "修改脱敏策略: " + b.get("policy_name"));
        return Map.of("success", true);
    }

    @DeleteMapping("/api/masking-policies/{id}")
    public Object deletePolicy(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> existing = jdbc.queryForMap("select policy_name from masking_policy where id=?", id);
        jdbc.update("delete from masking_policy where id=?", id);
        auth.audit(auth.currentUserId(request), "DELETE_MASKING_POLICY", "masking_policy", id, auth.ip(request), "SUCCESS", "删除脱敏策略: " + existing.get("POLICY_NAME"));
        return Map.of("success", true);
    }

    private boolean bool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v)); }
    private Long num(Object v) { return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v)); }

    private void attachEvidence(Map<String, Object> row) {
        String fieldName = String.valueOf(row.getOrDefault("FIELD_NAME", "")).toLowerCase();
        String comment = String.valueOf(row.getOrDefault("FIELD_COMMENT", "")).toLowerCase();
        String sample = String.valueOf(row.getOrDefault("SAMPLE_VALUE", ""));
        String combined = fieldName + " " + comment;
        String rule = jdbc.queryForList("select rule_name, match_pattern from classification_rule where enabled=1 order by id").stream()
                .filter(r -> java.util.Arrays.stream(String.valueOf(r.get("MATCH_PATTERN")).toLowerCase().split("[,/|]"))
                        .map(String::trim).filter(s -> !s.isBlank()).anyMatch(combined::contains))
                .map(r -> r.get("RULE_NAME") + " (" + r.get("MATCH_PATTERN") + ")")
                .findFirst().orElse("未命中字段名规则");
        String format = "未命中格式特征";
        if (sample.matches("1[3-9]\\d{9}")) format = "样例值符合手机号格式";
        else if (sample.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) format = "样例值符合邮箱格式";
        else if (sample.matches("^\\d{17}[0-9Xx]$")) format = "样例值符合身份证格式";
        else if (sample.matches("^\\d{12,19}$")) format = "样例值符合银行卡/账号格式";
        else if (fieldName.contains("password") || fieldName.contains("secret") || fieldName.contains("token")) format = "字段名包含密码/密钥特征";
        row.put("EVIDENCE_RULE", rule);
        row.put("EVIDENCE_FORMAT", format);
        row.put("LEVEL_EXPLANATION", switch (String.valueOf(row.get("LEVEL_CODE"))) {
            case "L1" -> "公开数据，泄露影响较小";
            case "L2" -> "内部数据，仅限组织内部使用";
            case "L3" -> "敏感数据，默认脱敏展示";
            case "L4" -> "高敏数据，原始值访问需要审批";
            case "L5" -> "核心数据，按最高强度审计和审批";
            default -> "未定义等级";
        });
    }
}
