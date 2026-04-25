package com.xxx.portal.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxx.portal.common.model.SysOperationLog;
import com.xxx.portal.system.mapper.SysOperationLogMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 */
@Service
public class LogService {

    @Autowired
    private SysOperationLogMapper logMapper;

    public IPage<SysOperationLog> page(Page<SysOperationLog> page, String username, String operation) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(username), SysOperationLog::getUsername, username)
                .like(StringUtils.isNotBlank(operation), SysOperationLog::getOperation, operation)
                .orderByDesc(SysOperationLog::getCreateTime);
        return logMapper.selectPage(page, wrapper);
    }

    /**
     * 异步保存操作日志
     */
    @Async
    public void saveAsync(SysOperationLog log) {
        logMapper.insert(log);
    }
}
