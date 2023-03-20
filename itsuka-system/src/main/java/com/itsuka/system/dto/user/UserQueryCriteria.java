package com.itsuka.system.dto.user;

import lombok.Data;

/**
 * UserQueryCriteria
 *
 * @author pc
 */
@Data
public class UserQueryCriteria {
    private String username;

    private String nickname;

    private Boolean enable;
}
