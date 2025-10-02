package org.itsuka.mybatis.plus.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 软删除实体基类
 * 在 AuditableDomain 基础上添加逻辑删除和乐观锁功能
 * 适用于需要软删除的实体
 * 
 * @author Itsuka
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SoftDeleteDomain extends AuditableDomain {

    private static final long serialVersionUID = 1L;

    /**
     * 逻辑删除标记 (0: 未删除, 1: 已删除)
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(value = "deleted")
    @JsonIgnore
    private Integer deleted;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version")
    @JsonIgnore
    private Integer version;

    // ====== 常用状态常量 ======
    
    /**
     * 删除标记 - 未删除
     */
    public static final Integer DELETED_NO = 0;
    
    /**
     * 删除标记 - 已删除
     */
    public static final Integer DELETED_YES = 1;

    // ====== 便捷方法 ======

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return DELETED_YES.equals(this.deleted);
    }

    /**
     * 初始化新实体的默认值（带软删除支持）
     */
    @Override
    public void initForInsert() {
        super.initForInsert();
        this.deleted = DELETED_NO;
        this.version = 0;
    }

    /**
     * 初始化新实体的默认值（带用户信息和软删除支持）
     */
    public void initForInsert(String userId) {
        super.initForInsert(userId);
        this.deleted = DELETED_NO;
        this.version = 0;
    }

    /**
     * 获取实体的简要信息
     */
    @Override
    public String getBriefInfo() {
        return String.format("%s{id=%s, deleted=%s, version=%s, createBy=%s}", 
            this.getClass().getSimpleName(), this.getId(), this.deleted, this.version, this.getCreateBy());
    }
}