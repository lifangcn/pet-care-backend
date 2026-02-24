package pvt.mktech.petcare.knowledge.entity.codelist;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code @description}: 文档处理状态枚举
 * {@code @date}: 2026-02-24
 * @author Michael
 */
@Getter
@AllArgsConstructor
public enum ProcessingStatusOfKnowledgeDocument {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "处理完成"),
    FAILED("FAILED", "处理失败");

    @EnumValue
    private final String code;
    private final String description;

    public static Optional<ProcessingStatusOfKnowledgeDocument> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
}
