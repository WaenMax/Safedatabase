if db_id(N'data_security') is null create database data_security;
go
use data_security;
go

drop trigger if exists trg_audit_field_classification_update;
drop trigger if exists trg_audit_access_request_update;
drop view if exists sensitive_field_view;
drop view if exists user_accessible_field_view;
drop procedure if exists sp_count_sensitive_fields_by_source;
drop procedure if exists sp_auto_classify_fields;
go

drop table if exists approval_record, agent_chat_history, risk_alert, agent_recommendation, agent_task;
drop table if exists access_request, masking_policy, field_classification, classification_rule;
drop table if exists audit_log, data_field_asset, data_table_asset, data_source;
drop table if exists sys_role_permission, sys_user_role, sys_permission, sys_role, sys_user;
drop table if exists classification_level, classification_category;
go

create table sys_user (
  id bigint identity(1,1) primary key,
  username nvarchar(50) not null unique,
  password nvarchar(100) not null,
  real_name nvarchar(50),
  email nvarchar(100),
  phone nvarchar(30),
  enabled bit not null default 1,
  created_time datetime2 default sysdatetime()
);

create table sys_role (
  id bigint identity(1,1) primary key,
  role_code nvarchar(50) not null unique,
  role_name nvarchar(100) not null,
  description nvarchar(255)
);

create table sys_permission (
  id bigint identity(1,1) primary key,
  permission_code nvarchar(100) not null unique,
  permission_name nvarchar(100) not null,
  description nvarchar(255)
);

create table sys_user_role (
  user_id bigint not null foreign key references sys_user(id),
  role_id bigint not null foreign key references sys_role(id),
  primary key(user_id, role_id)
);

create table sys_role_permission (
  role_id bigint not null foreign key references sys_role(id),
  permission_id bigint not null foreign key references sys_permission(id),
  primary key(role_id, permission_id)
);

create table data_source (
  id bigint identity(1,1) primary key,
  source_name nvarchar(100) not null,
  source_type nvarchar(50) not null,
  host nvarchar(100),
  port int,
  database_name nvarchar(100),
  description nvarchar(255),
  created_time datetime2 default sysdatetime()
);

create table data_table_asset (
  id bigint identity(1,1) primary key,
  source_id bigint not null foreign key references data_source(id),
  table_name nvarchar(100) not null,
  business_name nvarchar(100),
  description nvarchar(255),
  owner_department nvarchar(100),
  created_time datetime2 default sysdatetime()
);

create table data_field_asset (
  id bigint identity(1,1) primary key,
  table_id bigint not null foreign key references data_table_asset(id),
  field_name nvarchar(100) not null,
  field_type nvarchar(50) not null,
  field_comment nvarchar(255),
  sample_value nvarchar(500),
  is_sensitive bit not null default 0,
  created_time datetime2 default sysdatetime()
);

create table classification_category (
  id bigint identity(1,1) primary key,
  category_name nvarchar(100) not null,
  description nvarchar(255)
);

create table classification_level (
  id bigint identity(1,1) primary key,
  level_code nvarchar(10) not null unique,
  level_name nvarchar(50) not null,
  level_order int not null,
  description nvarchar(255)
);

create table classification_rule (
  id bigint identity(1,1) primary key,
  rule_name nvarchar(100) not null,
  match_type nvarchar(20) not null check(match_type in ('keyword','regex')),
  match_pattern nvarchar(255) not null,
  category_id bigint not null foreign key references classification_category(id),
  level_id bigint not null foreign key references classification_level(id),
  enabled bit not null default 1,
  created_time datetime2 default sysdatetime()
);

create table field_classification (
  id bigint identity(1,1) primary key,
  field_id bigint not null unique foreign key references data_field_asset(id),
  category_id bigint not null foreign key references classification_category(id),
  level_id bigint not null foreign key references classification_level(id),
  classify_method nvarchar(20) not null,
  classified_by bigint null foreign key references sys_user(id),
  classified_time datetime2 default sysdatetime(),
  remark nvarchar(255)
);

create table masking_policy (
  id bigint identity(1,1) primary key,
  policy_name nvarchar(100) not null,
  policy_type nvarchar(50) not null,
  example_before nvarchar(100),
  example_after nvarchar(100),
  description nvarchar(255),
  enabled bit not null default 1
);

create table access_request (
  id bigint identity(1,1) primary key,
  user_id bigint not null foreign key references sys_user(id),
  field_id bigint not null foreign key references data_field_asset(id),
  reason nvarchar(255),
  status nvarchar(20) not null check(status in ('PENDING','APPROVED','REJECTED')),
  request_time datetime2 default sysdatetime(),
  valid_until datetime2
);

create table approval_record (
  id bigint identity(1,1) primary key,
  request_id bigint not null foreign key references access_request(id),
  approver_id bigint null foreign key references sys_user(id),
  approval_result nvarchar(20) not null,
  approval_comment nvarchar(255),
  approval_time datetime2 default sysdatetime()
);

create table audit_log (
  id bigint identity(1,1) primary key,
  user_id bigint null,
  operation_type nvarchar(50) not null,
  target_type nvarchar(50),
  target_id bigint,
  operation_time datetime2 not null default sysdatetime(),
  ip_address nvarchar(50),
  result nvarchar(20),
  detail nvarchar(500)
);

create table agent_task (
  task_id bigint identity(1,1) primary key,
  task_type nvarchar(64),
  task_status nvarchar(32),
  input_data nvarchar(max),
  output_data nvarchar(max),
  created_by bigint null foreign key references sys_user(id),
  created_time datetime2 default sysdatetime(),
  finished_time datetime2,
  error_message nvarchar(max)
);

create table agent_recommendation (
  recommendation_id bigint identity(1,1) primary key,
  recommendation_type nvarchar(64),
  target_type nvarchar(64),
  target_id bigint,
  recommendation_result nvarchar(64),
  risk_level nvarchar(32),
  confidence decimal(5,2),
  reason nvarchar(max),
  suggestion nvarchar(max),
  applied bit not null default 0,
  created_time datetime2 default sysdatetime(),
  created_by bigint null foreign key references sys_user(id)
);

create table risk_alert (
  alert_id bigint identity(1,1) primary key,
  risk_type nvarchar(64),
  risk_level nvarchar(32),
  user_id bigint null foreign key references sys_user(id),
  target_type nvarchar(64),
  target_id bigint,
  description nvarchar(max),
  suggestion nvarchar(max),
  status nvarchar(32) default N'OPEN',
  created_time datetime2 default sysdatetime(),
  handled_by bigint null foreign key references sys_user(id),
  handled_time datetime2
);

create table agent_chat_history (
  chat_id bigint identity(1,1) primary key,
  user_id bigint null foreign key references sys_user(id),
  question nvarchar(max),
  answer nvarchar(max),
  created_time datetime2 default sysdatetime()
);
go

create index idx_data_field_name on data_field_asset(field_name);
create index idx_field_classification_level on field_classification(level_id);
create index idx_audit_user on audit_log(user_id);
create index idx_audit_time on audit_log(operation_time);
create index idx_access_status on access_request(status);
create index idx_agent_task_type on agent_task(task_type);
create index idx_agent_recommendation_target on agent_recommendation(target_type, target_id);
create index idx_risk_alert_level on risk_alert(risk_level);
create index idx_risk_alert_user on risk_alert(user_id);
create index idx_agent_chat_user on agent_chat_history(user_id);
go

set identity_insert sys_role on;
insert into sys_role(id, role_code, role_name, description) values
(1,N'admin',N'系统管理员',N'管理用户、角色和全部数据'),
(2,N'security_admin',N'数据安全管理员',N'维护分类分级、规则和脱敏策略'),
(3,N'user',N'普通业务用户',N'查看资产并提交高敏感访问申请'),
(4,N'approver',N'审批人员',N'审批数据访问申请');
set identity_insert sys_role off;

insert into sys_permission(permission_code, permission_name, description) values
(N'user:manage',N'用户管理',N'维护用户和角色'),
(N'asset:manage',N'资产管理',N'维护数据源、表、字段'),
(N'classify:manage',N'分类分级管理',N'维护分类结果和规则'),
(N'request:approve',N'审批管理',N'审批访问申请'),
(N'audit:view',N'审计查看',N'查看审计日志');

set identity_insert sys_user on;
insert into sys_user(id, username, password, real_name, email, phone, enabled) values
(1,N'admin',N'123456',N'系统管理员',N'admin@example.com',N'13800008888',1),
(2,N'security',N'123456',N'安全管理员',N'security@example.com',N'13800006666',1),
(3,N'user',N'123456',N'业务用户',N'user@example.com',N'13800001111',1),
(4,N'approver',N'123456',N'审批人员',N'approver@example.com',N'13800002222',1);
set identity_insert sys_user off;

insert into sys_user_role(user_id, role_id) values (1,1),(2,2),(3,3),(4,4);
insert into sys_role_permission(role_id, permission_id) select 1,id from sys_permission;
insert into sys_role_permission(role_id, permission_id) select 2,id from sys_permission where permission_code in (N'asset:manage',N'classify:manage',N'audit:view');
insert into sys_role_permission(role_id, permission_id) select 4,id from sys_permission where permission_code in (N'request:approve',N'audit:view');

set identity_insert classification_category on;
insert into classification_category(id, category_name, description) values
(1,N'个人基本信息',N'姓名、手机号、邮箱等基础身份联系信息'),
(2,N'个人身份信息',N'身份证、护照等强身份标识信息'),
(3,N'个人财产信息',N'银行卡、账户、资产等财产相关信息'),
(4,N'业务运营数据',N'订单、交易、客户运营数据'),
(5,N'系统运维数据',N'密码、令牌、密钥、运行日志等'),
(6,N'公开数据',N'可公开披露的数据');
set identity_insert classification_category off;

set identity_insert classification_level on;
insert into classification_level(id, level_code, level_name, level_order, description) values
(1,N'L1',N'公开数据',1,N'公开后影响较小'),
(2,N'L2',N'内部数据',2,N'仅限企业内部使用'),
(3,N'L3',N'敏感数据',3,N'泄露会造成一定影响'),
(4,N'L4',N'高敏感数据',4,N'泄露会造成严重影响'),
(5,N'L5',N'核心数据',5,N'核心机密或关键系统数据');
set identity_insert classification_level off;

insert into classification_rule(rule_name, match_type, match_pattern, category_id, level_id, enabled) values
(N'手机号识别',N'keyword',N'phone,mobile,tel,手机号',1,3,1),
(N'身份证识别',N'keyword',N'id_card,身份证',2,4,1),
(N'银行卡账户识别',N'keyword',N'bank,card,account',3,4,1),
(N'密码密钥识别',N'keyword',N'password,token,secret',5,5,1),
(N'邮箱识别',N'keyword',N'email',1,3,1);

set identity_insert data_source on;
insert into data_source(id, source_name, source_type, host, port, database_name, description) values
(1,N'客户经营库',N'SQL Server',N'127.0.0.1',1433,N'customer_db',N'客户和订单演示数据源'),
(2,N'运维日志库',N'MySQL',N'127.0.0.1',3306,N'ops_db',N'系统运维演示数据源');
set identity_insert data_source off;

set identity_insert data_table_asset on;
insert into data_table_asset(id, source_id, table_name, business_name, description, owner_department) values
(1,1,N'customer',N'客户表',N'客户基础资料',N'客户中心'),
(2,1,N'orders',N'订单表',N'客户订单数据',N'交易中心'),
(3,2,N'sys_account',N'系统账号表',N'运维账号和令牌',N'技术部');
set identity_insert data_table_asset off;

set identity_insert data_field_asset on;
insert into data_field_asset(id, table_id, field_name, field_type, field_comment, sample_value, is_sensitive) values
(1,1,N'customer_name',N'varchar(50)',N'客户姓名',N'张三',0),
(2,1,N'phone',N'varchar(20)',N'手机号',N'13812348888',1),
(3,1,N'email',N'varchar(100)',N'邮箱',N'abc_user@example.com',1),
(4,1,N'id_card',N'varchar(18)',N'身份证号',N'510123199901011234',1),
(5,2,N'order_no',N'varchar(32)',N'订单编号',N'SO202606220001',0),
(6,2,N'bank_card',N'varchar(32)',N'付款银行卡',N'6222020202021234',1),
(7,3,N'login_account',N'varchar(50)',N'登录账号',N'ops_admin',1),
(8,3,N'password_hash',N'varchar(100)',N'密码摘要',N'secret-token-demo',1);
set identity_insert data_field_asset off;

insert into field_classification(field_id, category_id, level_id, classify_method, classified_by, remark) values
(1,1,2,N'MANUAL',2,N'姓名按内部数据管理'),
(2,1,3,N'AUTO',2,N'手机号识别'),
(3,1,3,N'AUTO',2,N'邮箱识别'),
(4,2,4,N'AUTO',2,N'身份证识别'),
(5,4,2,N'MANUAL',2,N'订单编号'),
(6,3,4,N'AUTO',2,N'银行卡识别'),
(7,5,3,N'MANUAL',2,N'系统账号'),
(8,5,5,N'AUTO',2,N'密码密钥识别');

insert into masking_policy(policy_name, policy_type, example_before, example_after, description, enabled) values
(N'手机号脱敏',N'phone',N'13812348888',N'138****8888',N'保留前三后四',1),
(N'邮箱脱敏',N'email',N'abc_user@example.com',N'abc***@example.com',N'隐藏邮箱用户名中间部分',1),
(N'身份证脱敏',N'id_card',N'510123199901011234',N'510***********1234',N'保留前三后四',1),
(N'银行卡脱敏',N'bank_card',N'6222020202021234',N'6222 **** **** 1234',N'保留前四后四',1);

insert into access_request(user_id, field_id, reason, status, valid_until) values
(3,4,N'客户实名核验问题排查',N'PENDING',dateadd(day,7,sysdatetime())),
(3,6,N'订单退款银行卡核对',N'APPROVED',dateadd(day,7,sysdatetime()));

insert into approval_record(request_id, approver_id, approval_result, approval_comment) values
(2,4,N'APPROVED',N'演示审批通过');

insert into audit_log(user_id, operation_type, target_type, target_id, result, detail) values
(1,N'INIT',N'system',null,N'SUCCESS',N'初始化演示数据');

insert into agent_recommendation(recommendation_type,target_type,target_id,recommendation_result,risk_level,confidence,reason,suggestion,applied,created_by) values
(N'FIELD_CLASSIFY',N'data_field_asset',4,N'个人身份信息 / L4',N'medium',0.95,N'字段名命中 id_card 规则，样例值符合身份证格式',N'建议保持 L4 高敏感数据并启用身份证脱敏策略',1,2),
(N'ACCESS_REVIEW',N'access_request',1,N'manual_review',N'medium',0.70,N'普通用户申请 L4 字段，理由较充分但仍需人工确认',N'建议审批人员核实业务场景后处理',0,4),
(N'AUDIT_RISK',N'audit_log',null,N'review',N'high',0.88,N'检测到高敏字段访问行为较集中',N'建议核查访问目的并保留审计证据',0,1);

insert into risk_alert(risk_type,risk_level,user_id,target_type,target_id,description,suggestion,status) values
(N'HIGH_SENSITIVE_ACCESS',N'high',3,N'data_field_asset',4,N'普通用户访问 L4 身份证字段触发审批控制',N'核查访问申请理由，必要时缩短授权有效期',N'OPEN'),
(N'REJECTED_REQUEST_REPEAT',N'medium',3,N'access_request',1,N'用户存在高敏字段访问申请待处理记录',N'审批前核实业务必要性',N'OPEN'),
(N'CLASSIFICATION_CHANGE',N'medium',2,N'field_classification',8,N'核心字段分类分级近期被维护',N'复核 L5 字段的分类依据和脱敏策略',N'HANDLED');

insert into agent_chat_history(user_id,question,answer) values
(1,N'哪些字段是高敏感数据？',N'当前 L4/L5 字段包括 id_card、bank_card、password_hash。'),
(2,N'当前系统的敏感字段数量是多少？',N'当前敏感字段数量为 6 个。'),
(1,N'生成一份安全治理建议',N'建议优先复核 L4/L5 字段访问授权、完善脱敏策略覆盖率并定期分析审计日志。');
go

create view sensitive_field_view as
select ds.source_name, dt.table_name, f.field_name, f.field_type, f.field_comment,
       c.category_name, l.level_code, l.level_name, f.sample_value
from data_field_asset f
join data_table_asset dt on dt.id = f.table_id
join data_source ds on ds.id = dt.source_id
join field_classification fc on fc.field_id = f.id
join classification_category c on c.id = fc.category_id
join classification_level l on l.id = fc.level_id
where l.level_order >= 3;
go

create view user_accessible_field_view as
select u.id user_id, u.username, f.id field_id, f.field_name, l.level_code,
       case when l.level_order <= 3 then 1
            when ar.status = N'APPROVED' and ar.valid_until > sysdatetime() then 1
            else 0 end can_access_raw
from sys_user u
cross join data_field_asset f
left join field_classification fc on fc.field_id = f.id
left join classification_level l on l.id = fc.level_id
left join access_request ar on ar.user_id = u.id and ar.field_id = f.id;
go

create procedure sp_count_sensitive_fields_by_source
  @source_id bigint
as
begin
  select l.level_code, l.level_name, count(f.id) field_count
  from data_source ds
  join data_table_asset dt on dt.source_id = ds.id
  join data_field_asset f on f.table_id = dt.id
  join field_classification fc on fc.field_id = f.id
  join classification_level l on l.id = fc.level_id
  where ds.id = @source_id and l.level_order >= 3
  group by l.level_code, l.level_name, l.level_order
  order by l.level_order;
end;
go

create procedure sp_auto_classify_fields
as
begin
  set nocount on;
  merge field_classification as target
  using (
    select f.id field_id, r.category_id, r.level_id, r.rule_name
    from data_field_asset f
    cross apply (
      select top 1 * from classification_rule r
      where r.enabled = 1 and r.match_type = N'keyword'
        and exists (
          select 1 from string_split(r.match_pattern, N',') p
          where lower(f.field_name) like N'%' + lower(ltrim(rtrim(p.value))) + N'%'
        )
      order by r.id
    ) r
  ) as src
  on target.field_id = src.field_id
  when matched then update set category_id=src.category_id, level_id=src.level_id,
       classify_method=N'AUTO', classified_time=sysdatetime(), remark=N'命中规则: ' + src.rule_name
  when not matched then insert(field_id, category_id, level_id, classify_method, classified_time, remark)
       values(src.field_id, src.category_id, src.level_id, N'AUTO', sysdatetime(), N'命中规则: ' + src.rule_name);

  update f set is_sensitive = case when l.level_order >= 3 then 1 else 0 end
  from data_field_asset f
  join field_classification fc on fc.field_id = f.id
  join classification_level l on l.id = fc.level_id;
end;
go

create trigger trg_audit_field_classification_update
on field_classification
after update
as
begin
  insert into audit_log(user_id, operation_type, target_type, target_id, operation_time, result, detail)
  select i.classified_by, N'TRIGGER_CLASSIFICATION_UPDATE', N'field_classification', i.id, sysdatetime(), N'SUCCESS',
         concat(N'字段 ', i.field_id, N' 分类/分级由 ', d.category_id, N'/', d.level_id, N' 改为 ', i.category_id, N'/', i.level_id)
  from inserted i join deleted d on d.id = i.id
  where isnull(i.category_id,0) <> isnull(d.category_id,0) or isnull(i.level_id,0) <> isnull(d.level_id,0);
end;
go

create trigger trg_audit_access_request_update
on access_request
after update
as
begin
  insert into audit_log(user_id, operation_type, target_type, target_id, operation_time, result, detail)
  select i.user_id, N'TRIGGER_ACCESS_REQUEST_UPDATE', N'access_request', i.id, sysdatetime(), N'SUCCESS',
         concat(N'访问申请状态由 ', d.status, N' 改为 ', i.status)
  from inserted i join deleted d on d.id = i.id
  where isnull(i.status,N'') <> isnull(d.status,N'');
end;
go
