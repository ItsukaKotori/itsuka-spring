package com.itsuka.system.convert;

import com.itsuka.system.domain.SysUser;
import com.itsuka.system.dto.user.UserCreateDto;
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

    SysUser convert(UserCreateDto createDto);
}
