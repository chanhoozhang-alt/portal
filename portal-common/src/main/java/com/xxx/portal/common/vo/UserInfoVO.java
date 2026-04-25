package com.xxx.portal.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录成功返回的用户信息
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private SysUserVO user;
    private List<MenuVO> menus;
    private List<String> permissions;
    /** Sa-Token 值 */
    private String tokenValue;
}
