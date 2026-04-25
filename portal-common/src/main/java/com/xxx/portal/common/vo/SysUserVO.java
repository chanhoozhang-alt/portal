package com.xxx.portal.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息视图
 */
@Data
public class SysUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String empNo;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Long deptId;
    private String deptName;
    private Integer status;
    /** 角色列表 */
    private List<String> roles;
    /** 权限列表 */
    private List<String> permissions;
}
