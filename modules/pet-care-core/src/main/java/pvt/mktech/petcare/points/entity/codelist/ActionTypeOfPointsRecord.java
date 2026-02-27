package pvt.mktech.petcare.points.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 积分行为类型枚举
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum ActionTypeOfPointsRecord {

    REGISTER("REGISTER", "注册赠送", 0),
    CHECK_IN("CHECK_IN", "签到", 10),
    PUBLISH("PUBLISH", "发布内容", 30),
    COMMENT("COMMENT", "发布评论", 5),
    LIKE("LIKE", "点赞他人", 2),
    LIKED("LIKED", "被点赞", 1),
    COMMENTED("COMMENTED", "被评论", 3),
    AI_CONSULT("AI_CONSULT", "AI健康咨询", -10),
    COUPON_REDEEM("COUPON_REDEEM", "积分代金券兑换", 0);

    @EnumValue
    private final String code;
    private final String description;
    /**
     * 积分变动值（正为获取，负为消耗）
     */
    private final Integer points;

    /**
     * 判断是否为积分获取行为
     */
    public boolean isEarn() {
        return points > 0;
    }

    /**
     * 判断是否为积分消耗行为
     */
    public boolean isConsume() {
        return points < 0;
    }

    public static Optional<ActionTypeOfPointsRecord> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
