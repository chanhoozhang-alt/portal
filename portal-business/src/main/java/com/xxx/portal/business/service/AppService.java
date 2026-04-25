package com.xxx.portal.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxx.portal.common.exception.BusinessException;
import com.xxx.portal.common.model.PortalApp;
import com.xxx.portal.business.mapper.PortalAppMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用管理服务
 */
@Service
public class AppService {

    @Autowired
    private PortalAppMapper appMapper;

    /**
     * 查询所有启用的应用（门户首页展示）
     */
    public List<PortalApp> listEnabled() {
        return appMapper.selectList(new LambdaQueryWrapper<PortalApp>()
                .eq(PortalApp::getStatus, 1)
                .orderByAsc(PortalApp::getSort));
    }

    /**
     * 分页查询应用（管理页面）
     */
    public IPage<PortalApp> page(Page<PortalApp> page, String appName, Integer status) {
        LambdaQueryWrapper<PortalApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(appName), PortalApp::getAppName, appName)
                .eq(status != null, PortalApp::getStatus, status)
                .orderByAsc(PortalApp::getSort)
                .orderByDesc(PortalApp::getCreateTime);
        return appMapper.selectPage(page, wrapper);
    }

    public PortalApp getById(Long id) {
        return appMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(PortalApp app) {
        appMapper.insert(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(PortalApp app) {
        appMapper.updateById(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        appMapper.deleteById(id);
    }
}
