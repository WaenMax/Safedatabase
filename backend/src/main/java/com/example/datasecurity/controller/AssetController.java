package com.example.datasecurity.controller;

import com.example.datasecurity.service.AuthService;
import com.example.datasecurity.service.MaskingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AssetController {
    private final JdbcTemplate jdbc;
    private final AuthService auth;
    private final MaskingService masking;

    @GetMapping("/api/data-sources")
    public Object sources() { return jdbc.queryForList("select * from data_source order by id"); }

    @PostMapping("/api/data-sources")
    public Object createSource(@RequestBody Map<String, Object> b) {
        jdbc.update("insert into data_source(source_name,source_type,host,port,database_name,description,created_time) values(?,?,?,?,?,?,?)",
                b.get("source_name"), b.get("source_type"), b.get("host"), b.get("port"), b.get("database_name"), b.get("description"), LocalDateTime.now());
        return Map.of("success", true);
    }

    @PutMapping("/api/data-sources/{id}")
    public Object updateSource(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        jdbc.update("update data_source set source_name=?, source_type=?, host=?, port=?, database_name=?, description=? where id=?",
                b.get("source_name"), b.get("source_type"), b.get("host"), b.get("port"), b.get("database_name"), b.get("description"), id);
        return Map.of("success", true);
    }

    @DeleteMapping("/api/data-sources/{id}")
    public Object deleteSource(@PathVariable Long id) { jdbc.update("delete from data_source where id=?", id); return Map.of("success", true); }

    @GetMapping("/api/tables")
    public Object tables() { return jdbc.queryForList("select t.*, s.source_name from data_table_asset t left join data_source s on s.id=t.source_id order by t.id"); }

    @PostMapping("/api/tables")
    public Object createTable(@RequestBody Map<String, Object> b) {
        jdbc.update("insert into data_table_asset(source_id,table_name,business_name,description,owner_department,created_time) values(?,?,?,?,?,?)",
                b.get("source_id"), b.get("table_name"), b.get("business_name"), b.get("description"), b.get("owner_department"), LocalDateTime.now());
        return Map.of("success", true);
    }

    @PutMapping("/api/tables/{id}")
    public Object updateTable(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        jdbc.update("update data_table_asset set source_id=?, table_name=?, business_name=?, description=?, owner_department=? where id=?",
                b.get("source_id"), b.get("table_name"), b.get("business_name"), b.get("description"), b.get("owner_department"), id);
        return Map.of("success", true);
    }

    @DeleteMapping("/api/tables/{id}")
    public Object deleteTable(@PathVariable Long id) { jdbc.update("delete from data_table_asset where id=?", id); return Map.of("success", true); }

    @GetMapping("/api/fields")
    public Object fields() {
        return jdbc.queryForList("""
                select f.*, t.table_name, c.category_name, l.level_code, l.level_name
                from data_field_asset f
                left join data_table_asset t on t.id=f.table_id
                left join field_classification fc on fc.field_id=f.id
                left join classification_category c on c.id=fc.category_id
                left join classification_level l on l.id=fc.level_id
                order by f.id
                """);
    }

    @GetMapping("/api/fields/{id}")
    public Object field(@PathVariable Long id, HttpServletRequest request) {
        auth.audit(auth.currentUserId(request), "VIEW_FIELD", "data_field_asset", id, auth.ip(request), "SUCCESS", "查看字段详情");
        return jdbc.queryForMap("""
                select f.*, t.table_name, c.category_name, l.level_code, l.level_name
                from data_field_asset f
                left join data_table_asset t on t.id=f.table_id
                left join field_classification fc on fc.field_id=f.id
                left join classification_category c on c.id=fc.category_id
                left join classification_level l on l.id=fc.level_id
                where f.id=?
                """, id);
    }

    @PostMapping("/api/fields")
    public Object createField(@RequestBody Map<String, Object> b) {
        jdbc.update("insert into data_field_asset(table_id,field_name,field_type,field_comment,sample_value,is_sensitive,created_time) values(?,?,?,?,?,?,?)",
                b.get("table_id"), b.get("field_name"), b.get("field_type"), b.get("field_comment"), b.get("sample_value"), bool(b.get("is_sensitive")), LocalDateTime.now());
        return Map.of("success", true);
    }

    @PutMapping("/api/fields/{id}")
    public Object updateField(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        jdbc.update("update data_field_asset set table_id=?, field_name=?, field_type=?, field_comment=?, sample_value=?, is_sensitive=? where id=?",
                b.get("table_id"), b.get("field_name"), b.get("field_type"), b.get("field_comment"), b.get("sample_value"), bool(b.get("is_sensitive")), id);
        return Map.of("success", true);
    }

    @DeleteMapping("/api/fields/{id}")
    public Object deleteField(@PathVariable Long id) { jdbc.update("delete from data_field_asset where id=?", id); return Map.of("success", true); }

    @GetMapping("/api/fields/{id}/masked-value")
    public Object maskedValue(@PathVariable Long id) {
        Map<String, Object> f = jdbc.queryForMap("select field_name, sample_value from data_field_asset where id=?", id);
        return Map.of("value", masking.mask(String.valueOf(f.get("sample_value")), String.valueOf(f.get("field_name"))));
    }

    @GetMapping("/api/fields/{id}/raw-value")
    public Object rawValue(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> f = jdbc.queryForMap("""
                select f.field_name, f.sample_value, coalesce(l.level_code,'L1') level_code
                from data_field_asset f left join field_classification fc on fc.field_id=f.id
                left join classification_level l on l.id=fc.level_id where f.id=?
                """, id);
        String role = auth.currentRole(request);
        Long userId = auth.currentUserId(request);
        String level = String.valueOf(f.get("level_code"));
        boolean high = level.equals("L4") || level.equals("L5");
        boolean approved = userId != null && jdbc.queryForObject("""
                select count(*) from access_request
                where user_id=? and field_id=? and status='APPROVED' and valid_until > CURRENT_TIMESTAMP
                """, Integer.class, userId, id) > 0;
        if (high && role.equals("user") && !approved) {
            auth.audit(userId, "VIEW_RAW_VALUE", "data_field_asset", id, auth.ip(request), "DENIED", "高敏感字段未审批");
            return Map.of("requiresApproval", true, "value", masking.mask(String.valueOf(f.get("sample_value")), String.valueOf(f.get("field_name"))));
        }
        auth.audit(userId, "VIEW_RAW_VALUE", "data_field_asset", id, auth.ip(request), "SUCCESS", "查看原始样例值");
        return Map.of("requiresApproval", false, "value", f.get("sample_value"));
    }

    private boolean bool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v)); }
}
