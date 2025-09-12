/*
 * 数据库结构更新脚本
 * 将菜单相关的表和字段重命名为资源
 */

-- 重命名角色菜单关联表为角色资源关联表
ALTER TABLE menu_role RENAME TO resource_role;

-- 重命名menu_id列为resource_id
ALTER TABLE resource_role CHANGE COLUMN menu_id resource_id bigint NOT NULL COMMENT '资源ID';

-- 更新表注释
ALTER TABLE resource_role COMMENT '角色资源关联表';

-- 更新resource表的字段注释
ALTER TABLE resource MODIFY COLUMN name varchar(50) NOT NULL COMMENT '资源名称';
ALTER TABLE resource MODIFY COLUMN code varchar(50) NOT NULL COMMENT '资源编码';

-- 更新表注释
ALTER TABLE resource COMMENT '资源表';