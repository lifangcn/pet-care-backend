package pvt.mktech.petcare.agent.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;

import java.io.IOException;

/**
 * {@code @description}: Agent 执行记录存储（Elasticsearch）
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AgentExecutionRepository {

    public static final String AGENT_EXECUTION_INDEX = "agent_execution";

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    /**
     * 保存执行记录
     */
    public void save(AgentExecutionRecord record) {
        try {
            IndexRequest<Object> request = IndexRequest.of(i -> i
                    .index(AGENT_EXECUTION_INDEX)
                    .id(record.getExecutionId())
                    .document(record)
            );

            elasticsearchClient.index(request);
            log.debug("Agent 执行记录已保存: executionId={}, steps={}",
                    record.getExecutionId(), record.getTotalSteps());
        } catch (IOException e) {
            log.error("保存 Agent 执行记录失败: executionId={}",
                    record.getExecutionId(), e);
        }
    }
}
