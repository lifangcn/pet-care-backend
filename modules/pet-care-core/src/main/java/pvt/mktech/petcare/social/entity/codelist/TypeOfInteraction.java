package pvt.mktech.petcare.social.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 互动类型枚举
 * {@code @date}: 2026/02/13
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum TypeOfInteraction {

    LIKE("LIKE", "点赞"),
    RATING("RATING", "评分");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<TypeOfInteraction> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
