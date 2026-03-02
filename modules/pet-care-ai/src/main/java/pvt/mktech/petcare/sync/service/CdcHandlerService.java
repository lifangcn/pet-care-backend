package pvt.mktech.petcare.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.dto.event.CanalEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * {@code @description}: CDC处理核心服务
 * <p>提供通用的CDC事件处理逻辑，消除Listener中的重复代码</p>
 * {@code @date}: 2026-01-30
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdcHandlerService {

    /**
     * 高频变动字段（仅这些字段变动时跳过 ES 同步）
     */
    private static final Set<String> HIGH_FREQ_FIELDS = Set.of(
        "updated_at", "view_count", "like_count", "rating_count",
        "rating_total", "rating_avg", "current_participants", "check_in_count"
    );

    private final ObjectMapper objectMapper;
    private final SyncService syncService;

    /**
     * 处理 Canal CDC 事件（Canal Flat Message 格式）
     *
     * @param record      Kafka消息记录
     * @param documentType ES文档类型（直接从 Canal 反序列化）
     * @param index       ES索引名
     * @param ack         Acknowledgment
     * @param <T>         文档类型
     */
    public <T> void handleCanalEvent(ConsumerRecord<String, String> record, Class<T> documentType,
            String index, Acknowledgment ack) {

        try {
            String message = record.value();
            log.info("收到 Canal CDC 消息: message={}, topic={}, partition={}, offset={}",
                    message, record.topic(), record.partition(), record.offset());

            // 1. 解析 Canal 消息（直接反序列化为目标文档类型）
            CanalEvent<T> event = parseCanalEvent(message, documentType);

            // 2. 高频字段过滤：仅高频字段变动时跳过
            if (isOnlyHighFreqFieldsChanged(event)) {
                log.info("仅高频字段变动，跳过 ES 同步: type={}, table={}", event.getType(), event.getTable());
                if (ack != null) {
                    ack.acknowledge();
                }
                return;
            }

            // 3. Canal data 是数组，需要遍历处理（批量变更）
            List<T> dataList = event.getData();
            if (dataList == null || dataList.isEmpty()) {
                log.warn("Canal 消息 data 为空: {}", message);
                if (ack != null) {
                    ack.acknowledge();
                }
                return;
            }

            // 4. 根据操作类型处理
            String type = event.getType();
            for (T document : dataList) {
                if ("DELETE".equals(type)) {
                    handleDelete(index, document);
                } else if ("INSERT".equals(type) || "UPDATE".equals(type)) {
                    // 数据质量过滤
                    if (!shouldSync(document)) {
                        log.debug("数据不满足同步质量要求，跳过: id={}", getId(document));
                        continue;
                    }
                    handleUpsert(index, document);
                } else {
                    log.info("忽略操作类型: type={}", type);
                }
            }

            // 5. 手动提交 offset
            if (ack != null) {
                ack.acknowledge();
            }

        } catch (Exception e) {
            log.error("处理 Canal CDC 消息失败: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e);
            // 异常时不提交offset，等待下次重试
        }
    }

    /**
     * 解析 Canal 消息（Flat Message 格式）
     */
    private <T> CanalEvent<T> parseCanalEvent(String message, Class<T> dataType) throws Exception {
        return objectMapper.readValue(
                message,
                objectMapper.getTypeFactory().constructParametricType(CanalEvent.class, dataType)
        );
    }

    /**
     * 处理创建/更新操作
     */
    private <T> void handleUpsert(String index, T document) {
        try {
            syncService.upsert(index, String.valueOf(getId(document)), document);
            log.info("CDC upsert 成功: index={}, id={}", index, getId(document));
        } catch (Exception e) {
            log.error("CDC upsert 失败: index={}, id={}", index, getId(document), e);
            throw e;
        }
    }

    /**
     * 处理删除操作
     */
    private <T> void handleDelete(String index, T document) {
        try {
            Long id = getId(document);
            syncService.delete(index, String.valueOf(id));
            log.info("CDC delete 成功: index={}, id={}", index, id);
        } catch (Exception e) {
            log.error("CDC delete 失败: index={}, id={}", index, getId(document), e);
            throw e;
        }
    }

    /**
     * 从文档中提取 ID（通过反射获取 id 字段）
     */
    private <T> Long getId(T document) {
        try {
            java.lang.reflect.Field field = document.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return (Long) field.get(document);
        } catch (Exception e) {
            throw new RuntimeException("无法获取文档 ID", e);
        }
    }

    /**
     * 检查是否仅高频字段变动（若是则跳过 ES 同步）
     *
     * @param event Canal 事件
     * @param <T>    文档类型
     * @return true-仅高频字段变动，false-有其他字段变动
     */
    private <T> boolean isOnlyHighFreqFieldsChanged(CanalEvent<T> event) {
        if (!"UPDATE".equals(event.getType()) || event.getOld() == null || event.getOld().isEmpty()) {
            return false;
        }
        // old 的 keySet 就是变动的字段
        T oldData = event.getOld().get(0);
        for (Field field : oldData.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(oldData);
                if (value != null) {
                    String fieldName = camelToSnake(field.getName());
                    if (!HIGH_FREQ_FIELDS.contains(fieldName)) {
                        return false; // 有非高频字段变动
                    }
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return true; // 只有高频字段变动
    }

    /**
     * 检查数据是否符合同步质量要求
     *
     * @param document 文档对象
     * @param <T>      文档类型
     * @return true-符合同步要求，false-不符合
     */
    private <T> boolean shouldSync(T document) {
        Integer enabled = getFieldValue(document, "enabled");
        Integer isDeleted = getFieldValue(document, "isDeleted");
        // 只同步：enabled=1 且 is_deleted=0
        return enabled != null && enabled == 1
            && (isDeleted == null || isDeleted == 0);
    }

    /**
     * 通过反射获取字段值
     *
     * @param document 文档对象
     * @param fieldName 字段名（驼峰命名）
     * @param <T>      文档类型
     * @param <V>      返回值类型
     * @return 字段值
     */
    private <T, V> V getFieldValue(T document, String fieldName) {
        try {
            Field field = document.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (V) field.get(document);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 驼峰命名转下划线命名
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
