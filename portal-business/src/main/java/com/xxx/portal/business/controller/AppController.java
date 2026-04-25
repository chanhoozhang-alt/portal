package com.xxx.portal.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxx.portal.common.annotation.OperLog;
import com.xxx.portal.common.model.PortalApp;
import com.xxx.portal.common.result.R;
import com.xxx.portal.business.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理控制器
 */
@RestController
@RequestMapping("/api/app")
public class AppController {

    @Autowired
    private AppService appService;

    /**
     * 分页查询应用
     */
    @GetMapping("/page")
    public R<IPage<PortalApp>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Integer status) {
        return R.ok(appService.page(new Page<>(pageNum, pageSize), appName, status));
    }

    /**
     * 查询所有启用应用（门户首页用）
     */
    @GetMapping("/list")
    public R<List<PortalApp>> list() {
        return R.ok(appService.listEnabled());
    }

    /**
     * 查询应用详情
     */
    @GetMapping("/{id}")
    public R<PortalApp> getById(@PathVariable Long id) {
        return R.ok(appService.getById(id));
    }

    /**
     * 新增应用
     */
    @OperLog("新增应用")
    @PostMapping
    public R<Void> add(@RequestBody PortalApp app) {
        appService.add(app);
        return R.ok();
    }

    /**
     * 修改应用
     */
    @OperLog("修改应用")
    @PutMapping
    public R<Void> update(@RequestBody PortalApp app) {
        appService.update(app);
        return R.ok();
    }

    /**
     * 删除应用
     */
    @OperLog("删除应用")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return R.ok();
    }
}
