package pvt.mktech.petcare.health.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 健康记录类型枚举
 * {@code @date}: 2025/12/19 11:05
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum RecordTypeOfHealthRecord {

    WEIGHT("WEIGHT", "体重"),
    TEMPERATURE("TEMPERATURE", "体温"),
    MEDICAL("MEDICAL", "用药");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<RecordTypeOfHealthRecord> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
