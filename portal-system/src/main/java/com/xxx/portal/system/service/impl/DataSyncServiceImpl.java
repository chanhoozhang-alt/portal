package com.xxx.portal.system.service.impl;

import com.xxx.portal.system.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据同步默认实现（占位）
 * 待上游数据源确定后实现具体逻辑
 */
@Slf4j
@Service
public class DataSyncServiceImpl implements DataSyncService {

    @Override
    public int syncUsers() {
        log.info("人员数据同步 - 待实现，上游数据源未确定");
        return 0;
    }

    @Override
    public int syncDepts() {
        log.info("部门数据同步 - 待实现，上游数据源未确定");
        return 0;
    }
}
