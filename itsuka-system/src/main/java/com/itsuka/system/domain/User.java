package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.itsuka.core.domian.LoginUser;
import org.itsuka.mybatis.plus.domain.BaseDomain;
import org.springframework.security.core.GrantedAuthority;

/**
 * 系统用户表
 *
 * @author itsuka
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "user")
@Data
public class User extends BaseDomain {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 是否锁定
     */
    private Boolean lock;
}