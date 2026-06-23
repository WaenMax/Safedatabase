package com.example.datasecurity.service;

import com.example.datasecurity.dto.AgentChatRequestDTO;
import com.example.datasecurity.dto.AgentChatResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentChatService {
    private final JdbcTemplate jdbc;
    private final SecurityReportAgentService reportService;
    private final LlmProviderService llmProviderService;

    public AgentChatResponseDTO chat(AgentChatRequestDTO request, Long userId) {
        String q = request.getQuestion() == null ? "" : request.getQuestion();
        String provider = request.getProvider() == null || request.getProvider().isBlank() ? "local" : request.getProvider();
        if (llmProviderService.supports(provider)) {
            String answer = llmProviderService.chat(provider, q, buildContext(), request.getModel());
            jdbc.update("insert into agent_chat_history(user_id,question,answer,created_time) values(?,?,?,?)", userId, "[" + provider + "] " + q, answer, LocalDateTime.now());
            return new AgentChatResponseDTO(q, answer, provider, request.getModel());
        }
        return localChat(q, userId);
    }

    private AgentChatResponseDTO localChat(String q, Long userId) {
        String lower = q.toLowerCase(Locale.ROOT);
        String answer;
        if (q.contains("高敏感") || q.contains("L4") || q.contains("L5")) {
            answer = jdbc.queryForList("""
                    select f.field_name, l.level_code from data_field_asset f
                    join field_classification fc on fc.field_id=f.id
                    join classification_level l on l.id=fc.level_id
                    where l.level_code in ('L4','L5') order by l.level_order desc, f.id
                    """).stream().map(r -> r.get("FIELD_NAME") + "(" + r.get("LEVEL_CODE") + ")").collect(Collectors.joining("、"));
            answer = answer.isBlank() ? "当前没有 L4/L5 高敏感字段。" : "高敏感字段包括：" + answer + "。";
        } else if (q.contains("访问过") && q.contains("L4")) {
            answer = jdbc.queryForList("""
                    select distinct u.username from audit_log a join sys_user u on u.id=a.user_id
                    join field_classification fc on fc.field_id=a.target_id
                    join classification_level l on l.id=fc.level_id
                    where l.level_code='L4'
                    """).stream().map(r -> String.valueOf(r.get("USERNAME"))).collect(Collectors.joining("、"));
            answer = answer.isBlank() ? "最近审计日志中没有用户访问 L4 字段。" : "访问过 L4 数据的用户：" + answer + "。";
        } else if (q.contains("风险告警")) {
            answer = jdbc.queryForList("select risk_type, risk_level, description from risk_alert order by created_time desc, alert_id desc limit 5")
                    .stream().map(r -> "[" + r.get("RISK_LEVEL") + "] " + r.get("DESCRIPTION")).collect(Collectors.joining("\n"));
            answer = answer.isBlank() ? "当前暂无风险告警。" : answer;
        } else if (q.contains("没有分类") || q.contains("未分类")) {
            answer = jdbc.queryForList("select field_name from data_field_asset f left join field_classification fc on fc.field_id=f.id where fc.id is null")
                    .stream().map(r -> String.valueOf(r.get("FIELD_NAME"))).collect(Collectors.joining("、"));
            answer = answer.isBlank() ? "所有字段都已经完成分类分级。" : "未分类字段：" + answer + "。";
        } else if (q.contains("被拒绝") || lower.contains("rejected")) {
            answer = jdbc.queryForList("select id, reason from access_request where status='REJECTED' order by id desc")
                    .stream().map(r -> "申请#" + r.get("ID") + ": " + r.get("REASON")).collect(Collectors.joining("\n"));
            answer = answer.isBlank() ? "当前没有被拒绝的访问申请。" : answer;
        } else if (q.contains("敏感字段数量")) {
            answer = "当前系统的敏感字段数量是 " + jdbc.queryForObject("select count(*) from data_field_asset where is_sensitive=1", Integer.class) + " 个。";
        } else if (q.contains("治理建议") || q.contains("报告")) {
            answer = reportService.report().getMarkdown();
        } else {
            answer = "我可以回答高敏感字段、L4 访问用户、风险告警、未分类字段、被拒绝申请、敏感字段数量，并生成安全治理建议。";
        }
        jdbc.update("insert into agent_chat_history(user_id,question,answer,created_time) values(?,?,?,?)", userId, q, answer, LocalDateTime.now());
        return new AgentChatResponseDTO(q, answer, "local", null);
    }

    private String buildContext() {
        int sourceCount = jdbc.queryForObject("select count(*) from data_source", Integer.class);
        int tableCount = jdbc.queryForObject("select count(*) from data_table_asset", Integer.class);
        int fieldCount = jdbc.queryForObject("select count(*) from data_field_asset", Integer.class);
        int sensitiveCount = jdbc.queryForObject("select count(*) from data_field_asset where is_sensitive=1", Integer.class);
        String highFields = jdbc.queryForList("""
                select f.field_name, l.level_code, c.category_name from data_field_asset f
                join field_classification fc on fc.field_id=f.id
                join classification_level l on l.id=fc.level_id
                join classification_category c on c.id=fc.category_id
                where l.level_code in ('L4','L5') order by l.level_order desc, f.id
                """).stream().map(r -> r.get("FIELD_NAME") + "(" + r.get("LEVEL_CODE") + "," + r.get("CATEGORY_NAME") + ")").collect(Collectors.joining("、"));
        String alerts = jdbc.queryForList("select risk_type, risk_level, description from risk_alert order by created_time desc, alert_id desc limit 5")
                .stream().map(r -> "[" + r.get("RISK_LEVEL") + "]" + r.get("RISK_TYPE") + ":" + r.get("DESCRIPTION")).collect(Collectors.joining("\n"));
        return """
                数据源数量: %d
                数据表数量: %d
                字段总数: %d
                敏感字段数量: %d
                L4/L5字段: %s
                最近风险告警:
                %s
                """.formatted(sourceCount, tableCount, fieldCount, sensitiveCount, highFields.isBlank() ? "无" : highFields, alerts.isBlank() ? "无" : alerts);
    }
}
