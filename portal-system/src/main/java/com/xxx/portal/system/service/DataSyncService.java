package com.xxx.portal.system.service;

/**
 * 数据同步抽象接口
 * 具体实现取决于上游数据源（API、数据库、文件等）
 */
public interface DataSyncService {

    /**
     * 同步人员数据
     * @return 同步的记录数
     */
    int syncUsers();

    /**
     * 同步部门数据
     * @return 同步的记录数
     */
    int syncDepts();
}
