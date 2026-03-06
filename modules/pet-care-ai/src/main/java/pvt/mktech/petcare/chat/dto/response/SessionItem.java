package pvt.mktech.petcare.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * {@code @description}: 会话列表项
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionItem {

    /**
     * 会话ID
     */
    private String id;

    /**
     * 会话名称
     */
    private String name;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 消息数量
     */
    private Long messageCount;
}
