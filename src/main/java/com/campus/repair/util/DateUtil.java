package com.campus.repair.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类（线程安全，使用局部 SimpleDateFormat 实例）。
 */
public final class DateUtil {

    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_DATE = "yyyy-MM-dd";
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "yyyy/MM/dd"
    };

    private DateUtil() {
    }

    public static String format(Date date) {
        return date == null ? "" : new SimpleDateFormat(PATTERN_DATETIME).format(date);
    }

    public static String format(Date date, String pattern) {
        return date == null ? "" : new SimpleDateFormat(pattern).format(date);
    }

    public static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat(PATTERN_DATE).format(date);
    }

    /** 解析日期，支持多种常见格式；解析失败返回 null */
    public static Date parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String value = text.trim();
        for (String pattern : DATE_PATTERNS) {
            try {
                Date date = new SimpleDateFormat(pattern).parse(value);
                if (date != null) {
                    return date;
                }
            } catch (ParseException ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }

    /** 解析时间区间的结束时间（"2024-04-08" → 当天 23:59:59） */
    public static Date parseEndOfDay(String text) {
        Date date = parse(text);
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /** 当前时间 */
    public static Date now() {
        return new Date();
    }

    /** 相对当前时间偏移分钟 */
    public static Date minutesAgo(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -minutes);
        return calendar.getTime();
    }

    /** 相对当前时间偏移小时 */
    public static Date hoursAgo(int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -hours);
        return calendar.getTime();
    }

    /** 两个时间相差小时数 */
    public static double hoursBetween(Date begin, Date end) {
        if (begin == null || end == null) {
            return 0d;
        }
        return (end.getTime() - begin.getTime()) / 3600000d;
    }

    /** 距今多少小时（用于接单超时、确认超时判断） */
    public static double hoursSince(Date time) {
        if (time == null) {
            return 0d;
        }
        return (System.currentTimeMillis() - time.getTime()) / 3600000d;
    }

    /** 月份标签（用于统计按月分组） */
    public static String monthLabel(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM").format(date);
    }
}
