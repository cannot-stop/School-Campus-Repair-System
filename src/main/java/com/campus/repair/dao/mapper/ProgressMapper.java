package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.Progress;

/**
 * 维修进度反馈 Mapper 接口（承载设计书表 1.10"进度反馈"与延期原因记录）。
 */
public interface ProgressMapper {

    int insert(Progress progress);

    int deleteByTask(@Param("taskId") Integer taskId);

    List<Progress> findByTask(@Param("taskId") Integer taskId);

    List<Progress> findAll();
}
