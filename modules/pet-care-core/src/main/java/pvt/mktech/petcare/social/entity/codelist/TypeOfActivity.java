package pvt.mktech.petcare.social.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 活动类型枚举
 * {@code @date}: 2026/02/13
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum TypeOfActivity {

    ONLINE("ONLINE", "线上活动"),
    OFFLINE("OFFLINE", "线下聚会");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<TypeOfActivity> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
