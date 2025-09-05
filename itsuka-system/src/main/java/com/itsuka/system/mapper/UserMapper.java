package com.itsuka.system.mapper;

import com.itsuka.system.domain.User;

import org.apache.ibatis.annotations.Mapper;
import org.itsuka.mybatis.plus.mapper.CustomMapper;

/**
 * SysUserMapper
 *
 * @author ituska
 */
@Mapper
public interface UserMapper extends CustomMapper<User> {

}




