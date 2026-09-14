package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.campus.repair.dao.MaterialDao;
import com.campus.repair.domain.Material;

/**
 * 耗材数据访问内存实现。
 */
public class MemoryMaterialDaoImpl implements MaterialDao {

    @Override
    public int insert(Material material) {
        int id = MemoryStore.nextId(MemoryStore.MATERIAL_SEQ, material.getMatId());
        material.setMatId(Integer.valueOf(id));
        if (material.getStock() == null) {
            material.setStock(Integer.valueOf(0));
        }
        MemoryStore.MATERIALS.put(Integer.valueOf(id), material);
        return 1;
    }

    @Override
    public int update(Material material) {
        Material exists = MemoryStore.MATERIALS.get(material.getMatId());
        if (exists == null) {
            return 0;
        }
        exists.setMatName(material.getMatName());
        exists.setSpec(material.getSpec());
        exists.setStock(material.getStock());
        exists.setUnitPrice(material.getUnitPrice());
        return 1;
    }

    @Override
    public int delete(Integer matId) {
        return MemoryStore.MATERIALS.remove(matId) == null ? 0 : 1;
    }

    @Override
    public Material findById(Integer matId) {
        return MemoryStore.MATERIALS.get(matId);
    }

    @Override
    public Material findByNameAndSpec(String matName, String spec) {
        for (Material material : MemoryStore.MATERIALS.values()) {
            boolean nameEquals = material.getMatName() != null && material.getMatName().equals(matName);
            String existSpec = material.getSpec() == null ? "" : material.getSpec();
            String targetSpec = spec == null ? "" : spec;
            if (nameEquals && existSpec.equals(targetSpec)) {
                return material;
            }
        }
        return null;
    }

    @Override
    public List<Material> findAll() {
        List<Material> result = new ArrayList<Material>(MemoryStore.MATERIALS.values());
        result.sort(new Comparator<Material>() {
            @Override
            public int compare(Material a, Material b) {
                return a.getMatId().compareTo(b.getMatId());
            }
        });
        return result;
    }

    @Override
    public List<Material> findLowStock(int threshold) {
        List<Material> result = new ArrayList<Material>();
        for (Material material : MemoryStore.MATERIALS.values()) {
            int stock = material.getStock() == null ? 0 : material.getStock().intValue();
            if (stock < threshold) {
                result.add(material);
            }
        }
        result.sort(new Comparator<Material>() {
            @Override
            public int compare(Material a, Material b) {
                return (a.getStock() == null ? 0 : a.getStock()) - (b.getStock() == null ? 0 : b.getStock());
            }
        });
        return result;
    }

    @Override
    public int deductStock(Integer matId, int useCount) {
        Material exists = MemoryStore.MATERIALS.get(matId);
        if (exists == null) {
            return 0;
        }
        int stock = exists.getStock() == null ? 0 : exists.getStock().intValue();
        if (stock < useCount) {
            return 0;
        }
        exists.setStock(Integer.valueOf(stock - useCount));
        return 1;
    }

    @Override
    public int addStock(Integer matId, int count) {
        Material exists = MemoryStore.MATERIALS.get(matId);
        if (exists == null) {
            return 0;
        }
        int stock = exists.getStock() == null ? 0 : exists.getStock().intValue();
        exists.setStock(Integer.valueOf(stock + count));
        return 1;
    }
}
