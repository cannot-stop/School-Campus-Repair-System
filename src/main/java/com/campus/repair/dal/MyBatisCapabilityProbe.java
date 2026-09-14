package com.campus.repair.dal;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.mybatis.MyBatisSessionFactory;

import org.apache.ibatis.session.Configuration;

/**
 * 数据访问层自检（诊断用）：报告当前 DAO 实现类与 MyBatis 语句注册情况。
 *
 * <p>用途：部署后确认"到底走的是哪一套数据访问实现"，避免出现
 * "配置写了 mybatis 但仍在使用手写 SQL" 这类不易察觉的问题。</p>
 */
public final class MyBatisCapabilityProbe {

    private MyBatisCapabilityProbe() {
    }

    /** 返回指定 DAO 的实际实现类名（如 userDao、workerDao、repairOrderDao……） */
    public static String daoClass(String daoName) {
        try {
            Object dao;
            if ("userDao".equals(daoName)) {
                dao = DaoFactory.userDao();
            } else if ("workerDao".equals(daoName)) {
                dao = DaoFactory.workerDao();
            } else if ("repairOrderDao".equals(daoName)) {
                dao = DaoFactory.repairOrderDao();
            } else if ("repairTaskDao".equals(daoName)) {
                dao = DaoFactory.repairTaskDao();
            } else if ("materialDao".equals(daoName)) {
                dao = DaoFactory.materialDao();
            } else if ("materialUsageDao".equals(daoName)) {
                dao = DaoFactory.materialUsageDao();
            } else if ("evaluationDao".equals(daoName)) {
                dao = DaoFactory.evaluationDao();
            } else if ("messageDao".equals(daoName)) {
                dao = DaoFactory.messageDao();
            } else if ("progressDao".equals(daoName)) {
                dao = DaoFactory.progressDao();
            } else if ("baseDataDao".equals(daoName)) {
                dao = DaoFactory.baseDataDao();
            } else {
                return "UNKNOWN-DAO:" + daoName;
            }
            return dao.getClass().getName();
        } catch (RuntimeException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /** 已注册的 Mapper 语句总数（0 表示 MyBatis 未生效） */
    public static int mappedStatementCount() {
        if (!DaoFactory.isMyBatisMode()) {
            return 0;
        }
        try {
            Configuration configuration = MyBatisSessionFactory.getFactory().getConfiguration();
            return configuration.getMappedStatementNames().size();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** 已注册的 Mapper 接口列表 */
    public static String mapperNames() {
        if (!DaoFactory.isMyBatisMode()) {
            return "（非 MyBatis 模式）";
        }
        try {
            Configuration configuration = MyBatisSessionFactory.getFactory().getConfiguration();
            StringBuilder sb = new StringBuilder();
            for (Class<?> mapper : configuration.getMapperRegistry().getMappers()) {
                if (sb.length() > 0) {
                    sb.append("、");
                }
                sb.append(mapper.getSimpleName());
            }
            return sb.toString();
        } catch (RuntimeException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
