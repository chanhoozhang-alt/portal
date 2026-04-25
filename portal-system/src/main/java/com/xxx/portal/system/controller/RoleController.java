package com.xxx.portal.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxx.portal.common.annotation.OperLog;
import com.xxx.portal.common.model.SysRole;
import com.xxx.portal.common.result.R;
import com.xxx.portal.system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 分页查询角色
     */
    @GetMapping("/page")
    public R<IPage<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName) {
        return R.ok(roleService.page(new Page<>(pageNum, pageSize), roleName));
    }

    /**
     * 查询所有角色（下拉选择用）
     */
    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(roleService.list());
    }

    /**
     * 新增角色
     */
    @OperLog("新增角色")
    @PostMapping
    public R<Void> add(@RequestBody SysRole role) {
        roleService.add(role);
        return R.ok();
    }

    /**
     * 修改角色
     */
    @OperLog("修改角色")
    @PutMapping
    public R<Void> update(@RequestBody SysRole role) {
        roleService.update(role);
        return R.ok();
    }

    /**
     * 删除角色
     */
    @OperLog("删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    /**
     * 分配菜单权限
     */
    @OperLog("分配角色菜单")
    @PostMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }

    /**
     * 获取角色已分配的菜单 ID
     */
    @GetMapping("/{id}/menus")
    public R<List<Long>> getRoleMenuIds(@PathVariable Long id) {
        return R.ok(roleService.getRoleMenuIds(id));
    }

    /**
     * 分配数据权限（部门）
     */
    @OperLog("分配角色数据权限")
    @PostMapping("/{id}/depts")
    public R<Void> assignDepts(@PathVariable Long id, @RequestBody List<Long> deptIds) {
        roleService.assignDepts(id, deptIds);
        return R.ok();
    }

    /**
     * 获取角色已分配的部门 ID
     */
    @GetMapping("/{id}/depts")
    public R<List<Long>> getRoleDeptIds(@PathVariable Long id) {
        return R.ok(roleService.getRoleDeptIds(id));
    }
}
