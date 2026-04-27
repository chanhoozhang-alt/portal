package com.xxx.portal.auth.service;

import com.xxx.portal.common.feign.SystemFeignClient;
import com.xxx.portal.common.result.R;
import com.xxx.portal.common.vo.MenuVO;
import com.xxx.portal.common.vo.UserInfoVO;
import com.xxx.portal.common.vo.SysUserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限服务
 */
@Slf4j
@Service
public class PermissionService {

    @Autowired
    private SystemFeignClient systemFeignClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PERM_CACHE_PREFIX = "perm:";
    private static final String MENU_CACHE_PREFIX = "menu:";
    private static final long CACHE_TTL_HOURS = 2;

    /**
     * 获取用户完整信息（用户、菜单、权限）
     */
    public UserInfoVO getUserInfo(Long userId) {
        UserInfoVO userInfo = new UserInfoVO();

        // 获取菜单树
        List<MenuVO> menus = getMenuTree(userId);
        userInfo.setMenus(menus);

        // 获取权限列表
        List<String> permissions = getPermissions(userId);
        userInfo.setPermissions(permissions);

        return userInfo;
    }

    /**
     * 获取用户菜单树（带缓存）
     */
    @SuppressWarnings("unchecked")
    public List<MenuVO> getMenuTree(Long userId) {
        String cacheKey = MENU_CACHE_PREFIX + userId;

        // 查缓存
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<MenuVO>) cached;
            }
        } catch (Exception e) {
            log.warn("读取菜单缓存失败", e);
        }

        // 查数据库
        R<List<MenuVO>> result = systemFeignClient.getMenuTree(userId);
        if (result != null && result.getData() != null) {
            // 写入缓存
            try {
                redisTemplate.opsForValue().set(cacheKey,
                    result.getData(), CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("缓存菜单数据失败", e);
            }
            return result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 获取用户权限列表（带缓存）
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissions(Long userId) {
        String cacheKey = PERM_CACHE_PREFIX + userId;

        // 查缓存
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.warn("读取权限缓存失败", e);
        }

        // 查数据库
        R<List<String>> result = systemFeignClient.getPermissions(userId);
        if (result != null && result.getData() != null) {
            // 写入缓存
            try {
                redisTemplate.opsForValue().set(cacheKey,
                    result.getData(), CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("缓存权限数据失败", e);
            }
            return result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * 刷新权限缓存
     */
    public void refreshCache() {
        log.info("收到刷新权限缓存通知");
        scanAndDelete(PERM_CACHE_PREFIX + "*");
        scanAndDelete(MENU_CACHE_PREFIX + "*");
    }

    @SuppressWarnings("unchecked")
    private void scanAndDelete(String pattern) {
        try (Cursor<String> cursor = (Cursor<String>) redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            List<String> keys = new ArrayList<>();
            cursor.forEachRemaining(key -> keys.add((String) key));
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("扫描删除缓存失败, pattern={}", pattern, e);
        }
    }
}
