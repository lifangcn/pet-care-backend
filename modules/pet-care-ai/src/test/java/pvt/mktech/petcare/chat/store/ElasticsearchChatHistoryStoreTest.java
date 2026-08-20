package pvt.mktech.petcare.chat.store;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.Result;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.test.util.ReflectionTestUtils;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchChatHistoryStoreTest {
    @Test
    void firstCreateRefreshesSessionUsingCreatedMessageTimestamps() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchChatHistoryStore store = store(client);
        ChatMessageDocument message = message(1L);
        Instant existingUpdated = message.getCreatedAt().minus(1, ChronoUnit.HOURS);
        Instant existingExpiry = message.getExpiresAt().minus(1, ChronoUnit.HOURS);
        when(client.bulk(any(BulkRequest.class))).thenReturn(bulk(item(201, Result.Created.jsonValue(), null)));
        when(client.get(any(Function.class), eq(ElasticsearchChatHistoryStore.SessionDocument.class))).thenReturn(session(existingUpdated, existingExpiry));
        CountResponse countResponse = mock(CountResponse.class);
        when(countResponse.count()).thenReturn(1L);
        when(client.count(any(Function.class))).thenReturn(countResponse);

        store.saveMessages(List.of(message));

        ArgumentCaptor<Function> requests = ArgumentCaptor.forClass(Function.class);
        verify(client, times(2)).index(requests.capture());
        co.elastic.clients.util.ObjectBuilder<?> builder = (co.elastic.clients.util.ObjectBuilder<?>) requests.getAllValues().getLast().apply(new IndexRequest.Builder<>());
        ElasticsearchChatHistoryStore.SessionDocument refreshed = (ElasticsearchChatHistoryStore.SessionDocument) ((IndexRequest<?>) builder.build()).document();
        assertThat(refreshed.updatedAt()).isEqualTo(message.getCreatedAt());
        assertThat(refreshed.expiresAt()).isEqualTo(message.getExpiresAt());
        assertThat(refreshed.messageCount()).isEqualTo(1);
        assertThat(refreshed.name()).isEqualTo("已有标题");
    }

    @Test
    void duplicateConflictIsIdempotentAndDoesNotRefreshSession() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchChatHistoryStore store = store(client);
        when(client.bulk(any(BulkRequest.class))).thenReturn(bulk(item(409, null, ErrorCause.of(error -> error.type("version_conflict_engine_exception")))));

        store.saveMessages(List.of(message(2L)));

        verify(client).bulk(any(BulkRequest.class));
        verify(client).index(any(Function.class));
        verify(client, never()).count(any(Function.class));
    }

    @Test
    void nonConflictBulkItemFailureFailsSave() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchChatHistoryStore store = store(client);
        when(client.bulk(any(BulkRequest.class))).thenReturn(bulk(item(500, null, ErrorCause.of(error -> error.type("internal_server_error")))));

        assertThatThrownBy(() -> store.saveMessages(List.of(message(3L))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("保存消息失败");
    }

    @Test
    void cleanupRunsBoundedBatchesAndIsEnabledByDefault() {
        ChatHistoryRepository repository = mock(ChatHistoryRepository.class);
        ChatMemoryProperties properties = new ChatMemoryProperties();
        properties.getHistory().setCleanupBatchSize(2);
        properties.getHistory().setCleanupMaxBatches(3);
        when(repository.deleteExpiredBatch(2)).thenReturn(2, 2, 1);

        new ChatHistoryCleanupScheduler(repository, properties).cleanup();

        verify(repository, times(3)).deleteExpiredBatch(2);
        ConditionalOnProperty condition = ChatHistoryCleanupScheduler.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    private ElasticsearchChatHistoryStore store(ElasticsearchClient client) {
        ElasticsearchChatHistoryStore store = new ElasticsearchChatHistoryStore(client);
        ReflectionTestUtils.setField(store, "index", "chat_history");
        ReflectionTestUtils.setField(store, "retentionDays", 30L);
        return store;
    }

    private static BulkResponse bulk(BulkResponseItem item) {
        return BulkResponse.of(builder -> builder.errors(item.error() != null).items(item).took(1));
    }

    private static BulkResponseItem item(int status, String result, ErrorCause error) {
        return BulkResponseItem.of(builder -> {
            builder.operationType(co.elastic.clients.elasticsearch.core.bulk.OperationType.Create)
                    .index("chat_history").id("message:1").status(status);
            if (result != null) builder.result(result);
            if (error != null) builder.error(error);
            return builder;
        });
    }

    private static GetResponse<ElasticsearchChatHistoryStore.SessionDocument> session(Instant updatedAt, Instant expiresAt) {
        return GetResponse.of(builder -> builder.index("chat_history").id("session:1:s1").found(true)
                .source(new ElasticsearchChatHistoryStore.SessionDocument(1L, "s1", "已有标题", updatedAt.minus(1, ChronoUnit.HOURS),
                        updatedAt, expiresAt, 0L, "session")));
    }

    private static ChatMessageDocument message(Long id) {
        Instant createdAt = Instant.parse("2026-01-01T12:00:00Z");
        ChatMessageDocument message = new ChatMessageDocument();
        message.setId(id); message.setUserId(1L); message.setSessionId("s1"); message.setConversationId("c1");
        message.setRole("USER"); message.setContent("hello"); message.setCreatedAt(createdAt); message.setExpiresAt(createdAt.plus(30, ChronoUnit.DAYS));
        return message;
    }
}
