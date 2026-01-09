package pvt.mktech.petcare.ai.controller;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pvt.mktech.petcare.ai.advisor.MyLoggerAdvisor;
import pvt.mktech.petcare.ai.tool.QueryRewriter;
import pvt.mktech.petcare.common.constant.CommonConstant;
import pvt.mktech.petcare.ai.util.ConversationIdGenerator;
import pvt.mktech.petcare.common.usercache.UserContext;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

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
    private final MilvusVectorStore milvusVectorStore;
    private final ImageModel imageModel;
    private final SpeechSynthesisModel speechSynthesisModel;
    private final QueryRewriter queryRewriter;


    /**
     * AI 对话接口
     *
     * @param message 用户消息
     * @return 流式响应（Token by Token）
     */
    @GetMapping("/chat")
    public Flux<String> chat(@RequestParam("message") String message) {
        Long userId = 1L; // 测试用，模拟用户ID
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, userId))
                .user(message).stream().content();
    }

    /**
     * AI 文生图 接口
     *
     * @param message 用户消息
     * @return 流式响应（Token by Token）
     */
    @GetMapping(value = "/image")
    public String image(@RequestParam("message") String message) {
        return imageModel.call(
                new ImagePrompt(message, DashScopeImageOptions.builder().withModel("wan2.2-kf2v-flash").build())
        ).getResult().getOutput().getUrl();

    }

    /**
     * AI 文生音 接口
     *
     * @param message 用户消息
     * @return 流式响应（Token by Token）
     */
    @GetMapping(value = "/voice")
    public String voice(@RequestParam("message") String message) {
        // 调用模型
        DashScopeAudioSpeechOptions options = DashScopeAudioSpeechOptions.builder()
                .model("cosyvoice-v2")
                .voice("longhouge")
                .build();
        SpeechSynthesisResponse response = speechSynthesisModel.call(new SpeechSynthesisPrompt(message, options));
        // 字节流语音转文件
        String filePath = "/Users/michael/Downloads/" + UUID.randomUUID() + ".mp3";
        ByteBuffer audio = response.getResult().getOutput().getAudio();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(audio.array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filePath;
    }

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

        Long userId = UserContext.getUserInfo().getUserId();
        String conversationId = conversationIdGenerator.generate(userId, sessionId);
        String rewriteMessage = queryRewriter.doQueryRewrite(message);
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationId))
                .advisors(advisorSpec -> new QuestionAnswerAdvisor(milvusVectorStore))
                .user(rewriteMessage)
                .stream()
                .content();
    }

    /**
     * 今天吃什么
     *
     * @param message 用户消息
     */
    @GetMapping("/chat/dinner")
    public Flux<String> dinner(@RequestParam("message") String message) {
        String rewriteMessage = queryRewriter.doQueryRewrite(message);

        return chatClient.prompt()
                .system(loadDinnerPrompt())
                .user(rewriteMessage)
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, 2L))
//                .advisors(advisorSpec -> new QuestionAnswerAdvisor(milvusVectorStore))
                .advisors(new MyLoggerAdvisor())
                .stream().content();
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

    /**
     * 加载系统提示词
     * 系统提示词的作用：
     * 1. 定义 AI 的角色
     * 2. 定义 AI 的行为规范
     * 3. 定义 AI 的能力范围
     * 4. 定义输出格式要求
     */
    private String loadDinnerPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("dinner.md");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "你是宠物关怀提供的专业的服务咨询顾问";
        }
    }
}
