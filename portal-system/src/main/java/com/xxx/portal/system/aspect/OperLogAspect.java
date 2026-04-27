package com.xxx.portal.system.aspect;

import com.xxx.portal.common.annotation.OperLog;
import com.xxx.portal.common.model.SysOperationLog;
import com.xxx.portal.common.result.R;
import com.xxx.portal.system.service.LogService;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Autowired
    private LogService logService;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        // 构建日志对象
        SysOperationLog operationLog = new SysOperationLog();

        try {
            operationLog.setUserId(StpUtil.getLoginIdAsLong());
            cn.dev33.satoken.session.SaSession session = StpUtil.getSession(false);
            String realName = session != null ? (String) session.get("realName") : null;
            operationLog.setUsername(realName != null ? realName : StpUtil.getLoginIdAsString());
        } catch (Exception e) {
            // 未登录的情况
        }
        operationLog.setOperation(operLog.value());

        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            operationLog.setRequestUrl(request.getRequestURI());
            operationLog.setMethod(request.getMethod());
            operationLog.setIp(getClientIp(request));
        }

        // 记录请求参数
        try {
            Object[] args = pjp.getArgs();
            if (args != null && args.length > 0) {
                operationLog.setRequestParams(objectMapper.writeValueAsString(args));
            }
        } catch (Exception e) {
            log.warn("序列化请求参数失败", e);
        }

        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long costTime = System.currentTimeMillis() - start;

        operationLog.setCostTime(costTime);
        operationLog.setResponseCode(R.getCode(result));

        // 异步保存日志
        logService.saveAsync(operationLog);

        return result;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多次代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
