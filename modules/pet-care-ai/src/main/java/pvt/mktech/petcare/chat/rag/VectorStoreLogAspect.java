package pvt.mktech.petcare.chat.rag;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code @description}: 临时测试向量数据库检索
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

        log.info("=== RAG 检索开始 === 查询: {}", query);

        // 执行原方法
        Object result = joinPoint.proceed();

        // 记录检索结果
        if (result instanceof List) {
            List<Document> docs = (List<Document>) result;
            log.info("=== RAG 检索完成 === 数量: {}", docs.size());
            for (int i = 0; i < docs.size(); i++) {
                Object score = docs.get(i).getMetadata().get("distance");
                log.info("片段[{}] 分数: {} | 内容: {}",
                        i + 1,
                        score,
                        truncate(docs.get(i).getText(), 150));
            }
        }

        return result;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
