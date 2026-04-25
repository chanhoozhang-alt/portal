package com.xxx.portal.common.feign;

import com.xxx.portal.common.result.R;
import com.xxx.portal.common.vo.MenuVO;
import com.xxx.portal.common.vo.SysUserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * system-svc 暴露给其他服务的接口
 */
@FeignClient(name = "portal-system", path = "/api/system")
public interface SystemFeignClient {

    @GetMapping("/users/byEmpNo")
    R<SysUserVO> getUserByEmpNo(@RequestParam("empNo") String empNo);

    @GetMapping("/menus/tree")
    R<List<MenuVO>> getMenuTree(@RequestParam("userId") Long userId);

    @GetMapping("/permissions")
    R<List<String>> getPermissions(@RequestParam("userId") Long userId);
}
