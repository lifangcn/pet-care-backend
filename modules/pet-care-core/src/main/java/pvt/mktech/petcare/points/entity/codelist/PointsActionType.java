package pvt.mktech.petcare.points.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * {@code @description}: 积分行为类型枚举
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum PointsActionType {

    REGISTER(0, "注册赠送", 1000),
    CHECK_IN(1, "签到", 10),
    PUBLISH(2, "发布内容", 30),
    COMMENT(3, "发布评论", 5),
    LIKE(4, "点赞他人", 2),
    LIKED(5, "被点赞", 1),
    COMMENTED(6, "被评论", 3),
    AI_CONSULT(7, "AI健康咨询", -10);

    @EnumValue
    private final Integer code;
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
}
