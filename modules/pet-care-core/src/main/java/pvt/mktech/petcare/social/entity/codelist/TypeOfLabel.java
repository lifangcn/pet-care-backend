package pvt.mktech.petcare.social.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 标签类型枚举
 * {@code @date}: 2026/02/13
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum TypeOfLabel {

    GENERAL("GENERAL", "通用标签"),
    BREED("BREED", "宠物品种"),
    CONTENT("CONTENT", "内容类型");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<TypeOfLabel> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
