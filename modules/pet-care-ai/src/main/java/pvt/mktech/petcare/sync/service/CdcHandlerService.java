package pvt.mktech.petcare.sync.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.converter.DocumentConverter;
import pvt.mktech.petcare.sync.dto.event.CdcData;
import pvt.mktech.petcare.sync.dto.event.DebeziumEvent;

/**
 * {@code @description}: CDC处理核心服务
 * <p>提供通用的CDC事件处理逻辑，消除Listener中的重复代码</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdcHandlerService {

    private final ObjectMapper objectMapper;
    private final SyncService syncService;

    /**
     * 处理CDC事件（通用模板方法）
     *
     * @param record      Kafka消息记录
     * @param cdcDataType CDC数据类型
     * @param converter   文档转换器
     * @param index       ES索引名
     * @param ack         Acknowledgment
     * @param <T>         CDC数据类型
     * @param <R>         ES文档类型
     */
    public <T extends CdcData, R> void handleCdcEvent(
            ConsumerRecord<String, String> record,
            Class<T> cdcDataType,
            DocumentConverter<T, R> converter,
            String index,
            Acknowledgment ack) {

        try {
            String message = record.value();
            log.info("收到 CDC 消息: message={}, topic={}, partition={}, offset={}",
                    message, record.topic(), record.partition(), record.offset());

            // 1. 解析 Debezium 消息
            DebeziumEvent<T> event = parseEvent(message, cdcDataType);

            // 2. 判断操作类型并处理
            String op = event.getOp();
            T cdcData = event.getAfter();

            if (cdcData == null) {
                // delete 操作
                T beforeData = event.getBefore();
                if (beforeData != null && beforeData.getId() != null) {
                    handleDelete(index, beforeData.getId());
                } else {
                    log.warn("Delete操作缺少before数据: {}", message);
                }
            } else if ("c".equals(op) || "u".equals(op)) {
                // create 或 update
                handleUpsert(index, cdcData, converter);
            } else if ("r".equals(op)) {
                // read（快照读取）
                handleUpsert(index, cdcData, converter);
            } else {
                log.info("忽略操作类型: op={}", op);
            }

            // 3. 手动提交 offset
            if (ack != null) {
                ack.acknowledge();
            }

        } catch (Exception e) {
            log.error("处理 CDC 消息失败: topic={}, partition={}, offset={}", 
                    record.topic(), record.partition(), record.offset(), e);
            // 异常时不提交offset，等待下次重试
        }
    }

    /**
     * 解析 Debezium 消息
     */
    private <T> DebeziumEvent<T> parseEvent(String message, Class<T> dataType) throws Exception {
        return objectMapper.readValue(
                message,
                objectMapper.getTypeFactory().constructParametricType(DebeziumEvent.class, dataType)
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
