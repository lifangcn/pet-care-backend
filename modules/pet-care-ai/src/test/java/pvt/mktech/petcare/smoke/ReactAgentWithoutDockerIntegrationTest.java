package pvt.mktech.petcare.smoke;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.agent.config.AgentAutoConfiguration;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ReactAgentWithoutDockerIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DeterministicCollaborators.class);

    @Test
    void reactAgentExecutesADeterministicAnswerWithoutDockerIntegration() {
        contextRunner.run(context -> {
            ReactAgent reactAgent = context.getBean(ReactAgent.class);

            OverAllState state = reactAgent.invoke(Map.of("messages", List.of(new UserMessage("smoke question"))))
                    .orElseThrow();
            List<Message> messages = state.<List<Message>>value("messages").orElseThrow();

            assertThat(messages)
                    .anySatisfy(message -> assertThat(message)
                            .isInstanceOfSatisfying(AssistantMessage.class,
                                    assistant -> assertThat(assistant.getText()).isEqualTo("deterministic smoke answer")));
            assertThat(context.getBean(AtomicInteger.class).get()).isPositive();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class DeterministicCollaborators {

        @Bean
        AtomicInteger chatModelInvocationCount() {
            return new AtomicInteger();
        }

        @Bean(name = "deepSeekChatModel")
        ChatModel deepSeekChatModel(AtomicInteger chatModelInvocationCount) {
            return new ChatModel() {
                @Override
                public ChatResponse call(Prompt prompt) {
                    chatModelInvocationCount.incrementAndGet();
                    return response();
                }

                @Override
                public Flux<ChatResponse> stream(Prompt prompt) {
                    chatModelInvocationCount.incrementAndGet();
                    return Flux.just(response());
                }

                private ChatResponse response() {
                    return new ChatResponse(List.of(new Generation(new AssistantMessage("deterministic smoke answer"))));
                }
            };
        }

        @Bean
        ReactAgent reactAgent(ChatModel deepSeekChatModel) throws Exception {
            return new AgentAutoConfiguration().reactAgent(deepSeekChatModel, new pvt.mktech.petcare.agent.config.AgentProperties(), List.of());
        }
    }
}
