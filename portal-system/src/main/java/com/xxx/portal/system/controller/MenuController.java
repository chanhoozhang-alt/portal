package com.xxx.portal.system.controller;

import com.xxx.portal.common.annotation.OperLog;
import com.xxx.portal.common.model.SysMenu;
import com.xxx.portal.common.result.R;
import com.xxx.portal.common.vo.MenuVO;
import com.xxx.portal.system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 */
@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 获取菜单树（Feign 内部调用，按用户权限过滤）
     */
    @GetMapping("/tree")
    public R<List<MenuVO>> tree(@RequestParam("userId") Long userId) {
        return R.ok(menuService.treeByUserId(userId));
    }

    /**
     * 获取菜单列表
     */
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        return R.ok(menuService.list());
    }

    /**
     * 新增菜单
     */
    @OperLog("新增菜单")
    @PostMapping
    public R<Void> add(@RequestBody SysMenu menu) {
        menuService.add(menu);
        return R.ok();
    }

    /**
     * 修改菜单
     */
    @OperLog("修改菜单")
    @PutMapping
    public R<Void> update(@RequestBody SysMenu menu) {
        menuService.update(menu);
        return R.ok();
    }

    /**
     * 删除菜单
     */
    @OperLog("删除菜单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
