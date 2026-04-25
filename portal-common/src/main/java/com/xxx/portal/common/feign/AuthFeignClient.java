package com.xxx.portal.common.feign;

import com.xxx.portal.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * auth-svc 暴露给其他服务的接口
 */
@FeignClient(name = "portal-auth", path = "/api/auth")
public interface AuthFeignClient {

    @PostMapping("/refreshCache")
    R<Void> refreshPermissionCache();
}
