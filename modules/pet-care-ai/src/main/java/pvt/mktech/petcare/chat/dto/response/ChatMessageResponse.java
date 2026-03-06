package pvt.mktech.petcare.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * {@code @description}: 聊天消息响应
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 角色：user / assistant（前端期望小写）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 时间戳
     */
    private Instant timestamp;
}
