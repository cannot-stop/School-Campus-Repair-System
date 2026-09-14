package com.campus.repair.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码工具类。
 *
 * <p>对应设计书 1.1.2.1"系统对密码进行加密后保存用户信息"与表 2.10"密码（加盐加密存储）"。
 * 采用 SHA-256(固定盐 + 明文) 的方式存储，注册、登录、修改密码均经过本类处理；
 * 数据初始化脚本 db/seed.sql 中的演示账号密码即由此算法生成。</p>
 */
public final class PasswordUtil {

    /** 盐值（生产环境应从配置中心读取并定期轮换） */
    public static final String SALT = "campus-repair-2024";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordUtil() {
    }

    /** 加密（加盐 SHA-256，返回 64 位十六进制串） */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT.getBytes(UTF8));
            byte[] bytes = digest.digest(rawPassword.getBytes(UTF8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(HEX[(b >> 4) & 0x0F]).append(HEX[b & 0x0F]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    /** 校验明文密码与密文是否匹配 */
    public static boolean matches(String rawPassword, String encrypted) {
        if (rawPassword == null || encrypted == null) {
            return false;
        }
        return encrypt(rawPassword).equalsIgnoreCase(encrypted);
    }

    /** 密码强度校验：长度不少于 6 位，且不能为纯数字 */
    public static boolean isWeak(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            return true;
        }
        return rawPassword.matches("^\\d+$");
    }

    /** 密码强度提示 */
    public static String strengthTip() {
        return "密码长度不能少于 6 位，且不能为纯数字";
    }
}
