package pvt.mktech.petcare.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code @description}: 清除历史记录响应
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClearHistoryResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 删除数量
     */
    private long deletedCount;
}
