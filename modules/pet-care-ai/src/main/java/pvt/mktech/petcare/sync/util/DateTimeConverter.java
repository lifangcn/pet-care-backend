package pvt.mktech.petcare.sync.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * {@code @description}: 时间转换工具类
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public class DateTimeConverter {

    private static final DateTimeFormatter CANAL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * LocalDateTime 转换为 Instant（使用系统默认时区）
     */
    public static Instant toInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneOffset.systemDefault()).toInstant();
    }

    /**
     * LocalDateTime 转换为 Instant（指定时区）
     */
    public static Instant toInstant(LocalDateTime dateTime, ZoneOffset zoneOffset) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(zoneOffset).toInstant();
    }

    /**
     * LocalDateTime 转换为 Instant（使用UTC时区）
     */
    public static Instant toInstantUtc(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneOffset.UTC).toInstant();
    }

    /**
     * 解析 Canal 日期字符串（格式：yyyy-MM-dd HH:mm:ss）
     */
    public static Instant parseCanalDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, CANAL_DATE_FORMATTER);
        return toInstant(dateTime);
    }
}
