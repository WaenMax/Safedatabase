package com.example.datasecurity.config;

import com.example.datasecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoleAccessInterceptor implements HandlerInterceptor {
    private final AuthService auth;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/")) return true;

        String role = auth.currentRole(request);
        boolean allowed = switch (role) {
            case "admin" -> true;
            case "security_admin" -> securityAdminAllowed(path, method);
            case "approver" -> approverAllowed(path, method);
            case "user" -> userAllowed(path, method);
            default -> false;
        };
        if (allowed) return true;

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"当前角色无权访问该接口\"}");
        return false;
    }

    private boolean securityAdminAllowed(String path, String method) {
        return path.startsWith("/api/dashboard/")
                || path.startsWith("/api/data-sources")
                || path.startsWith("/api/tables")
                || path.startsWith("/api/fields")
                || path.startsWith("/api/field-classifications")
                || path.startsWith("/api/categories")
                || path.startsWith("/api/levels")
                || path.startsWith("/api/rules")
                || path.startsWith("/api/masking-policies")
                || path.startsWith("/api/audit-logs")
                || path.startsWith("/api/agent");
    }

    private boolean approverAllowed(String path, String method) {
        if (path.startsWith("/api/dashboard/") || path.startsWith("/api/fields")) return true;
        if (path.equals("/api/access-requests") && Set.of("GET", "POST").contains(method)) return true;
        if (path.matches("/api/access-requests/\\d+/detail") && "GET".equals(method)) return true;
        if (path.matches("/api/access-requests/\\d+/(approve|reject)") && "PUT".equals(method)) return true;
        return path.startsWith("/api/agent/review-access-request") || path.startsWith("/api/agent/chat");
    }

    private boolean userAllowed(String path, String method) {
        if (path.startsWith("/api/dashboard/") || path.startsWith("/api/fields")) return "GET".equals(method);
        return path.equals("/api/access-requests") && Set.of("GET", "POST").contains(method)
                || path.matches("/api/access-requests/\\d+/detail") && "GET".equals(method)
                || path.startsWith("/api/agent/chat");
    }
}
