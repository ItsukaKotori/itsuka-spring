package com.itsuka.system.convert;

import com.itsuka.system.domain.Role;
import com.itsuka.system.dto.role.RoleCreateDto;
import com.itsuka.system.dto.role.RoleUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * RoleConvert
 *
 * @author itsuka
 */
@Mapper
public interface RoleConvert {
    RoleConvert INSTANCE = Mappers.getMapper(RoleConvert.class);

    /**
     * convert
     *
     * @param createDto RoleCreateDto
     * @return Role
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    Role convert(RoleCreateDto createDto);

    /**
     * convert
     *
     * @param updateDto RoleUpdateDto
     * @return Role
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    Role convert(RoleUpdateDto updateDto);
}