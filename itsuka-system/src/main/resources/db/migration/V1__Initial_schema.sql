/*
 * 数据库初始化脚本
 * 创建系统所需的所有表结构
 */

-- 创建用户表
drop table if exists user;
create table user (
    id bigint not null comment '主键（雪花算法生成）',
    username varchar(50) not null comment '用户名',
    nickname varchar(50) comment '昵称',
    password varchar(255) not null comment '密码',
    enable boolean not null default true comment '是否启用',
    `lock` boolean not null default false comment '是否锁定',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    create_by varchar(50) comment '创建人',
    update_by varchar(50) comment '更新人',
    primary key (id),
    unique key uk_username (username)
) comment '系统用户表';

-- 创建角色表
drop table if exists role;
create table role (
    id bigint not null comment '主键（雪花算法生成）',
    code varchar(50) not null comment '编码',
    name varchar(50) not null comment '名称',
    description varchar(255) comment '描述',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    create_by varchar(50) comment '创建人',
    update_by varchar(50) comment '更新人',
    primary key (id),
    unique key uk_code (code)
) comment '角色表';

-- 创建资源表
drop table if exists resource;
create table resource (
    id bigint not null comment '主键（雪花算法生成）',
    name varchar(50) not null comment '资源名称',
    code varchar(50) not null comment '资源编码',
    type varchar(20) not null comment '资源类型',
    component varchar(255) comment '前端组件',
    path varchar(255) comment '路由路径',
    icon varchar(50) comment '前端图标',
    pid bigint default 0 comment '父级ID',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    create_by varchar(50) comment '创建人',
    update_by varchar(50) comment '更新人',
    primary key (id),
    unique key uk_code (code)
) comment '资源表';

-- 创建用户角色关联表
drop table if exists user_role;
create table user_role (
    user_id bigint not null comment '用户ID',
    role_id bigint not null comment '角色ID',
    primary key (user_id, role_id),
    foreign key (user_id) references user(id) on delete cascade,
    foreign key (role_id) references role(id) on delete cascade
) comment '用户角色关联表';

-- 创建角色资源关联表
drop table if exists resource_role;
create table resource_role (
    role_id bigint not null comment '角色ID',
    resource_id bigint not null comment '资源ID',
    primary key (role_id, resource_id),
    foreign key (role_id) references role(id) on delete cascade,
    foreign key (resource_id) references resource(id) on delete cascade
) comment '角色资源关联表';

/*
 * 初始化数据
 */

-- 初始化管理员用户
insert into user (id, username, nickname, password, enable, `lock`, create_time, update_time) 
values (1, 'admin', '系统管理员', '$2a$10$J3u5t8n9O1pR2e3wQ4e5rT6y7uI8o9p', true, false, now(), now());

-- 初始化角色
insert into role (id, code, name, description, create_time, update_time) 
values (1, 'ADMIN', '管理员', '系统管理员角色，拥有所有权限', now(), now());

-- 关联管理员用户和管理员角色
insert into user_role (user_id, role_id) 
values ((select id from user where username = 'admin'), (select id from role where code = 'ADMIN'));