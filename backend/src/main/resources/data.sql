insert into sys_role(id, role_code, role_name, description) values
(1,'admin','系统管理员','管理用户、角色和全部数据'),
(2,'security_admin','数据安全管理员','维护分类分级、规则和脱敏策略'),
(3,'user','普通业务用户','查看资产并提交高敏感访问申请'),
(4,'approver','审批人员','审批数据访问申请');

insert into sys_permission(permission_code, permission_name, description) values
('user:manage','用户管理','维护用户和角色'),
('asset:manage','资产管理','维护数据源、表、字段'),
('classify:manage','分类分级管理','维护分类结果和规则'),
('request:approve','审批管理','审批访问申请'),
('audit:view','审计查看','查看审计日志');

insert into sys_user(id, username, password, real_name, email, phone, enabled, created_time) values
(1,'admin','123456','系统管理员','admin@example.com','13800008888',1,current_timestamp),
(2,'security','123456','安全管理员','security@example.com','13800006666',1,current_timestamp),
(3,'user','123456','业务用户','user@example.com','13800001111',1,current_timestamp),
(4,'approver','123456','审批人员','approver@example.com','13800002222',1,current_timestamp);

insert into sys_user_role(user_id, role_id) values (1,1),(2,2),(3,3),(4,4);

insert into classification_category(id, category_name, description) values
(1,'个人基本信息','姓名、手机号、邮箱等基础身份联系信息'),
(2,'个人身份信息','身份证、护照等强身份标识信息'),
(3,'个人财产信息','银行卡、账户、资产等财产相关信息'),
(4,'业务运营数据','订单、交易、客户运营数据'),
(5,'系统运维数据','密码、令牌、密钥、运行日志等'),
(6,'公开数据','可公开披露的数据');

insert into classification_level(id, level_code, level_name, level_order, description) values
(1,'L1','公开数据',1,'公开后影响较小'),
(2,'L2','内部数据',2,'仅限企业内部使用'),
(3,'L3','敏感数据',3,'泄露会造成一定影响'),
(4,'L4','高敏感数据',4,'泄露会造成严重影响'),
(5,'L5','核心数据',5,'核心机密或关键系统数据');

insert into classification_rule(rule_name, match_type, match_pattern, category_id, level_id, enabled, created_time) values
('手机号识别','keyword','phone,mobile,tel,手机号',1,3,1,current_timestamp),
('身份证识别','keyword','id_card,身份证',2,4,1,current_timestamp),
('银行卡账户识别','keyword','bank,card,account',3,4,1,current_timestamp),
('密码密钥识别','keyword','password,token,secret',5,5,1,current_timestamp),
('邮箱识别','keyword','email',1,3,1,current_timestamp);

insert into data_source(id, source_name, source_type, host, port, database_name, description, created_time) values
(1,'客户经营库','SQL Server','127.0.0.1',1433,'customer_db','客户和订单演示数据源',current_timestamp),
(2,'运维日志库','MySQL','127.0.0.1',3306,'ops_db','系统运维演示数据源',current_timestamp);

insert into data_table_asset(id, source_id, table_name, business_name, description, owner_department, created_time) values
(1,1,'customer','客户表','客户基础资料','客户中心',current_timestamp),
(2,1,'orders','订单表','客户订单数据','交易中心',current_timestamp),
(3,2,'sys_account','系统账号表','运维账号和令牌','技术部',current_timestamp);

insert into data_field_asset(id, table_id, field_name, field_type, field_comment, sample_value, is_sensitive, created_time) values
(1,1,'customer_name','varchar(50)','客户姓名','张三',0,current_timestamp),
(2,1,'phone','varchar(20)','手机号','13812348888',1,current_timestamp),
(3,1,'email','varchar(100)','邮箱','abc_user@example.com',1,current_timestamp),
(4,1,'id_card','varchar(18)','身份证号','510123199901011234',1,current_timestamp),
(5,2,'order_no','varchar(32)','订单编号','SO202606220001',0,current_timestamp),
(6,2,'bank_card','varchar(32)','付款银行卡','6222020202021234',1,current_timestamp),
(7,3,'login_account','varchar(50)','登录账号','ops_admin',1,current_timestamp),
(8,3,'password_hash','varchar(100)','密码摘要','secret-token-demo',1,current_timestamp);

insert into field_classification(field_id, category_id, level_id, classify_method, classified_by, classified_time, remark) values
(1,1,2,'MANUAL',2,current_timestamp,'姓名按内部数据管理'),
(2,1,3,'AUTO',2,current_timestamp,'手机号识别'),
(3,1,3,'AUTO',2,current_timestamp,'邮箱识别'),
(4,2,4,'AUTO',2,current_timestamp,'身份证识别'),
(5,4,2,'MANUAL',2,current_timestamp,'订单编号'),
(6,3,4,'AUTO',2,current_timestamp,'银行卡识别'),
(7,5,3,'MANUAL',2,current_timestamp,'系统账号'),
(8,5,5,'AUTO',2,current_timestamp,'密码密钥识别');

insert into masking_policy(policy_name, policy_type, example_before, example_after, description, enabled) values
('手机号脱敏','phone','13812348888','138****8888','保留前三后四',1),
('邮箱脱敏','email','abc_user@example.com','abc***@example.com','隐藏邮箱用户名中间部分',1),
('身份证脱敏','id_card','510123199901011234','510***********1234','保留前三后四',1),
('银行卡脱敏','bank_card','6222020202021234','6222 **** **** 1234','保留前四后四',1);

insert into access_request(user_id, field_id, reason, status, request_time, valid_until) values
(3,4,'客户实名核验问题排查','PENDING',current_timestamp,dateadd('day',7,current_timestamp)),
(3,6,'订单退款银行卡核对','APPROVED',current_timestamp,dateadd('day',7,current_timestamp));

insert into approval_record(request_id, approver_id, approval_result, approval_comment, approval_time) values
(2,4,'APPROVED','演示审批通过',current_timestamp);

insert into audit_log(user_id, operation_type, target_type, target_id, operation_time, ip_address, result, detail) values
(1,'INIT','system',null,current_timestamp,'127.0.0.1','SUCCESS','初始化演示数据');

insert into agent_recommendation(recommendation_type,target_type,target_id,recommendation_result,risk_level,confidence,reason,suggestion,applied,created_time,created_by) values
('FIELD_CLASSIFY','data_field_asset',4,'个人身份信息 / L4','medium',0.95,'字段名命中 id_card 规则，样例值符合身份证格式','建议保持 L4 高敏感数据并启用身份证脱敏策略',1,current_timestamp,2),
('ACCESS_REVIEW','access_request',1,'manual_review','medium',0.70,'普通用户申请 L4 字段，理由较充分但仍需人工确认','建议审批人员核实业务场景后处理',0,current_timestamp,4),
('AUDIT_RISK','audit_log',null,'review','high',0.88,'检测到高敏字段访问行为较集中','建议核查访问目的并保留审计证据',0,current_timestamp,1);

insert into risk_alert(risk_type,risk_level,user_id,target_type,target_id,description,suggestion,status,created_time) values
('HIGH_SENSITIVE_ACCESS','high',3,'data_field_asset',4,'普通用户访问 L4 身份证字段触发审批控制','核查访问申请理由，必要时缩短授权有效期','OPEN',current_timestamp),
('REJECTED_REQUEST_REPEAT','medium',3,'access_request',1,'用户存在高敏字段访问申请待处理记录','审批前核实业务必要性','OPEN',current_timestamp),
('CLASSIFICATION_CHANGE','medium',2,'field_classification',8,'核心字段分类分级近期被维护','复核 L5 字段的分类依据和脱敏策略','HANDLED',current_timestamp);

insert into agent_chat_history(user_id,question,answer,created_time) values
(1,'哪些字段是高敏感数据？','当前 L4/L5 字段包括 id_card、bank_card、password_hash。',current_timestamp),
(2,'当前系统的敏感字段数量是多少？','当前敏感字段数量为 6 个。',current_timestamp),
(1,'生成一份安全治理建议','建议优先复核 L4/L5 字段访问授权、完善脱敏策略覆盖率并定期分析审计日志。',current_timestamp);

alter table sys_user alter column id restart with 100;
alter table sys_role alter column id restart with 100;
alter table data_source alter column id restart with 100;
alter table data_table_asset alter column id restart with 100;
alter table data_field_asset alter column id restart with 100;
alter table classification_category alter column id restart with 100;
alter table classification_level alter column id restart with 100;
