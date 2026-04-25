package com.xxx.portal.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxx.portal.common.model.SysOperationLog;
import com.xxx.portal.common.result.R;
import com.xxx.portal.system.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/system/logs")
public class LogController {

    @Autowired
    private LogService logService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    public R<IPage<SysOperationLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation) {
        return R.ok(logService.page(new Page<>(pageNum, pageSize), username, operation));
    }
}
