# 数据库设计说明

## 设计概述

系统围绕数据资产、分类分级、安全访问和审计留痕设计。`data_source`、`data_table_asset`、`data_field_asset` 构成数据资产目录；`classification_category`、`classification_level`、`classification_rule`、`field_classification` 构成分类分级模型；`access_request`、`approval_record` 支撑高敏字段访问审批；`audit_log` 记录关键操作。

## 表说明

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| sys_user | 系统用户 | username、password、enabled |
| sys_role | 角色字典 | role_code、role_name |
| sys_permission | 权限字典 | permission_code、permission_name |
| sys_user_role | 用户角色关联 | user_id、role_id |
| sys_role_permission | 角色权限关联 | role_id、permission_id |
| data_source | 数据源资产 | source_name、source_type、host、database_name |
| data_table_asset | 数据表资产 | source_id、table_name、business_name |
| data_field_asset | 字段资产 | table_id、field_name、field_type、sample_value、is_sensitive |
| classification_category | 分类字典 | category_name、description |
| classification_level | 分级字典 | level_code、level_order |
| classification_rule | 自动识别规则 | match_type、match_pattern、category_id、level_id |
| field_classification | 字段分类结果 | field_id、category_id、level_id、classify_method |
| masking_policy | 脱敏策略 | policy_type、example_before、example_after |
| access_request | 访问申请 | user_id、field_id、reason、status、valid_until |
| approval_record | 审批记录 | request_id、approver_id、approval_result |
| audit_log | 审计日志 | user_id、operation_type、target_type、operation_time、result |
| agent_task | Agent 任务 | task_type、task_status、input_data、output_data |
| agent_recommendation | Agent 建议 | recommendation_type、target_type、target_id、confidence、reason |
| risk_alert | 风险告警 | risk_type、risk_level、user_id、description、status |
| agent_chat_history | Agent 问答历史 | user_id、question、answer、created_time |

## 表关系

- 一个数据源包含多张数据表，一张数据表包含多个字段。
- 一个字段最多对应一条当前分类分级结果。
- 分类规则关联分类和分级，用于自动识别字段。
- 普通用户访问 L4/L5 字段原始值时，需要在 `access_request` 中存在有效的 `APPROVED` 记录。
- 审批动作写入 `approval_record`，关键操作写入 `audit_log`。
- Agent 执行记录写入 `agent_task`，字段分类和审批建议写入 `agent_recommendation`。
- 审计日志风险分析生成 `risk_alert`，Agent 问答写入 `agent_chat_history`。

## 数据库对象

- `sensitive_field_view`：展示所有 L3 及以上敏感字段。
- `user_accessible_field_view`：按用户展示字段原始值可访问状态。
- `sp_count_sensitive_fields_by_source`：按数据源统计不同等级敏感字段数量。
- `sp_auto_classify_fields`：根据启用规则自动更新字段分类分级。
- `trg_audit_field_classification_update`：分类分级更新后写审计。
- `trg_audit_access_request_update`：申请状态更新后写审计。
