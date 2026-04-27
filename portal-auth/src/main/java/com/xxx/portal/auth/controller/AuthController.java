package com.xxx.portal.auth.controller;

import com.xxx.portal.common.result.R;
import com.xxx.portal.common.vo.LoginVO;
import com.xxx.portal.common.vo.MenuVO;
import com.xxx.portal.common.vo.UserInfoVO;
import com.xxx.portal.auth.service.AuthService;
import com.xxx.portal.auth.service.PermissionService;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 登录验证（上游传入 JWT Token）
     */
    @PostMapping("/verify")
    public R<UserInfoVO> verify(@RequestBody LoginVO loginVO) {
        UserInfoVO userInfo = authService.verifyAndLogin(loginVO.getToken());
        return R.ok(userInfo);
    }

    /**
     * 测试登录（通过工号直接登录，返回 Sa-Token）
     * 仅用于开发测试，上线前删除
     */
    @PostMapping("/test-login")
    public R<UserInfoVO> testLogin(@RequestBody LoginVO loginVO) {
        UserInfoVO userInfo = authService.testLogin(loginVO.getEmpNo());
        return R.ok(userInfo);
    }

    /**
     * 获取当前用户权限信息
     */
    @GetMapping("/permissions")
    public R<UserInfoVO> getPermissions() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserInfoVO userInfo = permissionService.getUserInfo(userId);
        return R.ok(userInfo);
    }

    /**
     * 获取当前用户菜单树
     */
    @GetMapping("/menus")
    public R<List<MenuVO>> getMenuTree() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(permissionService.getMenuTree(userId));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    /**
     * 刷新权限缓存（供其他服务 Feign 调用）
     */
    @PostMapping("/refreshCache")
    public R<Void> refreshPermissionCache() {
        permissionService.refreshCache();
        return R.ok();
    }
}
