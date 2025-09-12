package com.itsuka.system.convert;

import com.itsuka.system.domain.Resource;
import com.itsuka.system.dto.resource.ResourceCreateDto;
import com.itsuka.system.dto.resource.ResourceUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * ResourceConvert
 *
 * @author itsuka
 */
@Mapper
public interface ResourceConvert {
    ResourceConvert INSTANCE = Mappers.getMapper(ResourceConvert.class);

    /**
     * convert
     *
     * @param createDto ResourceCreateDto
     * @return Resource
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    Resource convert(ResourceCreateDto createDto);

    /**
     * convert
     *
     * @param updateDto ResourceUpdateDto
     * @return Resource
     */
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    Resource convert(ResourceUpdateDto updateDto);
}