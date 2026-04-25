package com.xxx.portal.system.controller;

import com.xxx.portal.common.result.R;
import com.xxx.portal.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限查询控制器（供 Feign 内部调用）
 */
@RestController
@RequestMapping("/api/system")
public class PermissionController {

    @Autowired
    private UserService userService;

    /**
     * 查询用户权限列表
     */
    @GetMapping("/permissions")
    public R<List<String>> getPermissions(@RequestParam("userId") Long userId) {
        return R.ok(userService.getUserPermissions(userId));
    }
}
