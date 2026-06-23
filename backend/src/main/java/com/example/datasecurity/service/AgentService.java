package com.example.datasecurity.service;

import com.example.datasecurity.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentService {
    private final JdbcTemplate jdbc;
    private final AuthService auth;
    private final FieldClassificationAgentService fieldAgent;
    private final AccessReviewAgentService accessAgent;
    private final AuditRiskAgentService riskAgent;
    private final SecurityReportAgentService reportAgent;
    private final AgentChatService chatAgent;

    public AgentClassifyResultDTO classifyField(Long fieldId, boolean apply, HttpServletRequest request) {
        Long userId = auth.currentUserId(request);
        AgentClassifyResultDTO result = fieldAgent.analyze(fieldId, userId, apply);
        saveTask("FIELD_CLASSIFY", "fieldId=" + fieldId + ", apply=" + apply, result.toString(), userId);
        saveRecommendation("FIELD_CLASSIFY", "data_field_asset", fieldId, result.getCategoryName() + " / " + result.getLevelCode(), "medium", result.getConfidence(), result.getReason(), result.getSuggestion(), result.isApplied(), userId);
        auth.audit(userId, "AGENT_FIELD_CLASSIFY", "data_field_asset", fieldId, auth.ip(request), "SUCCESS", result.getReason());
        return result;
    }

    public List<AgentClassifyResultDTO> classifyAll(boolean apply, HttpServletRequest request) {
        return jdbc.queryForList("select id from data_field_asset").stream()
                .map(r -> classifyField(num(r.get("ID")), apply, request)).toList();
    }

    public AgentAccessReviewResultDTO reviewAccess(Long requestId, HttpServletRequest request) {
        Long userId = auth.currentUserId(request);
        AgentAccessReviewResultDTO result = accessAgent.review(requestId);
        saveTask("ACCESS_REVIEW", "requestId=" + requestId, result.toString(), userId);
        saveRecommendation("ACCESS_REVIEW", "access_request", requestId, result.getRecommendation(), result.getRiskLevel(), result.getConfidence(), result.getReason(), result.getSuggestion(), false, userId);
        auth.audit(userId, "AGENT_ACCESS_REVIEW", "access_request", requestId, auth.ip(request), "SUCCESS", result.getReason());
        return result;
    }

    public List<Map<String, Object>> analyzeAudit(HttpServletRequest request) {
        List<Map<String, Object>> alerts = riskAgent.analyze(auth.currentUserId(request));
        auth.audit(auth.currentUserId(request), "AGENT_AUDIT_RISK", "audit_log", null, auth.ip(request), "SUCCESS", "生成风险告警 " + alerts.size() + " 条");
        return alerts;
    }

    public SecurityReportDTO report(HttpServletRequest request) {
        SecurityReportDTO report = reportAgent.report();
        saveTask("SECURITY_REPORT", "summary", "markdown length=" + report.getMarkdown().length(), auth.currentUserId(request));
        auth.audit(auth.currentUserId(request), "AGENT_SECURITY_REPORT", "agent_task", null, auth.ip(request), "SUCCESS", "生成安全治理报告");
        return report;
    }

    public AgentChatResponseDTO chat(AgentChatRequestDTO body, HttpServletRequest request) {
        AgentChatResponseDTO response = chatAgent.chat(body, auth.currentUserId(request));
        auth.audit(auth.currentUserId(request), "AGENT_CHAT", "agent_chat_history", null, auth.ip(request), "SUCCESS", body.getQuestion());
        return response;
    }

    public Map<String, Object> workbench() {
        return Map.of(
                "todayTaskCount", jdbc.queryForObject("select count(*) from agent_task where cast(created_time as date)=current_date", Integer.class),
                "openAlertCount", jdbc.queryForObject("select count(*) from risk_alert where status in ('OPEN','GENERATED')", Integer.class),
                "highAlertCount", jdbc.queryForObject("select count(*) from risk_alert where risk_level='high' and status in ('OPEN','GENERATED')", Integer.class),
                "recentRecommendations", jdbc.queryForList("select top 8 * from agent_recommendation order by created_time desc, recommendation_id desc"),
                "recentAlerts", jdbc.queryForList("select top 8 * from risk_alert order by created_time desc, alert_id desc")
        );
    }

    public Object alerts(String level) {
        if (level == null || level.isBlank()) return jdbc.queryForList("select * from risk_alert order by created_time desc, alert_id desc");
        return jdbc.queryForList("select * from risk_alert where risk_level=? order by created_time desc, alert_id desc", level);
    }

    public Object handleAlert(Long id, HttpServletRequest request) {
        jdbc.update("update risk_alert set status='HANDLED', handled_by=?, handled_time=? where alert_id=?", auth.currentUserId(request), LocalDateTime.now(), id);
        auth.audit(auth.currentUserId(request), "AGENT_HANDLE_ALERT", "risk_alert", id, auth.ip(request), "SUCCESS", "风险告警已处理");
        return Map.of("success", true);
    }

    public Object unclassifiedFields() {
        return jdbc.queryForList("""
                select f.*, t.table_name from data_field_asset f
                left join data_table_asset t on t.id=f.table_id
                left join field_classification fc on fc.field_id=f.id
                where fc.id is null order by f.id
                """);
    }

    public Object recommendations() {
        return jdbc.queryForList("select * from agent_recommendation order by created_time desc, recommendation_id desc");
    }

    private void saveTask(String type, String input, String output, Long userId) {
        jdbc.update("insert into agent_task(task_type,task_status,input_data,output_data,created_by,created_time,finished_time) values(?,?,?,?,?,?,?)",
                type, "SUCCESS", input, output, userId, LocalDateTime.now(), LocalDateTime.now());
    }

    private void saveRecommendation(String type, String targetType, Long targetId, String result, String risk, double confidence, String reason, String suggestion, boolean applied, Long userId) {
        jdbc.update("""
                insert into agent_recommendation(recommendation_type,target_type,target_id,recommendation_result,risk_level,confidence,reason,suggestion,applied,created_time,created_by)
                values(?,?,?,?,?,?,?,?,?,?,?)
                """, type, targetType, targetId, result, risk, confidence, reason, suggestion, applied, LocalDateTime.now(), userId);
    }

    private Long num(Object v) { return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v)); }
}
