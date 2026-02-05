package pvt.mktech.petcare.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pvt.mktech.petcare.chat.rag.advisor.MyLoggerAdvisor;
import pvt.mktech.petcare.common.constant.CommonConstant;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.shared.ConversationIdGenerator;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * {@code @description}: AI 对话控制器:提供 HTTP 接口，支持流式响应
 * {@code @date}: 2025/12/30 15:19
 *
 * @author Michael
 */
@RestController
@Slf4j
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;
    private final ConversationIdGenerator conversationIdGenerator;
    private final VectorStore elasticsearchVectorStore;

    /**
     * RAG对话接口（基于向量数据库检索）
     *
     * @param message   用户消息
     * @param sessionId 会话标识（可选）
     * @return 流式响应（Token by Token）
     */
    @GetMapping("/chat/rag")
    public Flux<String> ragChat(
            @RequestParam("message") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        Long userId = UserContext.getUserId();
        String conversationId = conversationIdGenerator.generate(userId, sessionId);

        return chatClient.prompt()
                .advisors(new MyLoggerAdvisor())
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationId))
                .advisors(new QuestionAnswerAdvisor(elasticsearchVectorStore))
                .user("[用户ID:" + userId + "]\n[当前时间:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]\n" + message)
                .stream()
                .content();
    }

    /**
     * 从请求Header获取用户ID
     */
    private Long getUserIdFromRequest(ServerHttpRequest request) {
        String userIdHeader = request.getHeaders().getFirst(CommonConstant.HEADER_USER_ID);
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
