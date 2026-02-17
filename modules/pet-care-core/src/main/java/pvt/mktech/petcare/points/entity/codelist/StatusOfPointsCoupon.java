package pvt.mktech.petcare.points.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 积分代金券状态枚举
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum StatusOfPointsCoupon {

    UNUSED("UNUSED", "未使用"),
    USED("USED", "已使用"),
    EXPIRED("EXPIRED", "已过期");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<StatusOfPointsCoupon> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
