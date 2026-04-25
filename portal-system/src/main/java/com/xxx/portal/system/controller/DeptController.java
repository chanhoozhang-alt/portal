package com.xxx.portal.system.controller;

import com.xxx.portal.common.annotation.OperLog;
import com.xxx.portal.common.model.SysDept;
import com.xxx.portal.common.result.R;
import com.xxx.portal.common.vo.DeptVO;
import com.xxx.portal.system.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@RestController
@RequestMapping("/api/system/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 查询部门列表
     */
    @GetMapping("/list")
    public R<List<SysDept>> list() {
        return R.ok(deptService.list());
    }

    /**
     * 查询部门树
     */
    @GetMapping("/tree")
    public R<List<DeptVO>> tree() {
        return R.ok(deptService.tree());
    }

    /**
     * 新增部门
     */
    @OperLog("新增部门")
    @PostMapping
    public R<Void> add(@RequestBody SysDept dept) {
        deptService.add(dept);
        return R.ok();
    }

    /**
     * 修改部门
     */
    @OperLog("修改部门")
    @PutMapping
    public R<Void> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return R.ok();
    }

    /**
     * 删除部门
     */
    @OperLog("删除部门")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}
