package com.campus.repair.dao;

import java.util.List;

/**
 * 基础数据数据访问接口（楼栋、报修类别、维修工种等，对应设计书 1.1.1 系统管理员职责）。
 */
public interface BaseDataDao {

    int insert(String dataType, String dataValue, int sortNo);

    int delete(String dataType, String dataValue);

    List<String> findValues(String dataType);

    boolean exists(String dataType, String dataValue);
}
