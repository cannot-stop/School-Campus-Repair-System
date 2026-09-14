package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.Progress;

/**
 * 维修进度反馈数据访问接口。
 */
public interface ProgressDao {

    int insert(Progress progress);

    int deleteByTask(Integer taskId);

    List<Progress> findByTask(Integer taskId);

    List<Progress> findAll();
}
