package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.campus.repair.dao.WorkerDao;
import com.campus.repair.domain.Worker;

/**
 * 维修人员数据访问内存实现。
 */
public class MemoryWorkerDaoImpl implements WorkerDao {

    @Override
    public int insert(Worker worker) {
        int id = MemoryStore.nextId(MemoryStore.WORKER_SEQ, worker.getWorkerId());
        worker.setWorkerId(Integer.valueOf(id));
        if (worker.getCurrentOrders() == null) {
            worker.setCurrentOrders(Integer.valueOf(0));
        }
        if (worker.getStatus() == null) {
            worker.setStatus(Integer.valueOf(1));
        }
        MemoryStore.WORKERS.put(Integer.valueOf(id), worker);
        return 1;
    }

    @Override
    public int update(Worker worker) {
        Worker exists = MemoryStore.WORKERS.get(worker.getWorkerId());
        if (exists == null) {
            return 0;
        }
        exists.setName(worker.getName());
        exists.setPhone(worker.getPhone());
        exists.setSkillTags(worker.getSkillTags());
        exists.setCurrentOrders(worker.getCurrentOrders());
        exists.setLocation(worker.getLocation());
        exists.setStatus(worker.getStatus());
        return 1;
    }

    @Override
    public int updateStatus(Integer workerId, Integer status) {
        Worker exists = MemoryStore.WORKERS.get(workerId);
        if (exists == null) {
            return 0;
        }
        exists.setStatus(status);
        return 1;
    }

    @Override
    public int updateLocation(Integer workerId, String location) {
        Worker exists = MemoryStore.WORKERS.get(workerId);
        if (exists == null) {
            return 0;
        }
        exists.setLocation(location);
        return 1;
    }

    @Override
    public int addCurrentOrders(Integer workerId, int delta) {
        Worker exists = MemoryStore.WORKERS.get(workerId);
        if (exists == null) {
            return 0;
        }
        int current = exists.getCurrentOrders() == null ? 0 : exists.getCurrentOrders().intValue();
        exists.setCurrentOrders(Integer.valueOf(Math.max(current + delta, 0)));
        return 1;
    }

    @Override
    public int delete(Integer workerId) {
        return MemoryStore.WORKERS.remove(workerId) == null ? 0 : 1;
    }

    @Override
    public Worker findById(Integer workerId) {
        return MemoryStore.WORKERS.get(workerId);
    }

    @Override
    public Worker findByUserId(Integer userId) {
        for (Worker worker : MemoryStore.WORKERS.values()) {
            if (userId.equals(worker.getUserId())) {
                return worker;
            }
        }
        return null;
    }

    @Override
    public List<Worker> findAll() {
        List<Worker> result = new ArrayList<Worker>(MemoryStore.WORKERS.values());
        result.sort(new Comparator<Worker>() {
            @Override
            public int compare(Worker a, Worker b) {
                return a.getWorkerId().compareTo(b.getWorkerId());
            }
        });
        return result;
    }

    @Override
    public List<Worker> findAvailable() {
        List<Worker> result = new ArrayList<Worker>();
        for (Worker worker : MemoryStore.WORKERS.values()) {
            if (worker.isOnline()) {
                result.add(worker);
            }
        }
        result.sort(new Comparator<Worker>() {
            @Override
            public int compare(Worker a, Worker b) {
                return a.currentOrderCount() - b.currentOrderCount();
            }
        });
        return result;
    }

    @Override
    public List<Worker> findBySkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) {
            return findAll();
        }
        List<Worker> result = new ArrayList<Worker>();
        for (Worker worker : MemoryStore.WORKERS.values()) {
            String tags = worker.getSkillTags() == null ? "" : worker.getSkillTags();
            if (tags.contains(skill.trim())) {
                result.add(worker);
            }
        }
        result.sort(new Comparator<Worker>() {
            @Override
            public int compare(Worker a, Worker b) {
                return a.currentOrderCount() - b.currentOrderCount();
            }
        });
        return result;
    }
}
