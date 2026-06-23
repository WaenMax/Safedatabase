package com.example.datasecurity.controller;

import com.example.datasecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        return authService.login(body.get("username"), body.get("password"), request);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return authService.current(request);
    }
}
