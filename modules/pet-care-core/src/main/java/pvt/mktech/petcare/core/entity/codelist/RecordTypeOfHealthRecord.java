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
public enum RecordTypeOfHealthRecord {
    WEIGHT("weight", "体重"),
    TEMPERATURE("temperature", "体温"),
    MEDICAL("medical", "用药");

    @EnumValue
    private final String code;
    private final String description;
}
