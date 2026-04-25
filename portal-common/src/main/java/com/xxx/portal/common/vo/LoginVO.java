package com.xxx.portal.common.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 上游传入的 JWT Token */
    private String token;

    /** 工号（测试登录用） */
    private String empNo;
}
