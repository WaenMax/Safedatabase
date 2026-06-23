package com.example.datasecurity.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        List<Map<String, Object>> users = jdbc.queryForList("""
                select u.id, u.username, u.real_name, u.password, r.role_code, r.role_name
                from sys_user u
                join sys_user_role ur on ur.user_id = u.id
                join sys_role r on r.id = ur.role_id
                where u.username = ? and u.enabled = 1
                """, username);
        if (users.isEmpty() || !passwordEncoder.matches(password, String.valueOf(users.get(0).get("password")))) {
            audit(null, "LOGIN", "sys_user", null, ip(request), "FAIL", "用户名或密码错误: " + username);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        Map<String, Object> user = new LinkedHashMap<>();
        users.get(0).forEach((key, value) -> user.put(key.toLowerCase(Locale.ROOT), value));
        user.remove("password");
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((username + ":" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
        sessions.put(token, user);
        audit(asLong(user.get("id")), "LOGIN", "sys_user", asLong(user.get("id")), ip(request), "SUCCESS", "用户登录");
        return Map.of("token", token, "user", user);
    }

    public Map<String, Object> current(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return sessions.getOrDefault(token.substring(7), anonymous());
        }
        return anonymous();
    }

    public Long currentUserId(HttpServletRequest request) {
        return asLong(current(request).get("id"));
    }

    public String currentRole(HttpServletRequest request) {
        return String.valueOf(current(request).getOrDefault("role_code", "anonymous"));
    }

    public void audit(Long userId, String operation, String targetType, Long targetId, String ip, String result, String detail) {
        jdbc.update("""
                insert into audit_log(user_id, operation_type, target_type, target_id, operation_time, ip_address, result, detail)
                values(?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, operation, targetType, targetId, LocalDateTime.now(), ip, result, detail);
    }

    public String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0];
    }

    private Map<String, Object> anonymous() {
        return Map.of("username", "anonymous", "role_code", "anonymous");
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
