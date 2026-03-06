package pvt.mktech.petcare.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.chat.dto.response.ClearHistoryResponse;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;

/**
 * {@code @description}: 聊天历史管理服务
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 清除用户所有历史记录
     *
     * @param userId 用户ID
     * @return 清除结果
     */
    @Transactional
    public ClearHistoryResponse clearHistory(Long userId) {
        long deletedCount = chatHistoryRepository.deleteByUserId(userId);
        log.info("清除用户历史记录成功: userId={}, deletedCount={}", userId, deletedCount);
        return new ClearHistoryResponse(true, deletedCount);
    }
}
