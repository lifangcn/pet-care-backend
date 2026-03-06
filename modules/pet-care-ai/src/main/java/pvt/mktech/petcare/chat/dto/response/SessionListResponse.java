package pvt.mktech.petcare.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * {@code @description}: 会话列表响应
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResponse {

    /**
     * 总数
     */
    private Long total;

    /**
     * 会话列表
     */
    private List<SessionItem> items;
}
