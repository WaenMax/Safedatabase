package com.example.datasecurity.service;

import com.example.datasecurity.dto.AgentChatRequestDTO;
import com.example.datasecurity.dto.AgentChatResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            List<Map<String, Object>> references = buildReferences();
            try {
                String answer = llmProviderService.chat(provider, q, buildContext(references), request.getModel());
                jdbc.update("insert into agent_chat_history(user_id,question,answer,created_time) values(?,?,?,?)", userId, "[" + provider + "] " + q, answer, LocalDateTime.now());
                return new AgentChatResponseDTO(q, answer, provider, request.getModel(), references, defaultActions());
            } catch (Exception e) {
                AgentChatResponseDTO fallback = localChat(q, userId);
                fallback.setProvider("local-fallback");
                fallback.setModel(request.getModel());
                fallback.setAnswer(fallback.getAnswer() + "\n\n外部模型不可用，已自动回退本地规则。原因：" + e.getMessage());
                return fallback;
            }
        }
        return localChat(q, userId);
    }

    private AgentChatResponseDTO localChat(String q, Long userId) {
        String lower = q.toLowerCase(Locale.ROOT);
        String answer;
        List<Map<String, Object>> references = new ArrayList<>();
        List<Map<String, Object>> actions = new ArrayList<>();
        Intent intent = detectIntent(q, lower);
        if (intent == Intent.HIGH_FIELDS) {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    select f.id field_id, f.field_name, l.level_code, c.category_name from data_field_asset f
                    join field_classification fc on fc.field_id=f.id
                    join classification_level l on l.id=fc.level_id
                    join classification_category c on c.id=fc.category_id
                    where l.level_code in ('L4','L5') order by l.level_order desc, f.id
                    """);
            references.add(rowReference("L4/L5 高敏字段", "field_classification + classification_level", rows));
            actions.add(action("查看字段资产", "/fields"));
            answer = rows.stream().map(r -> r.get("FIELD_NAME") + "(" + r.get("LEVEL_CODE") + ")").collect(Collectors.joining("、"));
            answer = answer.isBlank() ? "当前没有 L4/L5 高敏感字段。" : "高敏感字段包括：" + answer + "。";
        } else if (intent == Intent.L4_ACCESS_USERS) {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    select distinct u.id user_id, u.username from audit_log a join sys_user u on u.id=a.user_id
                    join field_classification fc on fc.field_id=a.target_id
                    join classification_level l on l.id=fc.level_id
                    where l.level_code='L4'
                    """);
            references.add(rowReference("访问过 L4 的用户", "audit_log + field_classification", rows));
            actions.add(action("查看审计日志", "/audit"));
            answer = rows.stream().map(r -> String.valueOf(r.get("USERNAME"))).collect(Collectors.joining("、"));
            answer = answer.isBlank() ? "最近审计日志中没有用户访问 L4 字段。" : "访问过 L4 数据的用户：" + answer + "。";
        } else if (intent == Intent.RISK_ALERTS) {
            List<Map<String, Object>> rows = jdbc.queryForList("select top 5 alert_id, risk_type, risk_level, description from risk_alert order by created_time desc, alert_id desc");
            references.add(rowReference("最近风险告警", "risk_alert", rows));
            actions.add(action("查看风险告警", "/agent/risk-alerts"));
            answer = rows.stream().map(r -> "[" + r.get("RISK_LEVEL") + "] " + r.get("DESCRIPTION")).collect(Collectors.joining("\n"));
            answer = answer.isBlank() ? "当前暂无风险告警。" : answer;
        } else if (intent == Intent.UNCLASSIFIED_FIELDS) {
            List<Map<String, Object>> rows = jdbc.queryForList("select f.id field_id, field_name from data_field_asset f left join field_classification fc on fc.field_id=f.id where fc.id is null");
            references.add(rowReference("未分类字段", "data_field_asset left join field_classification", rows));
            actions.add(action("进入字段智能分类", "/agent/field-classify"));
            answer = rows.stream().map(r -> String.valueOf(r.get("FIELD_NAME"))).collect(Collectors.joining("、"));
            answer = answer.isBlank() ? "所有字段都已经完成分类分级。" : "未分类字段：" + answer + "。";
        } else if (intent == Intent.REJECTED_REQUESTS) {
            List<Map<String, Object>> rows = jdbc.queryForList("select id, reason from access_request where status='REJECTED' order by id desc");
            references.add(rowReference("被拒绝访问申请", "access_request", rows));
            actions.add(action("查看访问申请", "/requests"));
            answer = rows.stream().map(r -> "申请#" + r.get("ID") + ": " + r.get("REASON")).collect(Collectors.joining("\n"));
            answer = answer.isBlank() ? "当前没有被拒绝的访问申请。" : answer;
        } else if (intent == Intent.SENSITIVE_COUNT) {
            Integer count = jdbc.queryForObject("select count(*) from data_field_asset where is_sensitive=1", Integer.class);
            references.add(reference("敏感字段数量", "data_field_asset", count));
            answer = "当前系统的敏感字段数量是 " + count + " 个。";
            actions.add(action("查看字段资产", "/fields"));
        } else if (intent == Intent.REPORT) {
            answer = reportService.report().getMarkdown();
            references.addAll(buildReferences());
            actions.add(action("打开安全报告", "/agent/security-report"));
        } else {
            answer = "我可以回答高敏感字段、L4 访问用户、风险告警、未分类字段、被拒绝申请、敏感字段数量，并生成安全治理建议。";
            references.addAll(buildReferences());
            actions.addAll(defaultActions());
        }
        jdbc.update("insert into agent_chat_history(user_id,question,answer,created_time) values(?,?,?,?)", userId, q, answer, LocalDateTime.now());
        return new AgentChatResponseDTO(q, answer, "local", null, references, actions);
    }

    private String buildContext(List<Map<String, Object>> references) {
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

    private List<Map<String, Object>> buildReferences() {
        return List.of(
                reference("资产统计", "data_source / data_table_asset / data_field_asset", jdbc.queryForObject("select count(*) from data_field_asset", Integer.class)),
                reference("L4/L5 字段", "field_classification + classification_level", jdbc.queryForObject("""
                        select count(*) from field_classification fc join classification_level l on l.id=fc.level_id
                        where l.level_code in ('L4','L5')
                        """, Integer.class)),
                reference("最近风险告警", "risk_alert", jdbc.queryForObject("select count(*) from risk_alert", Integer.class))
        );
    }

    private Map<String, Object> reference(String title, String source, Object count) {
        return Map.of("title", title, "source", source, "count", count == null ? 0 : count);
    }

    private Map<String, Object> rowReference(String title, String source, List<Map<String, Object>> rows) {
        return Map.of("title", title, "source", source, "count", rows.size(), "rows", rows);
    }

    private Map<String, Object> action(String label, String path) {
        return Map.of("label", label, "path", path);
    }

    private List<Map<String, Object>> defaultActions() {
        return List.of(action("查看风险告警", "/agent/risk-alerts"), action("查看审批管理", "/approvals"), action("查看字段资产", "/fields"));
    }

    private Intent detectIntent(String q, String lower) {
        if (q.contains("访问过") && q.contains("L4")) return Intent.L4_ACCESS_USERS;
        if (q.contains("风险告警") || q.contains("高风险") || q.contains("风险")) return Intent.RISK_ALERTS;
        if (q.contains("没有分类") || q.contains("未分类")) return Intent.UNCLASSIFIED_FIELDS;
        if (q.contains("被拒绝") || lower.contains("rejected") || q.contains("驳回")) return Intent.REJECTED_REQUESTS;
        if (q.contains("敏感字段数量")) return Intent.SENSITIVE_COUNT;
        if (q.contains("治理建议") || q.contains("报告") || q.contains("整改")) return Intent.REPORT;
        if (q.contains("高敏感") || q.contains("L4") || q.contains("L5") || q.contains("高敏")) return Intent.HIGH_FIELDS;
        return Intent.UNKNOWN;
    }

    private enum Intent {
        HIGH_FIELDS, L4_ACCESS_USERS, RISK_ALERTS, UNCLASSIFIED_FIELDS, REJECTED_REQUESTS, SENSITIVE_COUNT, REPORT, UNKNOWN
    }
}
