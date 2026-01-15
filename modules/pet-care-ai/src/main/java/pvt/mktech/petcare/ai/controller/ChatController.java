package pvt.mktech.petcare.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pvt.mktech.petcare.ai.tool.QueryRewriter;
import pvt.mktech.petcare.ai.util.ConversationIdGenerator;
import pvt.mktech.petcare.common.constant.CommonConstant;
import pvt.mktech.petcare.common.usercache.UserContext;
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
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;
    private final ConversationIdGenerator conversationIdGenerator;
//    private final MilvusVectorStore milvusVectorStore;
    private final QueryRewriter queryRewriter;

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
//            String rewriteMessage = queryRewriter.doQueryRewrite(message); // 重写不一定是好事

        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationId))
//                    .advisors(advisorSpec -> new QuestionAnswerAdvisor(milvusVectorStore))
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
