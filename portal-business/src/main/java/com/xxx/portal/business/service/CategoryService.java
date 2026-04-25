package com.xxx.portal.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxx.portal.common.exception.BusinessException;
import com.xxx.portal.common.model.PortalCategory;
import com.xxx.portal.business.mapper.PortalCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类管理服务
 */
@Service
public class CategoryService {

    @Autowired
    private PortalCategoryMapper categoryMapper;

    /**
     * 查询所有启用的分类
     */
    public List<PortalCategory> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<PortalCategory>()
                .eq(PortalCategory::getStatus, 1)
                .orderByAsc(PortalCategory::getSort));
    }

    /**
     * 查询所有分类
     */
    public List<PortalCategory> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<PortalCategory>()
                .orderByAsc(PortalCategory::getSort));
    }

    public PortalCategory getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(PortalCategory category) {
        categoryMapper.insert(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(PortalCategory category) {
        categoryMapper.updateById(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }
}
