package pvt.mktech.petcare.chat.dto.request;

import lombok.Data;

/**
 * {@code @description}: 创建会话请求
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Data
public class CreateSessionRequest {

    /**
     * 会话名称（可选，不传则使用默认名称"新对话"）
     */
    private String name;
}
