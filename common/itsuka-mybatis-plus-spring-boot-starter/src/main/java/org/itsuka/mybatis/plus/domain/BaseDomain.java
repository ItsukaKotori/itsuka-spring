package org.itsuka.mybatis.plus.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 只包含最核心的字段：主键、创建时间、更新时间
 * 
 * @author Itsuka
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    // ====== 便捷方法 ======

    /**
     * 初始化新实体的默认值
     */
    public void initForInsert() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    /**
     * 更新时间戳
     */
    public void updateTimestamp() {
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 获取实体的显示名称（由子类实现）
     */
    public String getDisplayName() {
        return this.getClass().getSimpleName() + "#" + this.id;
    }

    /**
     * 获取实体的简要信息
     */
    public String getBriefInfo() {
        return String.format("%s{id=%s, createTime=%s}", 
            this.getClass().getSimpleName(), this.id, this.createTime);
    }
}
