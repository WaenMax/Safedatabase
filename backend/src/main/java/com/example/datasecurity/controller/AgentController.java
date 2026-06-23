package com.example.datasecurity.controller;

import com.example.datasecurity.dto.AgentChatRequestDTO;
import com.example.datasecurity.service.AgentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    private final AgentService agentService;

    @GetMapping("/workbench")
    public Object workbench() { return agentService.workbench(); }

    @GetMapping("/recommendations")
    public Object recommendations() { return agentService.recommendations(); }

    @GetMapping("/unclassified-fields")
    public Object unclassifiedFields() { return agentService.unclassifiedFields(); }

    @PostMapping("/classify-field/{fieldId}")
    public Object classifyField(@PathVariable Long fieldId, @RequestParam(defaultValue = "false") boolean apply, HttpServletRequest request) {
        return agentService.classifyField(fieldId, apply, request);
    }

    @PostMapping("/classify-all-fields")
    public Object classifyAll(@RequestParam(defaultValue = "false") boolean apply, HttpServletRequest request) {
        return agentService.classifyAll(apply, request);
    }

    @PostMapping("/review-access-request/{requestId}")
    public Object reviewAccess(@PathVariable Long requestId, HttpServletRequest request) {
        return agentService.reviewAccess(requestId, request);
    }

    @PostMapping("/analyze-audit-logs")
    public Object analyzeAudit(HttpServletRequest request) {
        return agentService.analyzeAudit(request);
    }

    @GetMapping("/risk-alerts")
    public Object riskAlerts(@RequestParam(required = false) String level) {
        return agentService.alerts(level);
    }

    @PutMapping("/risk-alerts/{id}/handle")
    public Object handleAlert(@PathVariable Long id, HttpServletRequest request) {
        return agentService.handleAlert(id, request);
    }

    @GetMapping("/security-report")
    public Object report(HttpServletRequest request) {
        return agentService.report(request);
    }

    @PostMapping("/chat")
    public Object chat(@RequestBody AgentChatRequestDTO body, HttpServletRequest request) {
        try {
            return agentService.chat(body, request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
