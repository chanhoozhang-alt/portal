package com.xxx.portal.system.scheduled;

import com.xxx.portal.common.feign.AuthFeignClient;
import com.xxx.portal.system.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据同步定时调度
 */
@Slf4j
@Component
public class DataSyncScheduler {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private AuthFeignClient authFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 每天凌晨 2 点同步数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledSync() {
        log.info("===== 开始定时数据同步 =====");
        try {
            int deptCount = dataSyncService.syncDepts();
            log.info("部门同步完成，共 {} 条", deptCount);

            int userCount = dataSyncService.syncUsers();
            log.info("人员同步完成，共 {} 条", userCount);

            // 同步完成后清理缓存
            afterSync();
            log.info("===== 定时数据同步完成 =====");
        } catch (Exception e) {
            log.error("数据同步失败", e);
        }
    }

    private void afterSync() {
        // 清理 Redis 权限缓存（使用 SCAN 避免 KEYS 阻塞）
        scanAndDelete("perm:*");
        scanAndDelete("menu:*");

        // 通知 auth-svc 刷新缓存
        try {
            authFeignClient.refreshPermissionCache();
        } catch (Exception e) {
            log.warn("通知 auth-svc 刷新缓存失败", e);
        }
    }

    private void scanAndDelete(String pattern) {
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            List<String> keys = new ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("扫描删除缓存失败, pattern={}", pattern, e);
        }
    }
}
