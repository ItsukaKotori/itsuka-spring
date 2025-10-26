package com.itsuka.system.convert;

import com.itsuka.system.domain.Organization;
import com.itsuka.system.dto.organization.OrganizationCreateDto;
import com.itsuka.system.dto.organization.OrganizationTreeDto;
import com.itsuka.system.dto.organization.OrganizationUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 组织机构转换类
 *
 * @author itsuka
 */
@Mapper
public interface OrganizationConvert {
    OrganizationConvert INSTANCE = Mappers.getMapper(OrganizationConvert.class);

    /**
     * 创建DTO转实体
     *
     * @param createDto 创建DTO
     * @return 组织机构实体
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createInfo", ignore = true)
    @Mapping(target = "updateInfo", ignore = true)
    Organization convert(OrganizationCreateDto createDto);

    /**
     * 更新DTO转实体
     *
     * @param updateDto 更新DTO
     * @return 组织机构实体
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createInfo", ignore = true)
    @Mapping(target = "updateInfo", ignore = true)
    Organization convert(OrganizationUpdateDto updateDto);

    /**
     * 实体转树形DTO
     *
     * @param organization 组织机构实体
     * @return 树形DTO
     */
    @Mapping(target = "children", ignore = true)
    OrganizationTreeDto convertToTreeDto(Organization organization);

    /**
     * 实体列表转树形DTO列表
     *
     * @param organizations 组织机构实体列表
     * @return 树形DTO列表
     */
    List<OrganizationTreeDto> convertToTreeDtoList(List<Organization> organizations);
}