package com.xxx.portal.business.controller;

import com.xxx.portal.common.annotation.OperLog;
import com.xxx.portal.common.model.PortalCategory;
import com.xxx.portal.common.result.R;
import com.xxx.portal.business.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询所有分类
     */
    @GetMapping("/list")
    public R<List<PortalCategory>> list() {
        return R.ok(categoryService.list());
    }

    /**
     * 查询启用的分类（门户首页用）
     */
    @GetMapping("/enabled")
    public R<List<PortalCategory>> listEnabled() {
        return R.ok(categoryService.listEnabled());
    }

    /**
     * 新增分类
     */
    @OperLog("新增应用分类")
    @PostMapping
    public R<Void> add(@RequestBody PortalCategory category) {
        categoryService.add(category);
        return R.ok();
    }

    /**
     * 修改分类
     */
    @OperLog("修改应用分类")
    @PutMapping
    public R<Void> update(@RequestBody PortalCategory category) {
        categoryService.update(category);
        return R.ok();
    }

    /**
     * 删除分类
     */
    @OperLog("删除应用分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
