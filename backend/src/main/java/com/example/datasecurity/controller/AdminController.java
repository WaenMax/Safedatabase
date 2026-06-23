package com.example.datasecurity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    @GetMapping("/api/users")
    public Object users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int total = jdbc.queryForObject("select count(*) from sys_user", Integer.class);
        int offset = (page - 1) * pageSize;
        var rows = jdbc.queryForList("""
                select u.id, u.username, u.real_name, u.email, u.phone, u.enabled, r.id role_id, r.role_code, r.role_name
                from sys_user u left join sys_user_role ur on ur.user_id=u.id left join sys_role r on r.id=ur.role_id
                order by u.id
                limit ? offset ?
                """, pageSize, offset);
        return Map.of("rows", rows, "total", total);
    }

    @PostMapping("/api/users")
    public Object createUser(@RequestBody Map<String, Object> b) {
        Long roleId = num(b.getOrDefault("role_id", 3));
        jdbc.update("insert into sys_user(username,password,real_name,email,phone,enabled,created_time) values(?,?,?,?,?,?,?)",
                b.get("username"), encoder.encode(String.valueOf(b.getOrDefault("password", "123456"))),
                b.get("real_name"), b.get("email"), b.get("phone"), bool(b.getOrDefault("enabled", true)), LocalDateTime.now());
        Long id = jdbc.queryForObject("select max(id) from sys_user", Long.class);
        jdbc.update("insert into sys_user_role(user_id, role_id) values(?,?)", id, roleId);
        return Map.of("id", id);
    }

    @PutMapping("/api/users/{id}")
    public Object updateUser(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        jdbc.update("update sys_user set real_name=?, email=?, phone=?, enabled=? where id=?",
                b.get("real_name"), b.get("email"), b.get("phone"), bool(b.getOrDefault("enabled", true)), id);
        if (b.get("role_id") != null) {
            jdbc.update("delete from sys_user_role where user_id=?", id);
            jdbc.update("insert into sys_user_role(user_id, role_id) values(?,?)", id, num(b.get("role_id")));
        }
        return Map.of("success", true);
    }

    @DeleteMapping("/api/users/{id}")
    public Object deleteUser(@PathVariable Long id) {
        jdbc.update("delete from sys_user_role where user_id=?", id);
        jdbc.update("delete from sys_user where id=?", id);
        return Map.of("success", true);
    }

    @GetMapping("/api/roles")
    public Object roles() {
        return jdbc.queryForList("select * from sys_role order by id");
    }

    private Long num(Object v) {
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }

    private boolean bool(Object v) {
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
    }
}
