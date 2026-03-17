package pvt.mktech.petcare.chat.rag;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.observability.context.ObservationContext;
import pvt.mktech.petcare.observability.context.ObservationContextManager;

import java.util.List;

/**
 * {@code @description}: 向量数据库检索日志切面
 * 职责：拦截 RAG 检索，记录 console 日志并写入 ObservationContext
 * {@code @date}: 2026/1/16 18:30
 *
 * @author Michael
 */

@Aspect
@Component
@Slf4j
public class VectorStoreLogAspect {

    @Around("execution(* org.springframework.ai.vectorstore.VectorStore.similaritySearch(..))")
    public Object logSimilaritySearch(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        String query = args[0].toString();

        long startTime = System.currentTimeMillis();

        log.info("=== RAG 检索开始 === 查询: {}", query);

        // 执行原方法
        Object result = joinPoint.proceed();

        long durationMs = System.currentTimeMillis() - startTime;

        // 记录检索结果（保留原有 console 日志）
        if (result instanceof List) {
            List<Document> docs = (List<Document>) result;
            log.info("=== RAG 检索完成 === 数量: {}", docs.size());

            double topScore = 0.0;
            for (int i = 0; i < docs.size(); i++) {
                Object score = docs.get(i).getMetadata().get("distance");
                if (score instanceof Number) {
                    double scoreValue = ((Number) score).doubleValue();
                    if (scoreValue > topScore) {
                        topScore = scoreValue;
                    }
                }
                log.info("片段[{}] 分数: {} | 内容: {}",
                        i + 1,
                        score,
                        truncate(docs.get(i).getText(), 150));
            }

            // 写入 ObservationContext（新增）
            writeRagInfoToContext(query, docs.size(), topScore, durationMs);
        }

        return result;
    }

    /**
     * 将 RAG 检索信息写入 ObservationContext
     */
    private void writeRagInfoToContext(String query, int resultsCount, double topScore, long durationMs) {
        if (!ObservationContextManager.exists()) {
            return;
        }

        try {
            ObservationContext context = ObservationContextManager.get();
            ObservationContext.RAGInfo ragInfo = new ObservationContext.RAGInfo();
            ragInfo.setEnabled(true);
            ragInfo.setQuery(query);
            ragInfo.setResultsCount(resultsCount);
            ragInfo.setTopScore(topScore);
            ragInfo.setDurationMs(durationMs);

            context.setRagInfo(ragInfo);
        } catch (Exception e) {
            log.warn("写入 RAG 信息到追踪上下文失败", e);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
