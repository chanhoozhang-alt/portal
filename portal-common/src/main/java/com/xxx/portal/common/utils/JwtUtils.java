package com.xxx.portal.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * JWT 工具类
 */
public class JwtUtils {

    private JwtUtils() {}

    /**
     * 校验 token 是否有效
     */
    public static boolean verify(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 token 中解析工号
     */
    public static String parseEmpNo(String token) {
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null ? loginId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
