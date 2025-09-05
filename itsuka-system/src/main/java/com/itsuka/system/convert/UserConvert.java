package com.itsuka.system.convert;

import com.itsuka.system.domain.User;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserUpdateDto;
import org.itsuka.core.domian.LoginUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * UserConvert
 *
 * @author pc
 */
@Mapper
public interface UserConvert {
    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    /**
     * convert
     *
     * @param createDto UserCreateDto
     * @return SysUser
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "lock", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    User convert(UserCreateDto createDto);

    /**
     * convert
     *
     * @param updateDto UserUpdateDto
     * @return SysUser
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "lock", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    User convert(UserUpdateDto updateDto);

    LoginUser toLoginUser(User user, Collection<? extends GrantedAuthority> authorities);
}
