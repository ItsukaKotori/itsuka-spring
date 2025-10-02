package org.itsuka.mybatis.plus.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多租户实体基类
 * 在 BusinessDomain 基础上添加多租户和扩展字段支持
 * 适用于需要多租户功能的实体
 * 
 * @author Itsuka
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TenantDomain extends BusinessDomain {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID（多租户支持）
     */
    @TableField(value = "tenant_id")
    @JsonIgnore
    private String tenantId;

    /**
     * 扩展字段（JSON格式）
     */
    @TableField(value = "extra_data")
    private String extraData;

    // ====== 便捷方法 ======

    /**
     * 设置租户信息
     */
    public void setTenantInfo(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 初始化新实体的默认值（带租户支持）
     */
    public void initForInsert(String userId, String tenantId) {
        super.initForInsert(userId);
        this.tenantId = tenantId;
    }

    /**
     * 复制基础信息到目标实体
     */
    public void copyBaseTo(TenantDomain target) {
        if (target != null) {
            target.setCreateTime(this.getCreateTime());
            target.setUpdateTime(this.getUpdateTime());
            target.setCreateBy(this.getCreateBy());
            target.setUpdateBy(this.getUpdateBy());
            target.setDeleted(this.getDeleted());
            target.setVersion(this.getVersion());
            target.setStatus(this.getStatus());
            target.setSortOrder(this.getSortOrder());
            target.setRemark(this.getRemark());
            target.setTenantId(this.tenantId);
            target.setExtraData(this.extraData);
        }
    }

    /**
     * 获取实体的简要信息
     */
    @Override
    public String getBriefInfo() {
        return String.format("%s{id=%s, tenantId=%s, status=%s, createBy=%s}", 
            this.getClass().getSimpleName(), this.getId(), this.tenantId, this.getStatus(), this.getCreateBy());
    }
}