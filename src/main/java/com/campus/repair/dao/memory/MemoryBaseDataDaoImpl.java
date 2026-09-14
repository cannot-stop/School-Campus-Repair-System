package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.dao.BaseDataDao;

/**
 * 基础数据数据访问内存实现。
 */
public class MemoryBaseDataDaoImpl implements BaseDataDao {

    @Override
    public int insert(String dataType, String dataValue, int sortNo) {
        MemoryStore.addBaseData(dataType, dataValue, sortNo);
        return 1;
    }

    @Override
    public int delete(String dataType, String dataValue) {
        return MemoryStore.removeBaseData(dataType, dataValue) ? 1 : 0;
    }

    @Override
    public List<String> findValues(String dataType) {
        Map<String, Integer> values = MemoryStore.BASE_DATA.get(dataType);
        if (values == null) {
            return new ArrayList<String>();
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(values.entrySet());
        entries.sort(new java.util.Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return a.getValue().compareTo(b.getValue());
            }
        });
        Map<String, Integer> sorted = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return new ArrayList<String>(sorted.keySet());
    }

    @Override
    public boolean exists(String dataType, String dataValue) {
        Map<String, Integer> values = MemoryStore.BASE_DATA.get(dataType);
        return values != null && values.containsKey(dataValue);
    }
}
