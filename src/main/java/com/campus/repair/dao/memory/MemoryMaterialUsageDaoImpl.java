package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.campus.repair.dao.MaterialUsageDao;
import com.campus.repair.domain.Material;
import com.campus.repair.domain.MaterialUsage;

/**
 * 耗材使用数据访问内存实现。
 */
public class MemoryMaterialUsageDaoImpl implements MaterialUsageDao {

    @Override
    public int insert(MaterialUsage usage) {
        int id = MemoryStore.nextId(MemoryStore.USAGE_SEQ, usage.getUseId());
        usage.setUseId(Integer.valueOf(id));
        if (usage.getUseTime() == null) {
            usage.setUseTime(new Date());
        }
        MemoryStore.USAGES.put(Integer.valueOf(id), usage);
        return 1;
    }

    @Override
    public int updateCount(Integer useId, int useCount) {
        MaterialUsage exists = MemoryStore.USAGES.get(useId);
        if (exists == null) {
            return 0;
        }
        exists.setUseCount(Integer.valueOf(useCount));
        return 1;
    }

    @Override
    public int delete(Integer useId) {
        return MemoryStore.USAGES.remove(useId) == null ? 0 : 1;
    }

    @Override
    public int deleteByTask(Integer taskId) {
        int count = 0;
        for (MaterialUsage usage : new ArrayList<MaterialUsage>(MemoryStore.USAGES.values())) {
            if (taskId.equals(usage.getTaskId())) {
                MemoryStore.USAGES.remove(usage.getUseId());
                count++;
            }
        }
        return count;
    }

    @Override
    public MaterialUsage findById(Integer useId) {
        return enrich(MemoryStore.USAGES.get(useId));
    }

    @Override
    public MaterialUsage findByTaskAndMaterial(Integer taskId, Integer matId) {
        for (MaterialUsage usage : MemoryStore.USAGES.values()) {
            if (taskId.equals(usage.getTaskId()) && matId.equals(usage.getMatId())) {
                return enrich(usage);
            }
        }
        return null;
    }

    @Override
    public List<MaterialUsage> findByTask(Integer taskId) {
        List<MaterialUsage> result = new ArrayList<MaterialUsage>();
        for (MaterialUsage usage : MemoryStore.USAGES.values()) {
            if (taskId.equals(usage.getTaskId())) {
                result.add(enrich(usage));
            }
        }
        result.sort(new Comparator<MaterialUsage>() {
            @Override
            public int compare(MaterialUsage a, MaterialUsage b) {
                return a.getUseId().compareTo(b.getUseId());
            }
        });
        return result;
    }

    @Override
    public List<MaterialUsage> findAll() {
        List<MaterialUsage> result = new ArrayList<MaterialUsage>();
        for (MaterialUsage usage : MemoryStore.USAGES.values()) {
            result.add(enrich(usage));
        }
        result.sort(new Comparator<MaterialUsage>() {
            @Override
            public int compare(MaterialUsage a, MaterialUsage b) {
                return a.getUseId().compareTo(b.getUseId());
            }
        });
        return result;
    }

    /** 带出耗材名称与单价 */
    public static MaterialUsage enrich(MaterialUsage usage) {
        if (usage == null) {
            return null;
        }
        Material material = MemoryStore.MATERIALS.get(usage.getMatId());
        if (material != null) {
            usage.setMatName(material.getMatName());
            usage.setSpec(material.getSpec());
            usage.setUnitPrice(material.getUnitPrice());
        }
        return usage;
    }
}
