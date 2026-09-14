package com.campus.repair.test;

import com.campus.repair.config.AppConfig;
import com.campus.repair.dao.DaoFactory;

/** 配置与存储模式探针：验证命令行 -D 参数覆盖配置文件的优先级。 */
public class ConfigProbe {

    public static void main(String[] args) {
        System.out.println("system property storage.mode = " + System.getProperty("storage.mode"));
        System.out.println("system property db.password    = " + System.getProperty("db.password"));
        AppConfig.load();
        System.out.println("AppConfig storage.mode         = " + AppConfig.get("storage.mode", "default"));
        System.out.println("AppConfig db.password          = "
                + (AppConfig.get("db.password", "").isEmpty() ? "(空)" : "已设置"));
        DaoFactory.init();
        System.out.println("DaoFactory mode                = " + DaoFactory.mode());
        System.out.println("isJdbcMode                     = " + DaoFactory.isJdbcMode());
    }
}
