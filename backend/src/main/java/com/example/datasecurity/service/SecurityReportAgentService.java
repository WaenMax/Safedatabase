package com.example.datasecurity.service;

import com.example.datasecurity.dto.SecurityReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityReportAgentService {
    private final JdbcTemplate jdbc;

    public SecurityReportDTO report() {
        int sources = count("select count(*) from data_source");
        int tables = count("select count(*) from data_table_asset");
        int fields = count("select count(*) from data_field_asset");
        int sensitive = count("select count(*) from data_field_asset where is_sensitive=1");
        int l3 = count("select count(*) from field_classification fc join classification_level l on l.id=fc.level_id where l.level_code='L3'");
        int l4 = count("select count(*) from field_classification fc join classification_level l on l.id=fc.level_id where l.level_code='L4'");
        int l5 = count("select count(*) from field_classification fc join classification_level l on l.id=fc.level_id where l.level_code='L5'");
        int highRiskRequests = count("select count(*) from agent_recommendation where recommendation_type='ACCESS_REVIEW' and risk_level='high'");
        int classified = count("select count(*) from field_classification");
        int policies = count("select count(*) from masking_policy where enabled=1");
        double coverage = fields == 0 ? 0 : classified * 100.0 / fields;
        double maskingCoverage = sensitive == 0 ? 100 : Math.min(100, policies * 25.0);
        String alerts = jdbc.queryForList("select top 5 risk_type, risk_level, description from risk_alert order by created_time desc, alert_id desc")
                .stream().map(a -> "- [" + a.get("RISK_LEVEL") + "] " + a.get("RISK_TYPE") + ": " + a.get("DESCRIPTION"))
                .reduce("", (a, b) -> a + b + "\n");
        String md = """
                # 数据安全治理 Agent 报告

                ## 资产概览
                - 数据源数量：%d
                - 数据表数量：%d
                - 字段总数：%d
                - 敏感字段数量：%d

                ## 分类分级情况
                - L3 敏感字段：%d
                - L4 高敏感字段：%d
                - L5 核心字段：%d
                - 分类分级覆盖率：%.2f%%
                - 脱敏策略覆盖率：%.2f%%

                ## 风险情况
                - 高风险访问申请数量：%d
                - 最近风险告警：
                %s
                ## 整改建议
                1. 优先复核 L4/L5 字段的授权范围和有效期。
                2. 对身份证、银行卡、密码密钥类字段保持强制脱敏展示。
                3. 每日执行审计日志风险分析，及时处理 OPEN/GENERATED 告警。
                4. 对未分类字段执行 Agent 分类建议并由安全管理员复核应用。
                """.formatted(sources, tables, fields, sensitive, l3, l4, l5, coverage, maskingCoverage, highRiskRequests, alerts.isBlank() ? "- 暂无风险告警\n" : alerts);
        return new SecurityReportDTO(md);
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
}
