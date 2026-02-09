package pvt.mktech.petcare.points.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * {@code @description}: 积分代金券状态枚举
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum StatusOfPointsCoupon {

    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    @EnumValue
    private final Integer code;
    private final String description;
}
