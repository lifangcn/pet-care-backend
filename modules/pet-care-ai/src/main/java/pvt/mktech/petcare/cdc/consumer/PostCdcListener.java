package pvt.mktech.petcare.cdc.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import pvt.mktech.petcare.cdc.constants.EsIndexConstants;
import pvt.mktech.petcare.cdc.consumer.dto.EsPostDocument;
import pvt.mktech.petcare.cdc.consumer.event.DebeziumEvent;
import pvt.mktech.petcare.cdc.consumer.event.PostCdcData;
import pvt.mktech.petcare.cdc.entity.core.LabelEntity;
import pvt.mktech.petcare.cdc.entity.core.PostEntity;
import pvt.mktech.petcare.cdc.entity.core.PostLabelEntity;
import pvt.mktech.petcare.cdc.mapper.core.LabelMapper;
import pvt.mktech.petcare.cdc.mapper.core.PostLabelMapper;
import pvt.mktech.petcare.cdc.mapper.core.PostMapper;
import pvt.mktech.petcare.cdc.service.EsSyncService;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@code @description}: Post CDC 监听器
 * <p>消费 Debezium 发送的 tb_post 变更事件，聚合数据后同步到 ES</p>
 * {@code @date}: 2026-01-27
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCdcListener {

    private final ObjectMapper objectMapper;
    private final PostMapper postMapper;
    private final PostLabelMapper postLabelMapper;
    private final LabelMapper labelMapper;
    private final EsSyncService esSyncService;

    private static final String TOPIC_POST = "petcare_mysql.pet_care_core.tb_post";

    /**
     * 监听 Post 表变更
     */
    @KafkaListener(topics = TOPIC_POST, groupId = "es-sync-group")
    public void onPostChange(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String message = record.value();
            log.debug("收到 Post CDC 消息: {}", message);

            // 解析 Debezium 消息
            DebeziumEvent<PostCdcData> event = objectMapper.readValue(
                    message,
                    objectMapper.getTypeFactory().constructParametricType(DebeziumEvent.class, PostCdcData.class)
            );

            String op = event.getOp();
            PostCdcData cdcData = event.getAfter();

            if (cdcData == null) {
                // delete 操作
                handleDelete(event.getBefore().getId());
            } else if ("c".equals(op) || "u".equals(op)) {
                // create 或 update
                handleUpsert(cdcData);
            }

            // 手动提交 offset
            if (ack != null) {
                ack.acknowledge();
            }

        } catch (Exception e) {
            log.error("处理 Post CDC 消息失败", e);
            // 异常时也不提交，等待下次重试
        }
    }

    /**
     * 处理创建/更新
     */
    private void handleUpsert(PostCdcData cdcData) {
        Long postId = cdcData.getId();

        // 1. 查询完整 Post 数据
        PostEntity post = postMapper.selectOneById(postId);
        if (post == null) {
            log.warn("Post 不存在: {}", postId);
            return;
        }

        // 2. 聚合标签名称
        List<String> labelNames = getLabelNames(postId);

        // 3. 构建 ES 文档
        EsPostDocument document = buildDocument(post, labelNames);

        // 4. 同步到 ES
        esSyncService.upsert(EsIndexConstants.POST_INDEX, String.valueOf(postId), document);
    }

    /**
     * 处理删除
     */
    private void handleDelete(Long postId) {
        esSyncService.delete(EsIndexConstants.POST_INDEX, String.valueOf(postId));
    }

    /**
     * 聚合标签名称
     */
    private List<String> getLabelNames(Long postId) {
        // 查询该 Post 关联的所有标签
        List<PostLabelEntity> postLabels = postLabelMapper.selectListByQuery(
                new QueryWrapper().eq("post_id", postId)
        );

        if (postLabels.isEmpty()) {
            return List.of();
        }

        List<Long> labelIds = postLabels.stream()
                .map(PostLabelEntity::getLabelId)
                .collect(Collectors.toList());

        // 批量查询标签名称
        List<LabelEntity> labels = labelMapper.selectListByIds(labelIds);

        return labels.stream()
                .map(LabelEntity::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建 ES 文档
     */
    private EsPostDocument buildDocument(PostEntity post, List<String> labelNames) {
        EsPostDocument doc = new EsPostDocument();
        doc.setId(post.getId());
        doc.setUserId(post.getUserId());
        doc.setTitle(post.getTitle());
        doc.setContent(post.getContent());
        doc.setPostType(post.getPostType());
        doc.setMediaUrls(parseMediaUrls(post.getMediaUrlsJson()));
        doc.setExternalLink(post.getExternalLink());
        doc.setLocationName(post.getLocationName());
        doc.setLocationAddress(post.getLocationAddress());
        doc.setLocationLatitude(post.getLocationLatitude());
        doc.setLocationLongitude(post.getLocationLongitude());
        doc.setPriceRange(post.getPriceRange());
        doc.setLikeCount(post.getLikeCount());
        doc.setRatingAvg(post.getRatingAvg());
        doc.setViewCount(post.getViewCount());
        doc.setStatus(post.getStatus());
        doc.setActivityId(post.getActivityId());
        doc.setLabels(labelNames);
        doc.setCreatedAt(post.getCreatedAt().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
        return doc;
    }

    /**
     * 解析 media_urls JSON
     */
    private List<String> parseMediaUrls(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("解析 media_urls 失败: {}", json, e);
            return List.of();
        }
    }
}
