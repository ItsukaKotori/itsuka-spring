package com.itsuka.system.convert;

import com.itsuka.system.domain.User;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserUpdateDto;
import org.itsuka.core.domain.LoginUser;
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
     * @return User
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createInfo", ignore = true)
    @Mapping(target = "updateInfo", ignore = true)
    User convert(UserCreateDto createDto);

    /**
     * convert
     *
     * @param updateDto UserUpdateDto
     * @return User
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createInfo", ignore = true)
    @Mapping(target = "updateInfo", ignore = true)
    @Mapping(target = "id", ignore = true)
    User convert(UserUpdateDto updateDto);

    /**
     * convert User to LoginUser
     *
     * @param user User
     * @param authorities Collection<? extends GrantedAuthority>
     * @return LoginUser
     */
    @Mapping(target = "authorities", source = "authorities")
    LoginUser toLoginUser(User user, Collection<? extends GrantedAuthority> authorities);
}
