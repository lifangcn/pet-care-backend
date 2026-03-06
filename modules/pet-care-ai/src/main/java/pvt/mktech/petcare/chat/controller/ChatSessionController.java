package pvt.mktech.petcare.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.chat.dto.request.CreateSessionRequest;
import pvt.mktech.petcare.chat.dto.response.*;
import pvt.mktech.petcare.chat.service.ChatHistoryService;
import pvt.mktech.petcare.chat.service.ChatSessionService;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.web.UserContext;

import java.util.List;

/**
 * {@code @description}: 会话管理API控制器
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Slf4j
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final ChatHistoryService chatHistoryService;

    /**
     * 创建会话
     */
    @PostMapping("/session")
    public Result<SessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        Long userId = UserContext.getUserId();
        SessionResponse session = chatSessionService.createSession(userId, request);
        return Result.success(session);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<SessionListResponse> listSessions(
            @RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        Long userId = UserContext.getUserId();
        SessionListResponse response = chatSessionService.listSessions(userId, pageNumber, pageSize);
        return Result.success(response);
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<ChatMessageResponse>> getSessionMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "100") int limit) {
        Long userId = UserContext.getUserId();
        // TODO: 验证会话归属（只能查看自己的会话）
        List<ChatMessageResponse> messages = chatSessionService
                .getSessionMessages(sessionId, limit);
        return Result.success(messages);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        Long userId = UserContext.getUserId();
        // TODO: 验证会话归属（只能删除自己的会话）
        chatSessionService.deleteSession(sessionId);
        return Result.success();
    }


    /**
     * 清除历史记录
     */
    @DeleteMapping("/history")
    public Result<ClearHistoryResponse> clearHistory() {
        Long userId = UserContext.getUserId();
        ClearHistoryResponse response = chatHistoryService.clearHistory(userId);
        return Result.success(response);
    }
}
