package com.itsuka.system.service;

import com.itsuka.system.dto.organization.OrganizationCreateDto;
import com.itsuka.system.dto.organization.OrganizationTreeDto;
import com.itsuka.system.dto.organization.OrganizationUpdateDto;
import com.itsuka.system.enums.OrganizationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 组织机构服务测试
 *
 * @author itsuka
 */
@SpringBootTest
@Transactional
public class OrganizationServiceTest {

    @Autowired
    private OrganizationService organizationService;

    @Test
    public void testCreateOrganization() {
        OrganizationCreateDto createDto = new OrganizationCreateDto();
        createDto.setName("测试部门");
        createDto.setCode("TEST_DEPT");
        createDto.setType(OrganizationType.DEPARTMENT.name());
        createDto.setParentId(1L);
        createDto.setSortOrder(1);
        createDto.setStatus(1);

        Boolean result = organizationService.create(createDto);
        assertTrue(result);
    }

    @Test
    public void testGetOrganizationTree() {
        List<OrganizationTreeDto> tree = organizationService.getOrganizationTree();
        assertNotNull(tree);
        assertFalse(tree.isEmpty());
    }

    @Test
    public void testUpdateOrganization() {
        // 先创建一个组织机构
        OrganizationCreateDto createDto = new OrganizationCreateDto();
        createDto.setName("测试部门");
        createDto.setCode("TEST_DEPT_UPDATE");
        createDto.setType(OrganizationType.DEPARTMENT.name());
        createDto.setParentId(1L);
        createDto.setSortOrder(1);
        createDto.setStatus(1);
        organizationService.create(createDto);

        // 更新组织机构
        OrganizationUpdateDto updateDto = new OrganizationUpdateDto();
        updateDto.setId(1L); // 使用已存在的ID
        updateDto.setName("更新后的部门名称");
        
        Boolean result = organizationService.update(updateDto);
        assertTrue(result);
    }

    @Test
    public void testCheckCodeExists() {
        // 测试已存在的编码
        Boolean exists = organizationService.checkCodeExists("ROOT_COMPANY", null);
        assertTrue(exists);

        // 测试不存在的编码
        Boolean notExists = organizationService.checkCodeExists("NON_EXISTENT_CODE", null);
        assertFalse(notExists);
    }

    @Test
    public void testGetByType() {
        List<com.itsuka.system.domain.Organization> companies = organizationService.getByType(OrganizationType.COMPANY.name());
        assertNotNull(companies);
        
        List<com.itsuka.system.domain.Organization> departments = organizationService.getByType(OrganizationType.DEPARTMENT.name());
        assertNotNull(departments);
        
        List<com.itsuka.system.domain.Organization> positions = organizationService.getByType(OrganizationType.POSITION.name());
        assertNotNull(positions);
    }

    @Test
    public void testGetChildrenByParentId() {
        List<com.itsuka.system.domain.Organization> children = organizationService.getChildrenByParentId(1L);
        assertNotNull(children);
    }
}