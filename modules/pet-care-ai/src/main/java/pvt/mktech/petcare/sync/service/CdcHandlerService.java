package pvt.mktech.petcare.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.converter.DocumentConverter;
import pvt.mktech.petcare.sync.dto.event.CanalEvent;
import pvt.mktech.petcare.sync.dto.event.CdcData;

import java.util.List;

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

    private final ObjectMapper objectMapper;
    private final SyncService syncService;

    /**
     * 处理 Canal CDC 事件（Canal Flat Message 格式）
     *
     * @param record      Kafka消息记录
     * @param cdcDataType CDC数据类型
     * @param converter   文档转换器
     * @param index       ES索引名
     * @param ack         Acknowledgment
     * @param <T>         CDC数据类型
     * @param <R>         ES文档类型
     */
    public <T extends CdcData, R> void handleCanalEvent(
            ConsumerRecord<String, String> record,
            Class<T> cdcDataType,
            DocumentConverter<T, R> converter,
            String index,
            Acknowledgment ack) {

        try {
            String message = record.value();
            log.info("收到 Canal CDC 消息: message={}, topic={}, partition={}, offset={}",
                    message, record.topic(), record.partition(), record.offset());

            // 1. 解析 Canal 消息
            CanalEvent<T> event = parseCanalEvent(message, cdcDataType);

            // 2. Canal data 是数组，需要遍历处理（批量变更）
            List<T> dataList = event.getData();
            if (dataList == null || dataList.isEmpty()) {
                log.warn("Canal 消息 data 为空: {}", message);
                if (ack != null) {
                    ack.acknowledge();
                }
                return;
            }

            // 3. 根据操作类型处理
            String type = event.getType();
            for (T cdcData : dataList) {
                if ("DELETE".equals(type)) {
                    handleDelete(index, cdcData.getId());
                } else if ("INSERT".equals(type) || "UPDATE".equals(type)) {
                    handleUpsert(index, cdcData, converter);
                } else {
                    log.info("忽略操作类型: type={}", type);
                }
            }

            // 4. 手动提交 offset
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
    private <T extends CdcData, R> void handleUpsert(
            String index,
            T cdcData,
            DocumentConverter<T, R> converter) {

        try {
            // 转换为ES文档
            R document = converter.convert(cdcData);

            // 同步到ES
            syncService.upsert(index, String.valueOf(cdcData.getId()), document);

            log.info("CDC upsert 成功: index={}, id={}", index, cdcData.getId());
        } catch (Exception e) {
            log.error("CDC upsert 失败: index={}, id={}", index, cdcData.getId(), e);
            throw e;
        }
    }

    /**
     * 处理删除操作
     */
    private void handleDelete(String index, Long id) {
        try {
            syncService.delete(index, String.valueOf(id));
            log.info("CDC delete 成功: index={}, id={}", index, id);
        } catch (Exception e) {
            log.error("CDC delete 失败: index={}, id={}", index, id, e);
            throw e;
        }
    }
}
