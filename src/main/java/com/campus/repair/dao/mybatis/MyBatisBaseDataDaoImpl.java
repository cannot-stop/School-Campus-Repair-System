package com.campus.repair.dao.mybatis;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.BaseDataDao;
import com.campus.repair.dao.mapper.BaseDataMapper;

/**
 * 基础数据访问 MyBatis 实现（楼栋 / 报修类别 / 维修工种）。
 * SQL 见 {@code resources/mapper/BaseDataMapper.xml}。
 */
public class MyBatisBaseDataDaoImpl extends MyBatisDaoSupport implements BaseDataDao {

    private BaseDataMapper mapper(SqlSession session) {
        return session.getMapper(BaseDataMapper.class);
    }

    @Override
    public int insert(String dataType, String dataValue, int sortNo) {
        if (exists(dataType, dataValue)) {
            return 0;
        }
        return mutate(session -> mapper(session).insert(dataType, dataValue, sortNo));
    }

    @Override
    public int delete(String dataType, String dataValue) {
        return mutate(session -> mapper(session).delete(dataType, dataValue));
    }

    @Override
    public List<String> findValues(String dataType) {
        return query(session -> mapper(session).findValues(dataType));
    }

    @Override
    public boolean exists(String dataType, String dataValue) {
        Long count = query(session -> Long.valueOf(mapper(session).countByTypeAndValue(dataType, dataValue)));
        return count != null && count.longValue() > 0;
    }
}
