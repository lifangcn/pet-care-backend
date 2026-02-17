package pvt.mktech.petcare.social.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 动态类型枚举 PRODUCT-好物分享 SERVICE-服务推荐 LOCATION-地点推荐 DAILY-日常分享 ACTIVITY_CHECK-活动打卡 ACTIVITY_JOIN-活动报名
 * {@code @date}: 2026/02/13
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum TypeOfPost {

    PRODUCT("PRODUCT", "好物分享"),
    SERVICE("SERVICE", "服务推荐"),
    LOCATION("LOCATION", "地点推荐"),
    DAILY("DAILY", "日常分享"),
    ACTIVITY_CHECK("ACTIVITY_CHECK", "活动打卡"),
    ACTIVITY_JOIN("ACTIVITY_JOIN", "活动报名");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<TypeOfPost> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
