# 接口文档

所有接口前缀为 `/api`，登录后前端在请求头携带 `Authorization: Bearer <token>`。

## 认证

- `POST /auth/login`：登录，参数 `username`、`password`
- `GET /auth/me`：获取当前用户

## 用户与角色

- `GET /users`：用户列表
- `POST /users`：新增用户
- `PUT /users/{id}`：修改用户
- `DELETE /users/{id}`：删除用户
- `GET /roles`：角色列表

## 数据资产

- `GET /data-sources`、`POST /data-sources`、`PUT /data-sources/{id}`、`DELETE /data-sources/{id}`
- `GET /tables`、`POST /tables`、`PUT /tables/{id}`、`DELETE /tables/{id}`
- `GET /fields`、`GET /fields/{id}`、`POST /fields`、`PUT /fields/{id}`、`DELETE /fields/{id}`

## 分类分级

- `GET /categories`：分类字典
- `GET /levels`：等级字典
- `GET /field-classifications`：字段分类结果
- `POST /field-classifications`：人工分类
- `PUT /field-classifications/{id}`：修改分类
- `POST /field-classifications/auto-classify`：执行自动分类

## 规则与脱敏

- `GET /rules`、`POST /rules`、`PUT /rules/{id}`、`DELETE /rules/{id}`
- `GET /masking-policies`：脱敏策略列表
- `GET /fields/{id}/masked-value`：查看脱敏样例值
- `GET /fields/{id}/raw-value`：查看原始样例值，L4/L5 普通用户需审批

## 访问申请、审批、审计

- `GET /access-requests`：申请列表
- `POST /access-requests`：提交访问申请
- `PUT /access-requests/{id}/approve`：审批通过
- `PUT /access-requests/{id}/reject`：审批驳回
- `GET /audit-logs`：审计日志
- `GET /dashboard/summary`：统计看板

## 数据安全治理 Agent

- `GET /agent/workbench`：Agent 工作台统计、最近建议、最近告警
- `GET /agent/recommendations`：Agent 建议列表
- `GET /agent/unclassified-fields`：未分类字段列表
- `POST /agent/classify-field/{fieldId}?apply=false`：分析单个字段，返回建议分类、分级、理由、置信度
- `POST /agent/classify-field/{fieldId}?apply=true`：分析并应用单个字段建议
- `POST /agent/classify-all-fields?apply=false`：批量分析全部字段
- `POST /agent/review-access-request/{requestId}`：生成访问审批建议
- `POST /agent/analyze-audit-logs`：分析审计日志并生成风险告警
- `GET /agent/risk-alerts?level=high`：风险告警列表，可按等级筛选
- `PUT /agent/risk-alerts/{id}/handle`：标记告警已处理
- `GET /agent/security-report`：生成 Markdown 安全治理报告
- `POST /agent/chat`：Agent 问答，参数 `question`、`provider`、`model`
  - `provider=local`：本地规则推理
  - `provider=deepseek`：DeepSeek 官方 API
  - `provider=siliconflow`：硅基流动 API


说明：后端 LLM Provider 使用 LangChain4j 实现。DeepSeek 官方模型可填写 deepseek-v4-flash 或 deepseek-v4-pro；硅基流动模型可填写平台模型名，如 deepseek-ai/DeepSeek-V3.2。

