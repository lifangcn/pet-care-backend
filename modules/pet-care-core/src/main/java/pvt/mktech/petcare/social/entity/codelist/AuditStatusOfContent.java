package pvt.mktech.petcare.social.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 内容审核状态枚举
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Getter
@AllArgsConstructor
public enum AuditStatusOfContent {

    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<AuditStatusOfContent> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst();
    }
}
