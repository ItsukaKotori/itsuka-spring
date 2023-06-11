package com.itsuka.system.convert;

import com.itsuka.system.domain.SysUser;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

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
    SysUser convert(UserCreateDto createDto);

    /**
     * convert
     *
     * @param updateDto UserUpdateDto
     * @return SysUser
     */
    SysUser convert(UserUpdateDto updateDto);
}
