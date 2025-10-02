package org.itsuka.mybatis.plus.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务实体基类
 * 在 SoftDeleteDomain 基础上添加常用的业务字段
 * 适用于大多数业务实体
 * 
 * @author Itsuka
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BusinessDomain extends SoftDeleteDomain {

    private static final long serialVersionUID = 1L;

    /**
     * 状态 (0: 禁用, 1: 启用)
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 排序号
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // ====== 常用状态常量 ======
    
    /**
     * 状态 - 禁用
     */
    public static final Integer STATUS_DISABLED = 0;
    
    /**
     * 状态 - 启用
     */
    public static final Integer STATUS_ENABLED = 1;

    // ====== 便捷方法 ======

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return STATUS_ENABLED.equals(this.status);
    }

    /**
     * 是否禁用
     */
    public boolean isDisabled() {
        return STATUS_DISABLED.equals(this.status);
    }

    /**
     * 设置为启用状态
     */
    public void enable() {
        this.status = STATUS_ENABLED;
    }

    /**
     * 设置为禁用状态
     */
    public void disable() {
        this.status = STATUS_DISABLED;
    }

    /**
     * 初始化新实体的默认值（带业务字段支持）
     */
    @Override
    public void initForInsert() {
        super.initForInsert();
        if (this.status == null) {
            this.status = STATUS_ENABLED;
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    /**
     * 初始化新实体的默认值（带用户信息和业务字段支持）
     */
    @Override
    public void initForInsert(String userId) {
        super.initForInsert(userId);
        if (this.status == null) {
            this.status = STATUS_ENABLED;
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    /**
     * 获取实体的简要信息
     */
    @Override
    public String getBriefInfo() {
        return String.format("%s{id=%s, status=%s, sortOrder=%s, createBy=%s}", 
            this.getClass().getSimpleName(), this.getId(), this.status, this.sortOrder, this.getCreateBy());
    }
}