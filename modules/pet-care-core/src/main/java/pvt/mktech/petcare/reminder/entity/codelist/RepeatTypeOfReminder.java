package pvt.mktech.petcare.reminder.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 重复类型 枚举类: NONE(不重复), DAILY(每天), WEEKLY(每周), MONTHLY(每月), CUSTOM(自定义)
 * {@code @date}: 2026/02/13
 *
 * @author Michael Li
 */
@Getter
@AllArgsConstructor
public enum RepeatTypeOfReminder {

    NONE("NONE", "不重复"),
    DAILY("DAILY", "每天"),
    WEEKLY("WEEKLY", "每周"),
    MONTHLY("MONTHLY", "每月"),
    CUSTOM("CUSTOM", "自定义");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<RepeatTypeOfReminder> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
