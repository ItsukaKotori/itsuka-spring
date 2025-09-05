package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsuka.system.domain.Resource;
import com.itsuka.system.mapper.ResourceMapper;
import com.itsuka.system.service.ResourceService;
import org.springframework.stereotype.Service;

/**
 * SysResourceServiceImpl
 *
 * @author itsuka
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource>
    implements ResourceService {

}




