package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.campus.repair.dao.ProgressDao;
import com.campus.repair.domain.Progress;

/**
 * 维修进度反馈数据访问内存实现。
 */
public class MemoryProgressDaoImpl implements ProgressDao {

    @Override
    public int insert(Progress progress) {
        int id = MemoryStore.nextId(MemoryStore.PROGRESS_SEQ, progress.getProgressId());
        progress.setProgressId(Integer.valueOf(id));
        if (progress.getCreateTime() == null) {
            progress.setCreateTime(new Date());
        }
        MemoryStore.PROGRESSES.put(Integer.valueOf(id), progress);
        return 1;
    }

    @Override
    public int deleteByTask(Integer taskId) {
        int count = 0;
        for (Progress progress : new ArrayList<Progress>(MemoryStore.PROGRESSES.values())) {
            if (taskId.equals(progress.getTaskId())) {
                MemoryStore.PROGRESSES.remove(progress.getProgressId());
                count++;
            }
        }
        return count;
    }

    @Override
    public List<Progress> findByTask(Integer taskId) {
        List<Progress> result = new ArrayList<Progress>();
        for (Progress progress : MemoryStore.PROGRESSES.values()) {
            if (taskId.equals(progress.getTaskId())) {
                result.add(progress);
            }
        }
        result.sort(new Comparator<Progress>() {
            @Override
            public int compare(Progress a, Progress b) {
                return a.getProgressId().compareTo(b.getProgressId());
            }
        });
        return result;
    }

    @Override
    public List<Progress> findAll() {
        List<Progress> result = new ArrayList<Progress>(MemoryStore.PROGRESSES.values());
        result.sort(new Comparator<Progress>() {
            @Override
            public int compare(Progress a, Progress b) {
                return b.getProgressId().compareTo(a.getProgressId());
            }
        });
        return result;
    }
}
