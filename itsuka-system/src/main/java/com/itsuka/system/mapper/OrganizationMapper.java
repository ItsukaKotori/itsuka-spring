package com.itsuka.system.mapper;

import com.itsuka.system.domain.Organization;
import com.itsuka.system.dto.organization.OrganizationTreeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.itsuka.mybatis.plus.mapper.CustomMapper;

import java.util.List;

/**
 * 组织机构Mapper
 *
 * @author itsuka
 */
@Mapper
public interface OrganizationMapper extends CustomMapper<Organization> {

    /**
     * 检查编码是否存在
     * 这个方法无法通过BaseMapper实现，需要自定义SQL
     *
     * @param code 编码
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 数量
     */
    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);
}