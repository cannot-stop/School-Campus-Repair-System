package com.campus.repair.dao;

import com.campus.repair.config.AppConfig;

/**
 * 数据访问工厂：根据 storage.mode 配置返回 MyBatis（MySQL）或内存实现。
 *
 * <p>storage.mode=mybatis → com.campus.repair.dao.mybatis.MyBatisXxxDaoImpl（MyBatis Mapper + MySQL，正式部署）<br>
 * storage.mode=memory  → com.campus.repair.dao.memory.MemoryXxxDaoImpl（内存库，用于无数据库环境的演示、自测与教学）</p>
 *
 * <p>两种实现遵循同一组 Dao 接口，Service 层无需感知差异，体现设计书"数据访问层屏蔽数据库访问细节"的设计目标。</p>
 */
public final class DaoFactory {

    private static UserDao userDao;
    private static WorkerDao workerDao;
    private static RepairOrderDao repairOrderDao;
    private static RepairTaskDao repairTaskDao;
    private static MaterialDao materialDao;
    private static MaterialUsageDao materialUsageDao;
    private static EvaluationDao evaluationDao;
    private static MessageDao messageDao;
    private static ProgressDao progressDao;
    private static BaseDataDao baseDataDao;

    private static String mode;

    private DaoFactory() {
    }

    public static synchronized void init() {
        AppConfig.load();
        if (mode != null) {
            return;
        }
        String configured = AppConfig.get("storage.mode", "mybatis").toLowerCase();
        if ("memory".equals(configured) || "mybatis".equals(configured)) {
            mode = configured;
        } else {
            // 其他取值（含已移除的 jdbc，以及 hibernate / spring-jdbc 等旧写法）统一按 MyBatis 处理
            mode = "mybatis";
        }
    }

    /** 当前存储模式：memory（内存库演示）/ mybatis（MyBatis Mapper + MySQL） */
    public static String mode() {
        init();
        return mode;
    }

    /** 是否使用内存库 */
    public static boolean isMemoryMode() {
        return "memory".equals(mode());
    }

    /** 是否使用 MyBatis（正式部署的默认持久化方式） */
    public static boolean isMyBatisMode() {
        return "mybatis".equals(mode());
    }

    /** 是否使用数据库持久化（MyBatis），事务需要真实数据库连接 */
    public static boolean isDatabaseMode() {
        return !isMemoryMode();
    }

    /**
     * 按接口名反射创建实现类。
     * 接口 UserDao → memory 模式 {@code MemoryUserDaoImpl}；mybatis 模式 {@code MyBatisUserDaoImpl}。
     */
    private static Object create(Class<?> type) {
        init();
        String simple = type.getSimpleName();
        String base = simple.endsWith("Dao") ? simple.substring(0, simple.length() - 3) : simple;
        String prefix = isMemoryMode()
                ? "com.campus.repair.dao.memory.Memory"
                : "com.campus.repair.dao.mybatis.MyBatis";
        String className = prefix + base + "DaoImpl";
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到 DAO 实现类：" + className, e);
        } catch (Exception e) {
            throw new IllegalStateException("DAO 实现类实例化失败：" + className, e);
        }
    }

    private static <T> T get(T cached, Class<T> type) {
        return cached;
    }

    public static UserDao userDao() {
        if (userDao == null) {
            userDao = (UserDao) create(UserDao.class);
        }
        return get(userDao, UserDao.class);
    }

    public static WorkerDao workerDao() {
        if (workerDao == null) {
            workerDao = (WorkerDao) create(WorkerDao.class);
        }
        return workerDao;
    }

    public static RepairOrderDao repairOrderDao() {
        if (repairOrderDao == null) {
            repairOrderDao = (RepairOrderDao) create(RepairOrderDao.class);
        }
        return repairOrderDao;
    }

    public static RepairTaskDao repairTaskDao() {
        if (repairTaskDao == null) {
            repairTaskDao = (RepairTaskDao) create(RepairTaskDao.class);
        }
        return repairTaskDao;
    }

    public static MaterialDao materialDao() {
        if (materialDao == null) {
            materialDao = (MaterialDao) create(MaterialDao.class);
        }
        return materialDao;
    }

    public static MaterialUsageDao materialUsageDao() {
        if (materialUsageDao == null) {
            materialUsageDao = (MaterialUsageDao) create(MaterialUsageDao.class);
        }
        return materialUsageDao;
    }

    public static EvaluationDao evaluationDao() {
        if (evaluationDao == null) {
            evaluationDao = (EvaluationDao) create(EvaluationDao.class);
        }
        return evaluationDao;
    }

    public static MessageDao messageDao() {
        if (messageDao == null) {
            messageDao = (MessageDao) create(MessageDao.class);
        }
        return messageDao;
    }

    public static ProgressDao progressDao() {
        if (progressDao == null) {
            progressDao = (ProgressDao) create(ProgressDao.class);
        }
        return progressDao;
    }

    public static BaseDataDao baseDataDao() {
        if (baseDataDao == null) {
            baseDataDao = (BaseDataDao) create(BaseDataDao.class);
        }
        return baseDataDao;
    }

    /** 重置（测试使用） */
    public static synchronized void reset() {
        userDao = null;
        workerDao = null;
        repairOrderDao = null;
        repairTaskDao = null;
        materialDao = null;
        materialUsageDao = null;
        evaluationDao = null;
        messageDao = null;
        progressDao = null;
        baseDataDao = null;
    }
}
