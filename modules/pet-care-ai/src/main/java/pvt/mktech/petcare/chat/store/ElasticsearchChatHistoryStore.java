package pvt.mktech.petcare.chat.store;

import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.dto.response.SessionItem;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** ES implementation keeps session documents first-class; messages and sessions share one index. */
@Component
@ConditionalOnProperty(prefix = "spring.ai.chat.memory.history", name = "store", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchChatHistoryStore implements ChatHistoryStore {
    private final ElasticsearchClient client;
    @Value("${spring.ai.chat.memory.history.index-name:chat_history}") private String index;
    @Value("${spring.ai.chat.memory.history.retention-days:90}") private long retentionDays;

    public ElasticsearchChatHistoryStore(ElasticsearchClient client) { this.client = client; }

    public void ensureSession(Long userId,String id,String name) {
        try {
            Instant now = Instant.now();
            client.index(i -> i.index(index).id(sessionDocumentId(userId, id)).opType(OpType.Create)
                    .document(new SessionDocument(userId, id, name, now, now, expiresAt(now), 0L, "session")));
        } catch (Exception e) {
            if (!isConflict(e)) throw new IllegalStateException("保存会话失败", e);
        }
    }

    public Optional<SessionItem> getSession(Long userId,String id) {
        try {
            var r = client.get(g -> g.index(index).id(sessionDocumentId(userId, id)), SessionDocument.class);
            return r.found() && !expired(r.source().expiresAt()) ? Optional.of(item(r.source())) : Optional.empty();
        } catch(Exception e) { throw new IllegalStateException("读取会话失败",e); }
    }

    public List<SessionItem> listSessions(Long userId,int offset,int limit) { try { return client.search(s->s.index(index).from(offset).size(limit).query(q->q.bool(b->b.filter(f->f.term(t->t.field("document_type").value("session"))).filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.range(r->r.date(d->d.field("expires_at").gt(Instant.now().toString())))))).sort(x->x.field(f->f.field("updated_at").order(SortOrder.Desc))),SessionDocument.class).hits().hits().stream().map(h->item(h.source())).toList(); } catch(Exception e) { throw new IllegalStateException("读取会话列表失败",e); } }
    public long countSessions(Long userId) { try { return client.count(c->c.index(index).query(q->q.bool(b->b.filter(f->f.term(t->t.field("document_type").value("session"))).filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.range(r->r.date(d->d.field("expires_at").gt(Instant.now().toString()))))))).count(); } catch(Exception e) { throw new IllegalStateException("统计会话失败",e); } }

    public void saveMessages(List<ChatMessageDocument> messages) {
        if (messages == null || messages.isEmpty()) return;
        messages.forEach(message -> ensureSession(message.getUserId(), message.getSessionId(), "新对话"));
        try {
            var request = new BulkRequest.Builder();
            for (var message : messages) {
                message.setDocumentType("message");
                request.operations(o -> o.create(i -> i.index(index).id("message:" + message.getId()).document(message)));
            }
            var response = client.bulk(request.build());
            Map<SessionKey, SessionStats> createdSessions = new LinkedHashMap<>();
            boolean itemFailure = response.items().size() != messages.size();
            for (int position = 0; position < response.items().size(); position++) {
                var item = response.items().get(position);
                if (isItemFailure(item)) itemFailure = true;
                if (isCreated(item)) {
                    var message = messages.get(position);
                    createdSessions.merge(new SessionKey(message.getUserId(), message.getSessionId()),
                            new SessionStats(message.getCreatedAt(), message.getExpiresAt()), SessionStats::merge);
                }
            }
            for (var entry : createdSessions.entrySet()) refreshSessionStats(entry.getKey().userId(), entry.getKey().sessionId(), entry.getValue());
            if (itemFailure) throw new IllegalStateException("ES bulk item failure");
        } catch (Exception e) { throw new IllegalStateException("保存消息失败", e); }
    }

    public void updateEmbeddings(List<ChatMessageDocument> messages) {
        if (messages == null || messages.isEmpty()) return;
        try {
            var request = new BulkRequest.Builder();
            for (var message : messages) {
                message.setDocumentType("message");
                request.operations(o -> o.index(i -> i.index(index).id("message:" + message.getId()).document(message)));
            }
            var response = client.bulk(request.build());
            if (response.items().size() != messages.size() || response.items().stream().anyMatch(this::isItemFailure)) {
                throw new IllegalStateException("ES bulk item failure");
            }
        } catch (Exception e) { throw new IllegalStateException("更新消息向量失败", e); }
    }
    public List<ChatMessageDocument> getSessionHistory(Long userId,String id,int limit) { try { return client.search(s->s.index(index).size(limit).query(q->q.bool(b->b.filter(f->f.term(t->t.field("document_type").value("message"))).filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.term(t->t.field("session_id").value(id))).filter(f->f.range(r->r.date(d->d.field("expires_at").gt(Instant.now().toString())))))).sort(x->x.field(f->f.field("created_at").order(SortOrder.Asc))),ChatMessageDocument.class).hits().hits().stream().map(h->h.source()).toList(); } catch(Exception e) { throw new IllegalStateException("读取消息失败",e); } }
    public List<ChatMessageDocument> semanticSearch(Long userId,List<Float> vector,int k,double score,int days) { try { Instant now=Instant.now(); return client.search(s->s.index(index).size(k).minScore(score).query(q->q.bool(b->b.must(x->x.knn(n->n.field("embedding").queryVector(vector).k(k).numCandidates(k*2))).filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.term(t->t.field("role").value("USER"))).filter(f->f.term(t->t.field("document_type").value("message"))).filter(f->f.range(r->r.date(d->d.field("expires_at").gt(now.toString())))).filter(f->f.range(r->r.date(d->d.field("created_at").gte(now.minus(days, ChronoUnit.DAYS).toString())))))),ChatMessageDocument.class).hits().hits().stream().map(h->h.source()).toList(); } catch(Exception e) { throw new IllegalStateException("语义查询失败",e); } }
    public long deleteSession(Long userId,String id) { try { long deleted=client.deleteByQuery(d->d.index(index).query(q->q.bool(b->b.filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.term(t->t.field("session_id").value(id))).filter(f->f.term(t->t.field("document_type").value("message"))))).refresh(true)).deleted(); client.delete(d->d.index(index).id(sessionDocumentId(userId,id)).refresh(Refresh.True)); return deleted; } catch(Exception e) { throw new IllegalStateException("删除会话失败",e); } }
    public long deleteByUserId(Long userId) { try { long deleted=client.deleteByQuery(d->d.index(index).query(q->q.bool(b->b.filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.term(t->t.field("document_type").value("message"))))).refresh(true)).deleted(); client.deleteByQuery(d->d.index(index).query(q->q.bool(b->b.filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.term(t->t.field("document_type").value("session"))))).refresh(true)); return deleted; } catch(Exception e) { throw new IllegalStateException("删除历史失败",e); } }
    public long countMessages(Long userId,String id) { try { return client.count(c->c.index(index).query(q->q.bool(b->b.filter(f->f.term(t->t.field("user_id").value(userId))).filter(f->f.term(t->t.field("session_id").value(id))).filter(f->f.term(t->t.field("document_type").value("message"))).filter(f->f.range(r->r.date(d->d.field("expires_at").gt(Instant.now().toString()))))))).count(); } catch(Exception e) { throw new IllegalStateException("统计消息失败",e); } }
    public boolean updateDefaultSessionName(Long userId,String id,String name) { try { var response=client.get(g->g.index(index).id(sessionDocumentId(userId,id)),SessionDocument.class); if (!response.found() || expired(response.source().expiresAt()) || !"新对话".equals(response.source().name())) return false; var existing=response.source(); client.index(i->i.index(index).id(sessionDocumentId(userId,id)).document(new SessionDocument(existing.userId(),existing.sessionId(),name,existing.createdAt(),existing.updatedAt(),existing.expiresAt(),existing.messageCount(),existing.documentType()))); return true; } catch(Exception e) { throw new IllegalStateException("更新会话名称失败",e); } }
    public int deleteExpiredBatch(int size) { try { return Math.toIntExact(client.deleteByQuery(d->d.index(index).maxDocs((long) size).query(q->q.range(r->r.date(v->v.field("expires_at").lt(Instant.now().toString())))).refresh(true)).deleted()); } catch(Exception e) { throw new IllegalStateException("清理过期消息失败",e); } }

    private void refreshSessionStats(Long userId, String sessionId, SessionStats createdStats) {
        try {
            var session = client.get(g -> g.index(index).id(sessionDocumentId(userId, sessionId)), SessionDocument.class);
            if (!session.found()) return;
            var existing = session.source();
            long count = countMessages(userId, sessionId);
            Instant updatedAt = greatest(existing.updatedAt(), createdStats.maxCreatedAt());
            Instant expiresAt = greatest(existing.expiresAt(), createdStats.maxExpiresAt());
            client.index(i -> i.index(index).id(sessionDocumentId(userId, sessionId)).document(new SessionDocument(existing.userId(), existing.sessionId(), existing.name(), existing.createdAt(), updatedAt, expiresAt, count, existing.documentType())));
        } catch (Exception e) {
            throw new IllegalStateException("更新会话统计失败", e);
        }
    }

    private Instant expiresAt(Instant now) { return now.plus(retentionDays, ChronoUnit.DAYS); }
    private boolean expired(Instant expiresAt) { return expiresAt != null && !expiresAt.isAfter(Instant.now()); }
    private String sessionDocumentId(Long userId, String sessionId) { return "session:" + userId + ":" + sessionId; }
    private boolean isCreated(co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem item) {
        return Result.Created.jsonValue().equals(item.result());
    }
    private boolean isItemFailure(co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem item) {
        return item.status() != 409 && (item.error() != null || item.result() == null);
    }
    private boolean isConflict(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ElasticsearchException exception && exception.status() == 409) return true;
            current = current.getCause();
        }
        return false;
    }
    private Instant greatest(Instant left, Instant right) { return left == null || right.isAfter(left) ? right : left; }
    private SessionItem item(SessionDocument d) { return new SessionItem(d.sessionId(),d.name(),d.createdAt(),d.updatedAt(),d.messageCount()); }
    private record SessionKey(Long userId, String sessionId) { }
    private record SessionStats(Instant maxCreatedAt, Instant maxExpiresAt) {
        private SessionStats merge(SessionStats other) {
            return new SessionStats(greatest(maxCreatedAt, other.maxCreatedAt), greatest(maxExpiresAt, other.maxExpiresAt));
        }

        private static Instant greatest(Instant left, Instant right) { return left == null || right.isAfter(left) ? right : left; }
    }
    public record SessionDocument(@JsonProperty("user_id") Long userId, @JsonProperty("session_id") String sessionId,
                                  @JsonProperty("name") String name, @JsonProperty("created_at") Instant createdAt,
                                  @JsonProperty("updated_at") Instant updatedAt, @JsonProperty("expires_at") Instant expiresAt,
                                  @JsonProperty("message_count") Long messageCount, @JsonProperty("document_type") String documentType) { }
}
