-- 用户表
create table sys_user
(
    id          bigint       not null comment '主键'
        primary key,
    username    varchar(30)  not null comment '用户名',
    nickname    varchar(30)  null comment '昵称',
    password    varchar(200) not null comment '密码',
    enable      bit          not null comment '是否启用',
    `lock`      bit          not null comment '是否锁定',
    create_time datetime     not null comment '创建时间',
    update_time datetime     not null comment '更新时间',
    create_by   varchar(30)  not null comment '创建人',
    update_by   varchar(30)  not null comment '更新人'
)
    comment '用户表';

-- 用户角色关联表
create table sys_user_role
(
    user_id bigint not null comment '用户ID',
    role_id bigint not null comment '角色ID',
    primary key (user_id, role_id)
)
    comment '用户角色关联表';

-- 角色表
create table sys_role
(
    id          bigint       not null
        primary key,
    code        varchar(30)  not null comment '编码',
    name        varchar(30)  not null comment '名称',
    description varchar(200) null comment '描述',
    create_time datetime     not null comment '创建时间',
    update_time datetime     not null comment '更新时间',
    create_by   varchar(30)  null comment '创建人',
    update_by   varchar(30)  null comment '更新人'
)
    comment '角色表';

-- 菜单表
create table sys_resource
(
    id          bigint       not null comment '主键'
        primary key,
    name        varchar(30)  not null comment '菜单名称',
    code        varchar(30)  not null comment '菜单编码',
    type        varchar(10)  null comment '资源类型',
    component   varchar(200) null comment '前端组件',
    path        varchar(200) null comment '路由路径',
    icon        varchar(50)  null comment '前端图标',
    pid         bigint       not null comment '父级ID',
    create_time datetime     not null comment '创建时间',
    update_time datetime     not null comment '更新时间',
    create_by   varchar(30)  not null comment '创建人',
    update_by   varchar(30)  not null comment '更新人'
)
    comment '菜单表';

-- 角色菜单关联表
create table sys_menu_role
(
    role_id bigint not null comment '角色ID',
    menu_id bigint not null comment '菜单ID',
    primary key (role_id, menu_id)
)
    comment '角色菜单关联表';