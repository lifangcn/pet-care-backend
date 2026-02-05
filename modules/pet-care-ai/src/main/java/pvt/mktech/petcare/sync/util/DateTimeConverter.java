package pvt.mktech.petcare.sync.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * {@code @description}: 时间转换工具类
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public class DateTimeConverter {

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
}
