package com.xxx.portal.business.controller;

import com.xxx.portal.common.model.PortalApp;
import com.xxx.portal.common.model.PortalCategory;
import com.xxx.portal.common.result.R;
import com.xxx.portal.business.service.AppService;
import com.xxx.portal.business.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门户首页控制器
 */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AppService appService;

    /**
     * 获取门户首页数据（分类+应用）
     */
    @GetMapping("/index")
    public R<Map<String, List<PortalApp>>> index() {
        List<PortalCategory> categories = categoryService.listEnabled();
        List<PortalApp> apps = appService.listEnabled();

        // 按分类分组
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(PortalCategory::getId, PortalCategory::getCategoryName));

        Map<String, List<PortalApp>> result = new LinkedHashMap<>();
        for (PortalCategory category : categories) {
            List<PortalApp> categoryApps = apps.stream()
                    .filter(app -> category.getId().equals(app.getCategoryId()))
                    .collect(Collectors.toList());
            result.put(category.getCategoryName(), categoryApps);
        }
        return R.ok(result);
    }
}
