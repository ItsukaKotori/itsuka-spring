/*
 * 数据库结构更新脚本
 * 添加组织机构相关表结构
 */

-- 创建组织机构表
CREATE TABLE IF NOT EXISTS organization (
    id bigint NOT NULL COMMENT '主键（雪花算法生成）',
    name varchar(100) NOT NULL COMMENT '组织名称',
    code varchar(50) NOT NULL COMMENT '组织编码',
    type varchar(20) NOT NULL COMMENT '组织类型：COMPANY-公司，DEPARTMENT-部门，POSITION-岗位',
    parent_id bigint DEFAULT 0 COMMENT '父级ID，0表示顶级',
    sort_order int DEFAULT 0 COMMENT '排序号',
    leader_id bigint COMMENT '负责人ID',
    phone varchar(20) COMMENT '联系电话',
    email varchar(100) COMMENT '邮箱',
    status tinyint DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time datetime COMMENT '创建时间',
    update_time datetime COMMENT '更新时间',
    create_by varchar(50) COMMENT '创建人',
    update_by varchar(50) COMMENT '更新人',
    deleted tinyint DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_parent_id (parent_id),
    KEY idx_type (type),
    KEY idx_deleted (deleted)
) COMMENT '组织机构表';

-- 更新用户表，添加部门关联
ALTER TABLE user ADD COLUMN IF NOT EXISTS organization_id bigint COMMENT '所属部门ID';
ALTER TABLE user ADD COLUMN IF NOT EXISTS position varchar(50) COMMENT '岗位';
ALTER TABLE user ADD INDEX IF NOT EXISTS idx_organization_id (organization_id);

-- 初始化组织机构数据
INSERT INTO organization (id, name, code, type, parent_id, sort_order, status, create_time, update_time, create_by, update_by) VALUES
(1, '总公司', 'ROOT_COMPANY', 'COMPANY', 0, 1, 1, NOW(), NOW(), 'system', 'system'),
(2, '技术部', 'TECH_DEPT', 'DEPARTMENT', 1, 1, 1, NOW(), NOW(), 'system', 'system'),
(3, '市场部', 'MARKET_DEPT', 'DEPARTMENT', 1, 2, 1, NOW(), NOW(), 'system', 'system'),
(4, '前端开发', 'FRONTEND_POS', 'POSITION', 2, 1, 1, NOW(), NOW(), 'system', 'system'),
(5, '后端开发', 'BACKEND_POS', 'POSITION', 2, 2, 1, NOW(), NOW(), 'system', 'system'),
(6, '产品经理', 'PM_POS', 'POSITION', 3, 1, 1, NOW(), NOW(), 'system', 'system'),
(7, '市场专员', 'MARKETER_POS', 'POSITION', 3, 2, 1, NOW(), NOW(), 'system', 'system');

-- 更新管理员用户的组织信息
UPDATE user SET organization_id = 1, position = '系统管理员' WHERE username = 'admin';