package com.xxx.portal.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxx.portal.common.annotation.OperLog;
import com.xxx.portal.common.result.R;
import com.xxx.portal.common.model.SysUser;
import com.xxx.portal.common.vo.SysUserVO;
import com.xxx.portal.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人员管理控制器
 */
@RestController
@RequestMapping("/api/system/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 根据工号查询用户（Feign 内部调用）
     */
    @GetMapping("/byEmpNo")
    public R<SysUserVO> getByEmpNo(@RequestParam("empNo") String empNo) {
        SysUserVO user = userService.getUserByEmpNo(empNo);
        if (user == null) {
            return R.fail("用户不存在");
        }
        return R.ok(user);
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    public R<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String empNo,
            @RequestParam(required = false) Integer status) {
        return R.ok(userService.page(new Page<>(pageNum, pageSize), realName, empNo, status));
    }

    /**
     * 更新用户状态
     */
    @OperLog("更新用户状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 分配角色
     */
    @OperLog("分配用户角色")
    @PostMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    /**
     * 获取用户角色
     */
    @GetMapping("/{id}/roles")
    public R<?> getUserRoles(@PathVariable Long id) {
        return R.ok(userService.getUserRoles(id));
    }
}
