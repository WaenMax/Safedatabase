package com.example.datasecurity.service;

import com.example.datasecurity.dto.AgentChatRequestDTO;
import com.example.datasecurity.dto.AgentChatResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentChatService {
    private final JdbcTemplate jdbc;
    private final SecurityReportAgentService reportService;
    private final LlmProviderService llmProviderService;

    // 简单的会话上下文：保留最近一次查询的意图和结果
    private final Map<Long, String[]> sessionContext = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, String[]> eldest) {
            return size() > 200;
        }
    };

    public AgentChatResponseDTO chat(AgentChatRequestDTO request, Long userId) {
        String q = request.getQuestion() == null ? "" : request.getQuestion();
        String provider = request.getProvider() == null || request.getProvider().isBlank() ? "local" : request.getProvider();

        // 外部模型优先路由
        if (llmProviderService.supports(provider)) {
            List<Map<String, Object>> references = buildReferences();
            try {
                String answer = llmProviderService.chat(provider, q, buildContext(references), request.getModel());
                jdbc.update("insert into agent_chat_history(user_id,question,answer,created_time) values(?,?,?,?)",
                        userId, "[" + provider + "] " + q, answer, LocalDateTime.now());
                sessionContext.put(userId, new String[]{q, "llm"});
                return new AgentChatResponseDTO(q, answer, provider, request.getModel(), references, defaultActions());
            } catch (Exception e) {
                AgentChatResponseDTO fallback = localChat(q, userId);
                fallback.setProvider("local-fallback");
                fallback.setModel(request.getModel());
                fallback.setAnswer(fallback.getAnswer() + "\n\n> ⚠️ 外部模型不可用，已自动回退本地规则。原因：" + e.getMessage());
                return fallback;
            }
        }

        // 本地规则推理
        AgentChatResponseDTO result = localChat(q, userId);
        sessionContext.put(userId, new String[]{q, "local"});
        return result;
    }

    private AgentChatResponseDTO localChat(String q, Long userId) {
        String lower = q.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> references = new ArrayList<>();
        List<Map<String, Object>> actions = new ArrayList<>();
        List<String> followups = new ArrayList<>();

        Intent intent = detectIntent(q, lower);

        String answer = switch (intent) {
            case HIGH_FIELDS -> handleHighFields(references, actions, followups);
            case FIELD_DETAIL -> handleFieldDetail(q, references, actions);
            case SENSITIVE_COUNT -> handleSensitiveCount(references, actions, followups);
            case RISK_ALERTS -> handleRiskAlerts(references, actions, followups);
            case UNCLASSIFIED_FIELDS -> handleUnclassifiedFields(references, actions, followups);
            case CLASSIFICATION_STATS -> handleClassificationStats(references, actions);
            case L4_ACCESS_USERS -> handleL4AccessUsers(references, actions, followups);
            case REJECTED_REQUESTS -> handleRejectedRequests(references, actions, followups);
            case PENDING_APPROVALS -> handlePendingApprovals(references, actions, followups);
            case RECENT_OPERATIONS -> handleRecentOperations(references, actions);
            case ACCESS_STATS -> handleAccessStats(references, actions);
            case SOURCE_TABLE_SUMMARY -> handleSourceTableSummary(references, actions);
            case DASHBOARD_SUMMARY -> handleDashboardSummary(references, actions);
            case REPORT -> handleReport(references, actions, followups);
            default -> handleUnknown(references, actions, followups, q, lower);
        };

        // 追加快捷追问
        if (!followups.isEmpty()) {
            answer += "\n\n**💡 你可能还想问：**";
            for (String f : followups) {
                answer += "\n- " + f;
            }
        }

        jdbc.update("insert into agent_chat_history(user_id,question,answer,created_time) values(?,?,?,?)",
                userId, q, answer, LocalDateTime.now());
        return new AgentChatResponseDTO(q, answer, "local", null, references, actions);
    }

    // ──────────── 意图处理器 ────────────

    private String handleHighFields(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select f.id field_id, f.field_name, f.field_comment, l.level_code, c.category_name, t.table_name
                from data_field_asset f
                join field_classification fc on fc.field_id = f.id
                join classification_level l on l.id = fc.level_id
                join classification_category c on c.id = fc.category_id
                left join data_table_asset t on t.id = f.table_id
                where l.level_code in ('L4','L5')
                order by l.level_order desc, f.id
                """);
        refs.add(rowReference("L4/L5 高敏字段", "field_classification + classification_level", rows));
        actions.add(action("查看字段资产", "/fields"));
        followups.add("哪些字段还没有完成分类？");
        followups.add("最近有哪些风险告警？");
        if (rows.isEmpty()) return "✅ 当前系统没有 L4/L5 高敏感字段。";
        StringBuilder sb = new StringBuilder("### 🔴 高敏感字段清单\n\n");
        sb.append("| 字段名 | 等级 | 分类 | 所属表 | 说明 |\n");
        sb.append("|--------|------|------|--------|------|\n");
        for (Map<String, Object> r : rows) {
            sb.append(String.format("| %s | **%s** | %s | %s | %s |\n",
                    r.get("FIELD_NAME"), r.get("LEVEL_CODE"), r.get("CATEGORY_NAME"),
                    r.getOrDefault("TABLE_NAME", "-"), r.getOrDefault("FIELD_COMMENT", "-")));
        }
        sb.append("\n共 ").append(rows.size()).append(" 个 L4/L5 字段，访问原始值需要审批。");
        return sb.toString();
    }

    private String handleFieldDetail(String q, List<Map<String, Object>> refs, List<Map<String, Object>> actions) {
        // 从问题中提取字段名关键词
        List<Map<String, Object>> fields = jdbc.queryForList("""
                select f.id field_id, f.field_name, f.field_type, f.field_comment, f.sample_value,
                       f.is_sensitive, t.table_name, s.source_name,
                       c.category_name, l.level_code, l.level_name
                from data_field_asset f
                left join data_table_asset t on t.id = f.table_id
                left join data_source s on s.id = t.source_id
                left join field_classification fc on fc.field_id = f.id
                left join classification_category c on c.id = fc.category_id
                left join classification_level l on l.id = fc.level_id
                order by f.id
                """);
        // 按问题关键词匹配——在字段名、注释、表名中查找
        String query = q.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> matched = fields.stream()
                .filter(f -> {
                    String hay = (String.valueOf(f.get("FIELD_NAME")) + " "
                            + String.valueOf(f.getOrDefault("FIELD_COMMENT", "")) + " "
                            + String.valueOf(f.getOrDefault("TABLE_NAME", ""))).toLowerCase(Locale.ROOT);
                    return Arrays.stream(query.split("[\\s，。？?、]+"))
                            .anyMatch(w -> w.length() >= 2 && hay.contains(w));
                })
                .limit(8).toList();
        refs.add(rowReference("匹配字段", "data_field_asset", matched));
        actions.add(action("查看字段资产", "/fields"));
        if (matched.isEmpty()) return "未找到与「" + q + "」相关的字段。请尝试使用具体字段名查询。";
        StringBuilder sb = new StringBuilder("### 🔍 字段查询结果\n\n");
        for (Map<String, Object> f : matched) {
            sb.append(String.format("- **%s** (%s) — %s / %s — 表 %s — %s\n",
                    f.get("FIELD_NAME"),
                    f.getOrDefault("LEVEL_CODE", "未分级"),
                    f.getOrDefault("CATEGORY_NAME", "未分类"),
                    f.get("FIELD_TYPE"),
                    f.getOrDefault("TABLE_NAME", "-"),
                    f.getOrDefault("FIELD_COMMENT", "")));
        }
        return sb.toString();
    }

    private String handleSensitiveCount(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        Integer total = count("select count(*) from data_field_asset");
        Integer sensitive = count("select count(*) from data_field_asset where is_sensitive=1");
        Integer l3 = levelCount("L3"), l4 = levelCount("L4"), l5 = levelCount("L5");
        Integer unclassified = count("""
                select count(*) from data_field_asset f
                left join field_classification fc on fc.field_id = f.id where fc.id is null
                """);
        refs.add(reference("字段总数", "data_field_asset", total));
        refs.add(reference("敏感字段数", "data_field_asset (is_sensitive=1)", sensitive));
        actions.add(action("查看字段资产", "/fields"));
        followups.add("高敏感字段有哪些？");
        followups.add("哪些字段还没有分类？");
        return String.format("""
                ### 📊 字段敏感度统计

                | 指标 | 数量 |
                |------|------|
                | 字段总数 | %d |
                | 敏感字段 | %d |
                | L3 敏感 | %d |
                | L4 高敏 | %d |
                | L5 核心 | %d |
                | 未分类 | %d |
                """, total, sensitive, l3, l4, l5, unclassified);
    }

    private String handleRiskAlerts(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select alert_id, risk_type, risk_level, description, suggestion, status, created_time from risk_alert order by created_time desc limit 8");
        refs.add(rowReference("最近风险告警", "risk_alert", rows));
        actions.add(action("查看风险告警", "/agent/risk-alerts"));
        followups.add("最近有哪些高风险访问行为？");
        followups.add("生成一份安全治理建议");
        if (rows.isEmpty()) return "✅ 当前系统暂无风险告警，治理状态良好。";
        StringBuilder sb = new StringBuilder("### ⚠️ 最近风险告警\n\n");
        for (Map<String, Object> r : rows) {
            sb.append(String.format("- **[%s]** %s — %s\n  > 建议: %s  `%s`\n",
                    r.get("RISK_LEVEL"), r.get("RISK_TYPE"),
                    r.get("DESCRIPTION"), r.get("SUGGESTION"), r.get("STATUS")));
        }
        return sb.toString();
    }

    private String handleUnclassifiedFields(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select f.id field_id, f.field_name, f.field_type, f.field_comment, t.table_name
                from data_field_asset f
                left join data_table_asset t on t.id = f.table_id
                left join field_classification fc on fc.field_id = f.id
                where fc.id is null order by f.id
                """);
        refs.add(rowReference("未分类字段", "data_field_asset", rows));
        actions.add(action("进入字段智能分类", "/agent/field-classify"));
        followups.add("帮我分类这些字段");
        if (rows.isEmpty()) return "✅ 所有字段都已完成分类分级。";
        StringBuilder sb = new StringBuilder("### 📝 未分类字段\n\n");
        for (Map<String, Object> r : rows) {
            sb.append(String.format("- **%s** (%s) — %s — 表 %s\n",
                    r.get("FIELD_NAME"), r.get("FIELD_TYPE"),
                    r.getOrDefault("FIELD_COMMENT", "-"), r.getOrDefault("TABLE_NAME", "-")));
        }
        sb.append("\n共 ").append(rows.size()).append(" 个字段等待分类，建议进入字段智能分类页面处理。");
        return sb.toString();
    }

    private String handleClassificationStats(List<Map<String, Object>> refs, List<Map<String, Object>> actions) {
        List<Map<String, Object>> levelStats = jdbc.queryForList("""
                select l.level_code, l.level_name, count(fc.id) cnt
                from classification_level l
                left join field_classification fc on fc.level_id = l.id
                group by l.level_code, l.level_name, l.level_order order by l.level_order
                """);
        List<Map<String, Object>> catStats = jdbc.queryForList("""
                select c.category_name, count(fc.id) cnt
                from classification_category c
                left join field_classification fc on fc.category_id = c.id
                group by c.category_name order by c.category_name
                """);
        refs.add(rowReference("分级统计", "classification_level", levelStats));
        refs.add(rowReference("分类统计", "classification_category", catStats));
        actions.add(action("查看分类分级", "/classifications"));
        StringBuilder sb = new StringBuilder("### 📈 分类分级统计\n\n**按等级分布：**\n");
        for (Map<String, Object> r : levelStats) {
            sb.append(String.format("- %s (%s): %s 个字段\n", r.get("LEVEL_CODE"), r.get("LEVEL_NAME"), r.get("CNT")));
        }
        sb.append("\n**按分类分布：**\n");
        for (Map<String, Object> r : catStats) {
            sb.append(String.format("- %s: %s 个字段\n", r.get("CATEGORY_NAME"), r.get("CNT")));
        }
        return sb.toString();
    }

    private String handleL4AccessUsers(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select distinct u.id user_id, u.username, count(a.id) access_count
                from audit_log a join sys_user u on u.id = a.user_id
                join field_classification fc on fc.field_id = a.target_id
                join classification_level l on l.id = fc.level_id
                where l.level_code in ('L4','L5') and a.operation_type = 'VIEW_RAW_VALUE'
                group by u.id, u.username order by access_count desc
                """);
        refs.add(rowReference("访问过 L4/L5 的用户", "audit_log", rows));
        actions.add(action("查看审计日志", "/audit"));
        followups.add("最近有哪些高风险访问行为？");
        if (rows.isEmpty()) return "📋 审计日志中暂无用户访问 L4/L5 高敏字段的记录。";
        StringBuilder sb = new StringBuilder("### 👤 高敏字段访问用户\n\n");
        for (Map<String, Object> r : rows) {
            sb.append(String.format("- **%s** — %s 次 L4/L5 访问\n", r.get("USERNAME"), r.get("ACCESS_COUNT")));
        }
        return sb.toString();
    }

    private String handleRejectedRequests(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select ar.id, u.username, f.field_name, ar.reason, ar.status, l.level_code
                from access_request ar
                join sys_user u on u.id = ar.user_id
                join data_field_asset f on f.id = ar.field_id
                left join field_classification fc on fc.field_id = f.id
                left join classification_level l on l.id = fc.level_id
                where ar.status in ('REJECTED','PENDING')
                order by ar.id desc limit 10
                """);
        refs.add(rowReference("访问申请记录", "access_request", rows));
        actions.add(action("查看审批管理", "/approvals"));
        followups.add("当前有几个待审批申请？");
        if (rows.isEmpty()) return "✅ 没有被驳回或待审批的访问申请。";
        StringBuilder sb = new StringBuilder("### 📋 访问申请状态\n\n");
        for (Map<String, Object> r : rows) {
            sb.append(String.format("- 申请#%d **%s** 申请查看 **%s** (%s) — `%s`\n",
                    r.get("ID"), r.get("USERNAME"), r.get("FIELD_NAME"),
                    r.getOrDefault("LEVEL_CODE", "L1"), r.get("STATUS")));
        }
        return sb.toString();
    }

    private String handlePendingApprovals(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        Integer pending = count("select count(*) from access_request where status='PENDING'");
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select ar.id, u.username, f.field_name, l.level_code, ar.reason, ar.request_time
                from access_request ar
                join sys_user u on u.id = ar.user_id
                join data_field_asset f on f.id = ar.field_id
                left join field_classification fc on fc.field_id = f.id
                left join classification_level l on l.id = fc.level_id
                where ar.status = 'PENDING' order by ar.request_time desc limit 8
                """);
        refs.add(rowReference("待审批申请", "access_request", rows));
        actions.add(action("进入审批管理", "/approvals"));
        followups.add("帮我分析这些申请的风险");
        if (pending == 0) return "✅ 当前没有待审批的访问申请。";
        StringBuilder sb = new StringBuilder("### ⏳ 待审批申请（共 " + pending + " 个）\n\n");
        for (Map<String, Object> r : rows) {
            sb.append(String.format("- 申请#%d — **%s** 申请查看 **%s** (%s)\n  > 原因: %s\n",
                    r.get("ID"), r.get("USERNAME"), r.get("FIELD_NAME"),
                    r.getOrDefault("LEVEL_CODE", "L1"), r.get("REASON")));
        }
        return sb.toString();
    }

    private String handleRecentOperations(List<Map<String, Object>> refs, List<Map<String, Object>> actions) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select a.operation_type, a.result, a.operation_time, u.username, a.detail
                from audit_log a left join sys_user u on u.id = a.user_id
                order by a.operation_time desc limit 15
                """);
        refs.add(rowReference("最近操作", "audit_log", rows));
        actions.add(action("查看审计日志", "/audit"));
        if (rows.isEmpty()) return "📋 暂无操作记录。";
        StringBuilder sb = new StringBuilder("### 🕐 最近系统操作\n\n");
        for (Map<String, Object> r : rows) {
            sb.append(String.format("- `%s` **%s** %s — %s\n",
                    r.get("OPERATION_TIME"), r.get("USERNAME"),
                    r.get("OPERATION_TYPE"), r.getOrDefault("DETAIL", "")));
        }
        return sb.toString();
    }

    private String handleAccessStats(List<Map<String, Object>> refs, List<Map<String, Object>> actions) {
        Integer totalRequests = count("select count(*) from access_request");
        Integer pending = count("select count(*) from access_request where status='PENDING'");
        Integer approved = count("select count(*) from access_request where status='APPROVED'");
        Integer rejected = count("select count(*) from access_request where status='REJECTED'");
        refs.add(reference("访问申请统计", "access_request", totalRequests));
        actions.add(action("查看审批管理", "/approvals"));
        return String.format("""
                ### 📊 访问申请统计

                | 状态 | 数量 |
                |------|------|
                | 待审批 | %d |
                | 已通过 | %d |
                | 已驳回 | %d |
                | **合计** | **%d** |
                """, pending, approved, rejected, totalRequests);
    }

    private String handleSourceTableSummary(List<Map<String, Object>> refs, List<Map<String, Object>> actions) {
        List<Map<String, Object>> sources = jdbc.queryForList("""
                select s.source_name, count(distinct t.id) table_count, count(f.id) field_count
                from data_source s
                left join data_table_asset t on t.source_id = s.id
                left join data_field_asset f on f.table_id = t.id
                group by s.id, s.source_name order by s.id
                """);
        refs.add(rowReference("数据源概览", "data_source + data_table_asset", sources));
        actions.add(action("查看数据源", "/sources"));
        StringBuilder sb = new StringBuilder("### 🗄️ 数据资产概览\n\n");
        for (Map<String, Object> r : sources) {
            sb.append(String.format("- **%s** — %s 张表，%s 个字段\n",
                    r.get("SOURCE_NAME"), r.get("TABLE_COUNT"), r.get("FIELD_COUNT")));
        }
        return sb.toString();
    }

    private String handleDashboardSummary(List<Map<String, Object>> refs, List<Map<String, Object>> actions) {
        int sources = count("select count(*) from data_source");
        int tables = count("select count(*) from data_table_asset");
        int fields = count("select count(*) from data_field_asset");
        int sensitive = count("select count(*) from data_field_asset where is_sensitive=1");
        int pending = count("select count(*) from access_request where status='PENDING'");
        int alerts = count("select count(*) from risk_alert where status in ('OPEN','GENERATED')");
        refs.addAll(buildReferences());
        actions.add(action("打开首页看板", "/"));
        return String.format("""
                ### 🏠 系统治理概览

                | 指标 | 数值 |
                |------|------|
                | 数据源 | %d |
                | 数据表 | %d |
                | 字段总数 | %d |
                | 敏感字段 | %d |
                | 待审批申请 | %d |
                | 未处理风险告警 | %d |

                输入具体问题可深入查看详情。""", sources, tables, fields, sensitive, pending, alerts);
    }

    private String handleReport(List<Map<String, Object>> refs, List<Map<String, Object>> actions, List<String> followups) {
        String md = reportService.report().getMarkdown();
        refs.addAll(buildReferences());
        actions.add(action("打开安全报告", "/agent/security-report"));
        followups.add("当前系统的敏感字段数量是多少？");
        followups.add("最近有哪些风险告警？");
        return md;
    }

    private String handleUnknown(List<Map<String, Object>> refs, List<Map<String, Object>> actions,
                                  List<String> followups, String q, String lower) {
        refs.addAll(buildReferences());
        actions.addAll(defaultActions());

        // 尝试用外部 LLM 兜底
        try {
            if (llmProviderService.supports("deepseek") || llmProviderService.supports("siliconflow")) {
                String provider = llmProviderService.supports("deepseek") ? "deepseek" : "siliconflow";
                String context = buildContext(buildReferences());
                String llmAnswer = llmProviderService.chat(provider, q, context, null);
                return "### 🤖 AI 分析结果\n\n" + llmAnswer + "\n\n> 该问题超出本地规则引擎范围，已通过外部模型辅助回答。";
            }
        } catch (Exception ignored) {
            // 外部模型不可用，继续返回本地引导
        }

        // 提示用户可问的问题类型
        StringBuilder sb = new StringBuilder("### 🤔 我可以帮你解答以下问题\n\n");
        sb.append("**数据资产：**\n");
        sb.append("- 「有哪些高敏感字段？」「字段总数是多少？」\n");
        sb.append("- 「phone 字段的详细信息」「数据源概览」\n\n");
        sb.append("**分类分级：**\n");
        sb.append("- 「哪些字段还没分类？」「分类分级统计情况」\n\n");
        sb.append("**访问审批：**\n");
        sb.append("- 「有几个待审批申请？」「访问申请统计」\n");
        sb.append("- 「最近被驳回的申请有哪些？」\n\n");
        sb.append("**审计风险：**\n");
        sb.append("- 「最近有哪些风险告警？」「最近谁访问过高敏数据？」\n");
        sb.append("- 「最近系统操作记录」「生成安全治理报告」\n\n");
        sb.append("**全局概览：**\n");
        sb.append("- 「系统概览」「当前有哪些高风险问题？」\n");
        sb.append("\n或者切换 **DeepSeek / 硅基流动** 通道获得更灵活的回答。");

        followups.add("有哪些高敏感字段？");
        followups.add("当前有几个待审批申请？");
        followups.add("生成一份安全治理建议");
        return sb.toString();
    }

    // ──────────── 意图识别（评分制） ────────────

    private Intent detectIntent(String q, String lower) {
        // 优先级评分：更具体的匹配得分更高
        int bestScore = 0;
        Intent best = Intent.UNKNOWN;

        best = updateIntent(best, lower, "字段总数", "敏感字段数量", "多少个字段", "多少字段", 8, Intent.SENSITIVE_COUNT, bestScore);
        if (getScore(lower, "字段总数", "敏感字段数量", "多少个字段", "多少字段") > bestScore) {
            bestScore = getScore(lower, "字段总数", "敏感字段数量", "多少个字段", "多少字段");
            best = Intent.SENSITIVE_COUNT;
        }

        best = updateIntent(best, lower, "高敏感", "高敏字段", "l4", "l5", "核心字段", 8, Intent.HIGH_FIELDS, bestScore);
        if (getScore(lower, "高敏感", "高敏字段", "l4", "l5", "核心字段") > bestScore) {
            bestScore = getScore(lower, "高敏感", "高敏字段", "l4", "l5", "核心字段");
            best = Intent.HIGH_FIELDS;
        }

        best = updateIntent(best, lower, "字段", "field", "是什么", "详情", 5, Intent.FIELD_DETAIL, bestScore);
        if (getScore(lower, "字段", "field", "是什么", "详情") > bestScore) {
            bestScore = getScore(lower, "字段", "field", "是什么", "详情");
            best = Intent.FIELD_DETAIL;
        }

        best = updateIntent(best, lower, "风险告警", "高风险行为", "风险", 7, Intent.RISK_ALERTS, bestScore);
        if (getScore(lower, "风险告警", "高风险行为", "风险") > bestScore) {
            bestScore = getScore(lower, "风险告警", "高风险行为", "风险");
            best = Intent.RISK_ALERTS;
        }

        best = updateIntent(best, lower, "没有分类", "未分类", "还没分类", 9, Intent.UNCLASSIFIED_FIELDS, bestScore);
        if (getScore(lower, "没有分类", "未分类", "还没分类") > bestScore) {
            bestScore = getScore(lower, "没有分类", "未分类", "还没分类");
            best = Intent.UNCLASSIFIED_FIELDS;
        }

        best = updateIntent(best, lower, "分类统计", "分级统计", "分类分布", "分级分布", "等级分布", 6, Intent.CLASSIFICATION_STATS, bestScore);
        if (getScore(lower, "分类统计", "分级统计", "分类分布", "分级分布", "等级分布") > bestScore) {
            bestScore = getScore(lower, "分类统计", "分级统计", "分类分布", "分级分布", "等级分布");
            best = Intent.CLASSIFICATION_STATS;
        }

        best = updateIntent(best, lower, "访问过", "访问高敏", "谁访问", 6, Intent.L4_ACCESS_USERS, bestScore);
        if (getScore(lower, "访问过", "访问高敏", "谁访问") > bestScore) {
            bestScore = getScore(lower, "访问过", "访问高敏", "谁访问");
            best = Intent.L4_ACCESS_USERS;
        }

        best = updateIntent(best, lower, "被拒绝", "rejected", "驳回", "拒绝", 8, Intent.REJECTED_REQUESTS, bestScore);
        if (getScore(lower, "被拒绝", "rejected", "驳回", "拒绝") > bestScore) {
            bestScore = getScore(lower, "被拒绝", "rejected", "驳回", "拒绝");
            best = Intent.REJECTED_REQUESTS;
        }

        best = updateIntent(best, lower, "待审批", "等待审批", "pending", "审批", 7, Intent.PENDING_APPROVALS, bestScore);
        if (getScore(lower, "待审批", "等待审批", "pending", "审批") > bestScore) {
            bestScore = getScore(lower, "待审批", "等待审批", "pending", "审批");
            best = Intent.PENDING_APPROVALS;
        }

        best = updateIntent(best, lower, "最近操作", "最近活动", "操作记录", "最近谁", 8, Intent.RECENT_OPERATIONS, bestScore);
        if (getScore(lower, "最近操作", "最近活动", "操作记录", "最近谁") > bestScore) {
            bestScore = getScore(lower, "最近操作", "最近活动", "操作记录", "最近谁");
            best = Intent.RECENT_OPERATIONS;
        }

        best = updateIntent(best, lower, "访问统计", "申请统计", "有多少申请", 7, Intent.ACCESS_STATS, bestScore);
        if (getScore(lower, "访问统计", "申请统计", "有多少申请") > bestScore) {
            bestScore = getScore(lower, "访问统计", "申请统计", "有多少申请");
            best = Intent.ACCESS_STATS;
        }

        best = updateIntent(best, lower, "数据源", "数据库列表", "数据表概览", "资产概览", 7, Intent.SOURCE_TABLE_SUMMARY, bestScore);
        if (getScore(lower, "数据源", "数据库列表", "数据表概览", "资产概览") > bestScore) {
            bestScore = getScore(lower, "数据源", "数据库列表", "数据表概览", "资产概览");
            best = Intent.SOURCE_TABLE_SUMMARY;
        }

        best = updateIntent(best, lower, "系统概览", "全局概览", "治理概览", "首页", "dashboard", "总体情况", 6, Intent.DASHBOARD_SUMMARY, bestScore);
        if (getScore(lower, "系统概览", "全局概览", "治理概览", "首页", "dashboard", "总体情况") > bestScore) {
            bestScore = getScore(lower, "系统概览", "全局概览", "治理概览", "首页", "dashboard", "总体情况");
            best = Intent.DASHBOARD_SUMMARY;
        }

        best = updateIntent(best, lower, "治理建议", "报告", "整改", "安全报告", 10, Intent.REPORT, bestScore);
        if (getScore(lower, "治理建议", "报告", "整改", "安全报告") > bestScore) {
            best = Intent.REPORT;
        }

        return best;
    }

    private Intent updateIntent(Intent current, String lower, String... keywords) {
        return current; // no-op, the real logic is in the if block above
    }

    private int getScore(String lower, String... keywords) {
        int score = 0;
        for (String kw : keywords) {
            if (lower.contains(kw)) score += kw.length();
        }
        return score;
    }

    // ──────────── 辅助方法 ────────────

    private String buildContext(List<Map<String, Object>> references) {
        int sourceCount = count("select count(*) from data_source");
        int tableCount = count("select count(*) from data_table_asset");
        int fieldCount = count("select count(*) from data_field_asset");
        int sensitiveCount = count("select count(*) from data_field_asset where is_sensitive=1");
        int pendingCount = count("select count(*) from access_request where status='PENDING'");
        int alertCount = count("select count(*) from risk_alert where status in ('OPEN','GENERATED')");
        String highFields = jdbc.queryForList("""
                select f.field_name, l.level_code, c.category_name from data_field_asset f
                join field_classification fc on fc.field_id = f.id
                join classification_level l on l.id = fc.level_id
                join classification_category c on c.id = fc.category_id
                where l.level_code in ('L4','L5') order by l.level_order desc, f.id
                """).stream().map(r -> r.get("FIELD_NAME") + "(" + r.get("LEVEL_CODE") + "," + r.get("CATEGORY_NAME") + ")")
                .collect(Collectors.joining("、"));
        String alerts = jdbc.queryForList(
                        "select risk_type, risk_level, description from risk_alert order by created_time desc limit 5")
                .stream().map(r -> "[" + r.get("RISK_LEVEL") + "]" + r.get("RISK_TYPE") + ":" + r.get("DESCRIPTION"))
                .collect(Collectors.joining("\n"));
        return String.format("""
                数据源数量: %d
                数据表数量: %d
                字段总数: %d
                敏感字段数量: %d
                待审批申请: %d
                未处理告警: %d
                L4/L5字段: %s
                最近风险告警:
                %s
                """, sourceCount, tableCount, fieldCount, sensitiveCount, pendingCount, alertCount,
                highFields.isBlank() ? "无" : highFields, alerts.isBlank() ? "无" : alerts);
    }

    private List<Map<String, Object>> buildReferences() {
        return List.of(
                reference("资产统计", "data_source / data_table_asset / data_field_asset",
                        count("select count(*) from data_field_asset")),
                reference("L4/L5 字段", "field_classification + classification_level",
                        count("select count(*) from field_classification fc join classification_level l on l.id=fc.level_id where l.level_code in ('L4','L5')")),
                reference("待审批申请", "access_request",
                        count("select count(*) from access_request where status='PENDING'")),
                reference("风险告警", "risk_alert",
                        count("select count(*) from risk_alert where status in ('OPEN','GENERATED')"))
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
        return List.of(
                action("查看风险告警", "/agent/risk-alerts"),
                action("查看审计日志", "/audit"),
                action("查看字段资产", "/fields")
        );
    }

    private int count(String sql) {
        Integer result = jdbc.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }

    private int levelCount(String code) {
        Integer result = jdbc.queryForObject(
                "select count(*) from field_classification fc join classification_level l on l.id=fc.level_id where l.level_code=?",
                Integer.class, code);
        return result == null ? 0 : result;
    }

    private enum Intent {
        HIGH_FIELDS, FIELD_DETAIL, SENSITIVE_COUNT, RISK_ALERTS, UNCLASSIFIED_FIELDS,
        CLASSIFICATION_STATS, L4_ACCESS_USERS, REJECTED_REQUESTS, PENDING_APPROVALS,
        RECENT_OPERATIONS, ACCESS_STATS, SOURCE_TABLE_SUMMARY, DASHBOARD_SUMMARY,
        REPORT, UNKNOWN
    }
}
