package pvt.mktech.petcare.core.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * {@code @description}: 健康记录类型枚举
 * {@code @date}: 2025/12/19 11:05
 *
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum StatusOfReminderExecution {

    PENDING("PENDING", "待处理"),
    COMPLETED("COMPLETED", "已完成"),
    OVERDUE("OVERDUE", "已过期");

    private final String code;
    private final String description;

    @EnumValue
    public String getCode() {
        return code;
    }
}
