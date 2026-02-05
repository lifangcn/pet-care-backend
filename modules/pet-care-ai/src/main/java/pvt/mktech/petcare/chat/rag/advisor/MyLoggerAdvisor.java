package pvt.mktech.petcare.chat.rag.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * {@code @description}:
 * {@code @date}: 2026/1/7 11:35
 *
 * @author Michael
 */
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        log.info("AI Request: {}", chatClientRequest.prompt());
        chatClientRequest = chatClientRequest;
        ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
        log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        log.info("AI Request: {}", chatClientRequest.prompt());
        chatClientRequest = chatClientRequest;
        Flux<ChatClientResponse> chatClientResponseFlux = chain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponseFlux,
                chatClientResponse -> log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText()));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
