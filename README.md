# 数据分类分级保护系统

面向企业数据资产管理场景的数据库课程设计项目，实现数据源、数据表、字段资产登记，字段分类分级，规则自动识别，脱敏展示，访问申请审批，审计日志和统计看板。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Web、Spring JDBC、Spring Security、Lombok
- 数据库：MySQL，启动时自动初始化演示表和数据
- 前端：Vue 3、Vite、Element Plus、Axios、Vue Router

## 功能模块

- 用户登录与角色管理：admin、security_admin、user、approver
- 数据资产管理：数据源、数据表、字段资产 CRUD
- 分类分级管理：L1-L5，支持人工分类与规则自动分类
- 分类规则管理：keyword/regex 规则维护与启用禁用
- 脱敏策略：手机号、邮箱、身份证、银行卡、密码密钥
- 访问申请与审批：普通用户访问 L4/L5 原始值需审批
- 审计日志：登录、查看字段、修改分类、审批、访问高敏数据
- 统计看板：资产数量、敏感字段、等级/分类统计、最近日志
- 数据安全治理 Agent：字段智能分类、审批建议、审计风险分析、安全报告、Agent 问答

## 数据库表

系统包含 16 张核心表：`sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`、`data_source`、`data_table_asset`、`data_field_asset`、`classification_category`、`classification_level`、`classification_rule`、`field_classification`、`masking_policy`、`access_request`、`approval_record`、`audit_log`。

Agent 模块新增 4 张表：`agent_task`、`agent_recommendation`、`risk_alert`、`agent_chat_history`。

当前 MySQL 初始化脚本包含核心表、索引和演示数据。课程设计中的数据库对象还包括：

- 视图：`sensitive_field_view`、`user_accessible_field_view`
- 存储过程：`sp_count_sensitive_fields_by_source`、`sp_auto_classify_fields`
- 触发器：`trg_audit_field_classification_update`、`trg_audit_access_request_update`
- 索引：字段名、分级、审计用户、审计时间、申请状态索引

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认使用 MySQL，启动后自动执行 `backend/src/main/resources/schema.sql` 和 `data.sql`。后端地址：`http://localhost:8080`。

启动前先创建数据库：

```bash
mysql -u root -p -e "create database if not exists data_security default character set utf8mb4 collate utf8mb4_unicode_ci;"
```

默认连接参数可通过环境变量覆盖：

```bash
MYSQL_URL=jdbc:mysql://localhost:3306/data_security?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
MYSQL_USERNAME=root
MYSQL_PASSWORD=123456
```

启动：

```bash
cd backend
mvn spring-boot:run
```

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`。

## 演示账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | admin |
| security | 123456 | security_admin |
| user | 123456 | user |
| approver | 123456 | approver |

课程演示环境使用明文密码方便初始化和讲解，生产环境应改为 BCrypt 等强哈希存储。

## 项目结构

```text
backend/     Spring Boot 后端
frontend/    Vue 3 前端
sql/         MySQL 建表与初始化脚本
docs/        课程设计报告辅助材料
```

## 课程设计亮点

- 数据库对象完整：表、主外键、索引、视图、存储过程、触发器齐全
- 规则驱动自动分类：字段名命中关键词后自动写入分类分级结果
- 数据安全闭环：敏感识别、脱敏、审批、授权访问、审计留痕
- 演示数据充分：客户表、订单表、账号表及敏感字段样例可直接演示
- 本地规则 Agent：不依赖外部大模型 API，Agent 建议、风险告警和问答历史全部落库

## 数据安全治理 Agent

Agent 默认使用本地规则推理，可离线运行：

- 字段分类：读取字段名、类型、说明、样例值，结合 `classification_rule` 和内置格式规则输出分类、分级、理由和置信度。
- 审批建议：结合申请人角色、字段等级、申请理由、历史驳回记录输出 `approve`、`reject` 或 `manual_review`。
- 审计风险：识别短时间高频敏感访问、L4/L5 访问集中、登录失败过多、同 IP 多账号登录、非管理员修改分类等风险。
- 安全报告：生成 Markdown 报告，包含资产概览、L3/L4/L5 数量、覆盖率、风险告警和整改建议。
- Agent 问答：关键词意图识别，支持高敏字段、L4 访问用户、风险告警、未分类字段、被拒申请、敏感字段数量和治理建议。

Agent 接口：

- `GET /api/agent/workbench`
- `POST /api/agent/classify-field/{fieldId}?apply=true|false`
- `POST /api/agent/classify-all-fields?apply=true|false`
- `POST /api/agent/review-access-request/{requestId}`
- `POST /api/agent/analyze-audit-logs`
- `GET /api/agent/risk-alerts`
- `PUT /api/agent/risk-alerts/{id}/handle`
- `GET /api/agent/security-report`
- `POST /api/agent/chat`

悬浮 AI 助手：

- 登录后右下角显示 `AI` 悬浮按钮，可在任意页面打开助手。
- 支持 `本地规则`、`DeepSeek 官方 API`、`硅基流动 API` 三种通道。
- API Key 通过环境变量读取，不写入源码：`DEEPSEEK_API_KEY`、`SILICONFLOW_API_KEY`。
- 可选模型环境变量：`DEEPSEEK_MODEL`、`SILICONFLOW_MODEL`。硅基流动默认模型为 `deepseek-ai/DeepSeek-V3.2`。

Agent 演示流程：

1. 在字段资产管理中新增 `id_card_no`，样例值填写 `510123199901011234`，到“字段智能分类”点击 Agent 分析并应用建议。
2. 普通用户申请访问 `password_hash`，审批人员到“审批建议”点击 Agent 审查，根据 high risk 建议驳回。
3. 管理员到“风险告警”点击 Agent 风险分析，生成并处理告警。
4. 管理员到“安全报告”点击生成报告，查看 Markdown 治理建议。

LangChain4j 实现说明：

- 后端 LLM Provider 已改为基于 LangChain4j 的 OpenAI-compatible ChatModel 编排。
- 对外接口保持不变，悬浮助手仍调用 `POST /api/agent/chat`。
- DeepSeek 官方推荐测试模型：`deepseek-v4-flash`，也可在悬浮窗模型框输入 `deepseek-v4-pro`。
- 硅基流动仍可使用 `deepseek-ai/DeepSeek-V3.2` 等平台支持模型。
- 本地规则推理继续作为默认 fallback 和安全基线。
