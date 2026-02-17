package pvt.mktech.petcare.reminder.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 提醒来源类型 枚举类: MANUAL(手动添加), SYSTEM(系统生成)
 * {@code @date}: 2026/02/13
 *
 * @author Michael Li
 */
@Getter
@AllArgsConstructor
public enum SourceTypeOfReminder {

    MANUAL("MANUAL", "手动添加"),
    SYSTEM("SYSTEM", "系统生成");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<SourceTypeOfReminder> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
