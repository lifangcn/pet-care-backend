package pvt.mktech.petcare.chat.mapper;

import com.mybatisflex.annotation.UseDataSource;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import pvt.mktech.petcare.chat.store.ChatSessionRecord;
import pvt.mktech.petcare.chat.store.PostgresqlChatMessagePayload;
import pvt.mktech.petcare.chat.store.PostgresqlChatMessageRow;

import java.time.Instant;
import java.util.List;

@Mapper
@UseDataSource("ai")
public interface ChatHistoryMapper {
    @Insert("INSERT INTO petcare.chat_session(session_id,user_id,name,created_at,updated_at,expires_at) VALUES(#{id},#{userId},#{name},CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP + (#{days} * INTERVAL '1 day')) ON CONFLICT (session_id,user_id) DO NOTHING")
    void ensureSession(@Param("userId") Long userId, @Param("id") String sessionId, @Param("name") String name, @Param("days") int days);

    @Select("SELECT s.session_id sessionId,s.user_id userId,s.name,s.created_at createdAt,s.updated_at updatedAt,s.expires_at expiresAt,COUNT(m.id) messageCount FROM petcare.chat_session s LEFT JOIN petcare.chat_message m ON m.session_id=s.session_id AND m.user_id=s.user_id AND m.expires_at>CURRENT_TIMESTAMP WHERE s.user_id=#{userId} AND s.session_id=#{id} AND s.expires_at>CURRENT_TIMESTAMP GROUP BY s.session_id,s.user_id")
    ChatSessionRecord getSession(@Param("userId") Long userId, @Param("id") String sessionId);

    @Select("SELECT s.session_id sessionId,s.user_id userId,s.name,s.created_at createdAt,s.updated_at updatedAt,s.expires_at expiresAt,COUNT(m.id) messageCount FROM petcare.chat_session s LEFT JOIN petcare.chat_message m ON m.session_id=s.session_id AND m.user_id=s.user_id AND m.expires_at>CURRENT_TIMESTAMP WHERE s.user_id=#{userId} AND s.expires_at>CURRENT_TIMESTAMP GROUP BY s.session_id,s.user_id ORDER BY s.updated_at DESC OFFSET #{offset} LIMIT #{limit}")
    List<ChatSessionRecord> listSessions(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM petcare.chat_session WHERE user_id=#{userId} AND expires_at>CURRENT_TIMESTAMP")
    long countSessions(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO petcare.chat_message(id,conversation_id,user_id,session_id,role,content,embedding,metadata,created_at,expires_at)
            VALUES(#{payload.id},#{payload.conversationId},#{payload.userId},#{payload.sessionId},#{payload.role},#{payload.content},
                   CAST(#{payload.embedding} AS vector),CAST(#{payload.metadataJson} AS jsonb),#{payload.createdAt},#{payload.expiresAt})
            ON CONFLICT (id) DO NOTHING
            """)
    int insertMessage(@Param("payload") PostgresqlChatMessagePayload payload);

    @Update("UPDATE petcare.chat_message SET embedding=CAST(#{embedding} AS vector) WHERE id=#{id} AND user_id=#{userId} AND session_id=#{sessionId}")
    void updateEmbedding(@Param("id") Long id, @Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("embedding") String embedding);

    @Update("UPDATE petcare.chat_session SET updated_at=GREATEST(updated_at, #{createdAt}), expires_at=GREATEST(expires_at, #{expiresAt}) WHERE user_id=#{userId} AND session_id=#{id}")
    void touch(@Param("userId") Long userId, @Param("id") String sessionId, @Param("createdAt") Instant createdAt, @Param("expiresAt") Instant expiresAt);

    @Select("""
            SELECT m.id,m.conversation_id conversationId,m.user_id userId,m.session_id sessionId,m.role,m.content,
                   m.metadata::text metadataJson,m.created_at createdAt,m.expires_at expiresAt
            FROM petcare.chat_message m JOIN petcare.chat_session s ON s.user_id=m.user_id AND s.session_id=m.session_id
            WHERE m.user_id=#{userId} AND m.session_id=#{id} AND m.expires_at>CURRENT_TIMESTAMP AND s.expires_at>CURRENT_TIMESTAMP
            ORDER BY m.created_at ASC LIMIT #{limit}
            """)
    List<PostgresqlChatMessageRow> history(@Param("userId") Long userId, @Param("id") String id, @Param("limit") int limit);

    @Select("""
            WITH candidates AS (
                SELECT m.id,m.conversation_id conversationId,m.user_id userId,m.session_id sessionId,m.role,m.content,
                       m.metadata::text metadataJson,m.created_at createdAt,m.expires_at expiresAt,
                       m.embedding <=> CAST(#{embedding} AS vector) distance
                FROM petcare.chat_message m JOIN petcare.chat_session s ON s.user_id=m.user_id AND s.session_id=m.session_id
                WHERE m.user_id=#{userId} AND m.role='USER' AND m.embedding IS NOT NULL
                  AND m.expires_at>CURRENT_TIMESTAMP AND s.expires_at>CURRENT_TIMESTAMP
                  AND m.created_at>=CURRENT_TIMESTAMP-(#{days} * INTERVAL '1 day')
                ORDER BY m.embedding <=> CAST(#{embedding} AS vector)
                LIMIT #{candidateLimit}
            )
            SELECT id,conversationId,userId,sessionId,role,content,metadataJson,createdAt,expiresAt
            FROM candidates WHERE 1-distance>=#{minScore} ORDER BY distance ASC,id ASC LIMIT #{topK}
            """)
    List<PostgresqlChatMessageRow> semantic(@Param("userId") Long userId, @Param("embedding") String embedding,
                                             @Param("topK") int topK, @Param("candidateLimit") int candidateLimit,
                                             @Param("minScore") double minScore, @Param("days") int days);

    @Select("SELECT COUNT(*) FROM petcare.chat_message WHERE user_id=#{userId} AND session_id=#{id}")
    long countMessagesForDeletion(@Param("userId") Long userId, @Param("id") String id);

    @Select("SELECT COUNT(*) FROM petcare.chat_message WHERE user_id=#{userId}")
    long countMessagesForUserDeletion(@Param("userId") Long userId);

    @Delete("DELETE FROM petcare.chat_session WHERE user_id=#{userId} AND session_id=#{id}")
    int deleteSession(@Param("userId") Long userId, @Param("id") String id);

    @Delete("DELETE FROM petcare.chat_session WHERE user_id=#{userId}")
    int deleteUser(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*) FROM petcare.chat_message m JOIN petcare.chat_session s ON s.user_id=m.user_id AND s.session_id=m.session_id
            WHERE m.user_id=#{userId} AND m.session_id=#{id} AND m.expires_at>CURRENT_TIMESTAMP AND s.expires_at>CURRENT_TIMESTAMP
            """)
    long countMessages(@Param("userId") Long userId, @Param("id") String id);

    @Update("UPDATE petcare.chat_session SET name=#{name} WHERE user_id=#{userId} AND session_id=#{id} AND name='新对话' AND expires_at>CURRENT_TIMESTAMP")
    int updateDefaultName(@Param("userId") Long userId, @Param("id") String id, @Param("name") String name);

    @Delete("""
            WITH expired AS (
                SELECT ctid FROM petcare.chat_message WHERE expires_at<CURRENT_TIMESTAMP
                ORDER BY expires_at FOR UPDATE SKIP LOCKED LIMIT #{size}
            ) DELETE FROM petcare.chat_message WHERE ctid IN (SELECT ctid FROM expired)
            """)
    int deleteExpiredMessages(@Param("size") int size);

    @Delete("""
            WITH expired AS (
                SELECT s.ctid FROM petcare.chat_session s
                WHERE s.expires_at<CURRENT_TIMESTAMP
                  AND NOT EXISTS (SELECT 1 FROM petcare.chat_message m WHERE m.user_id=s.user_id AND m.session_id=s.session_id)
                ORDER BY s.expires_at FOR UPDATE SKIP LOCKED LIMIT #{size}
            ) DELETE FROM petcare.chat_session WHERE ctid IN (SELECT ctid FROM expired)
            """)
    int deleteEmptyExpiredSessions(@Param("size") int size);
}
