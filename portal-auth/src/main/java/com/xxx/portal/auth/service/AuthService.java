package com.xxx.portal.auth.service;

import com.xxx.portal.common.exception.BusinessException;
import com.xxx.portal.common.feign.SystemFeignClient;
import com.xxx.portal.common.result.R;
import com.xxx.portal.common.utils.JwtUtils;
import com.xxx.portal.common.vo.SysUserVO;
import com.xxx.portal.common.vo.UserInfoVO;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private SystemFeignClient systemFeignClient;

    @Autowired
    private PermissionService permissionService;

    /**
     * 验证上游 JWT Token 并建立本地会话
     */
    public UserInfoVO verifyAndLogin(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(401, "Token 不能为空");
        }

        // 解析 token 中的工号
        String empNo = JwtUtils.parseEmpNo(token);
        if (StringUtils.isBlank(empNo)) {
            throw new BusinessException(401, "无效的 Token");
        }

        return doLogin(empNo);
    }

    /**
     * 测试登录：通过工号直接登录
     * 仅用于开发测试，上线前删除
     */
    public UserInfoVO testLogin(String empNo) {
        if (StringUtils.isBlank(empNo)) {
            throw new BusinessException(401, "工号不能为空");
        }
        return doLogin(empNo);
    }

    private UserInfoVO doLogin(String empNo) {
        // 通过 Feign 查询用户信息
        R<SysUserVO> userResult = systemFeignClient.getUserByEmpNo(empNo);
        if (userResult == null || userResult.getData() == null) {
            throw new BusinessException(401, "用户不存在");
        }

        SysUserVO user = userResult.getData();
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(401, "用户已被禁用");
        }

        // Sa-Token 建立会话
        StpUtil.login(user.getId());

        // 查询权限菜单
        UserInfoVO userInfo = permissionService.getUserInfo(user.getId());
        userInfo.setUser(user);
        // 返回 Sa-Token 供前端存储
        userInfo.setTokenValue(StpUtil.getTokenValue());

        log.info("用户 {} ({}) 登录成功", user.getRealName(), user.getEmpNo());
        return userInfo;
    }
}
