package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * 基础数据 Mapper 接口（楼栋、报修类别、维修工种，对应设计书 1.1.1 系统管理员职责）。
 */
public interface BaseDataMapper {

    int insert(@Param("dataType") String dataType, @Param("dataValue") String dataValue,
               @Param("sortNo") int sortNo);

    int delete(@Param("dataType") String dataType, @Param("dataValue") String dataValue);

    List<String> findValues(@Param("dataType") String dataType);

    long countByTypeAndValue(@Param("dataType") String dataType, @Param("dataValue") String dataValue);
}
