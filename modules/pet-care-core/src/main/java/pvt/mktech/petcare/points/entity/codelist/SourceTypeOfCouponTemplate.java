package pvt.mktech.petcare.points.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * {@code @description}: 积分代金券来源类型枚举
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum SourceTypeOfCouponTemplate {

    SYSTEM(1, "系统发放"),
    ACTIVITY(2, "活动发放"),
    NEWBIE(3, "新人礼包");

    @EnumValue
    private final Integer code;
    private final String description;
}
