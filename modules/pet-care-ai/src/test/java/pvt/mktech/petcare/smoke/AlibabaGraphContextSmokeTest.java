package pvt.mktech.petcare.smoke;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.agent.config.AgentAutoConfiguration;
import pvt.mktech.petcare.agent.config.AgentProperties;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlibabaGraphContextSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ReactAgentConfiguration.class);

    @Test
    void currentAgentFactoryMethodCreatesAReactAgentWithoutCallingAModel() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ReactAgent.class);
            assertThat(context.getBean(ReactAgent.class).name()).isEqualTo("PetCare ReAct Agent");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ReactAgentConfiguration {

        @Bean
        ReactAgent reactAgent() throws Exception {
            return new AgentAutoConfiguration().reactAgent(new ChatModel() {
                @Override
                public ChatResponse call(Prompt prompt) {
                    throw new AssertionError("Graph context smoke test must not invoke a chat model");
                }

                @Override
                public Flux<ChatResponse> stream(Prompt prompt) {
                    throw new AssertionError("Graph context smoke test must not invoke a chat model");
                }
            }, new AgentProperties(), List.of());
        }
    }
}
