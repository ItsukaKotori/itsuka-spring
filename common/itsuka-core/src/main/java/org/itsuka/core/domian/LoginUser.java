package org.itsuka.core.domian;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录用户信息
 *
 * @author Itsuka
 * @since 2023/2/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser {
    /**
     * 用户ID
     */
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
     * 权限列表
     */
    private List<String> authorities;
}
