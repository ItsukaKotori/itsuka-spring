package org.itsuka.mybatis.plus.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审计实体基类
 * 在 BaseDomain 基础上添加创建人和更新人字段
 * 适用于需要审计功能的实体
 * 
 * @author Itsuka
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AuditableDomain extends BaseDomain {

    private static final long serialVersionUID = 1L;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    // ====== 便捷方法 ======

    /**
     * 设置创建信息
     */
    public void setCreateInfo(String userId) {
        this.createBy = userId;
        this.setCreateTime(LocalDateTime.now());
        this.updateBy = userId;
        this.setUpdateTime(this.getCreateTime());
    }

    /**
     * 设置更新信息
     */
    public void setUpdateInfo(String userId) {
        this.updateBy = userId;
        this.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 初始化新实体的默认值（带用户信息）
     */
    public void initForInsert(String userId) {
        super.initForInsert();
        setCreateInfo(userId);
    }

    /**
     * 获取实体的简要信息
     */
    @Override
    public String getBriefInfo() {
        return String.format("%s{id=%s, createBy=%s, createTime=%s}", 
            this.getClass().getSimpleName(), this.getId(), this.createBy, this.getCreateTime());
    }
}