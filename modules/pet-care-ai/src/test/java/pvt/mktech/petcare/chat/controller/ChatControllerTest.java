package pvt.mktech.petcare.chat.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.reactive.function.client.WebClient;
import pvt.mktech.petcare.agent.orchestrator.AgentOrchestrator;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.chat.sink.ChatMessageSink;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.shared.ConversationIdGenerator;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {
    @Mock private ChatClient chatClient;
    @Mock private ConversationIdGenerator conversationIdGenerator;
    @Mock private VectorStore vectorStore;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private ChatMessageSink chatMessageSink;
    @Mock private AgentOrchestrator agentOrchestrator;
    @Mock private ChatHistoryRepository history;
    private ChatController controller;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
        controller = new ChatController(chatClient, conversationIdGenerator, vectorStore, webClientBuilder, chatMessageSink, agentOrchestrator, history);
    }

    @AfterEach
    void tearDown() { UserContext.removeUserId(); }

    @Test
    void explicitSessionIsVerifiedBeforeRagLlmInvocation() {
        doThrow(new IllegalArgumentException("会话不存在或无权访问")).when(history).normalizeSessionId(1L, "foreign");

        assertThrows(IllegalArgumentException.class, () -> controller.ragChat("hello", "foreign"));

        verifyNoInteractions(chatClient, conversationIdGenerator);
    }

    @Test
    void explicitSessionIsVerifiedBeforeAgentSelection() {
        doThrow(new IllegalArgumentException("会话不存在或无权访问")).when(history).normalizeSessionId(1L, "foreign");

        assertThrows(IllegalArgumentException.class, () -> controller.agentChat("hello", "foreign"));

        verifyNoInteractions(agentOrchestrator, conversationIdGenerator);
    }
}
