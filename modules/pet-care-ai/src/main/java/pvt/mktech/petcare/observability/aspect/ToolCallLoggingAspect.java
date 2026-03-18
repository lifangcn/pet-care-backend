package pvt.mktech.petcare.observability.aspect;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.observability.context.ObservationContext;
import pvt.mktech.petcare.observability.context.ObservationContextManager;

import java.time.Instant;

/**
 * 工具调用日志切面
 * 职责：拦截所有工具方法调用，记录调用详情到 ObservationContext
 *
 * @description: 拦截带有 @Tool 注解的方法，记录工具名称、参数、结果、耗时、是否成功
 * @date: 2026-03-17
 * @author Michael Li
 */
@Aspect
@Component
@Slf4j
public class ToolCallLoggingAspect {

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object logToolCall(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!ObservationContextManager.exists()) {
            return joinPoint.proceed();
        }

        // 获取工具方法信息
        String toolName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String argumentsJson = JSONUtil.toJsonStr(args);

        ObservationContext.ToolCallInfo toolCallInfo = new ObservationContext.ToolCallInfo();
        toolCallInfo.setToolName(toolName);
        toolCallInfo.setArguments(argumentsJson);
        toolCallInfo.setStartTime(Instant.now());

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - startTime;

            toolCallInfo.setDurationMs(durationMs);
            toolCallInfo.setSuccess(true);
            toolCallInfo.setResult(JSONUtil.toJsonStr(result));

            log.debug("[可观测性] 工具调用成功: toolName={}, durationMs={}", toolName, durationMs);

            // 写入追踪上下文
            addToObservationContext(toolCallInfo);

            return result;

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;

            toolCallInfo.setDurationMs(durationMs);
            toolCallInfo.setSuccess(false);
            toolCallInfo.setResult(e.getMessage());

            log.warn("[可观测性] 工具调用失败: toolName={}, error={}", toolName, e.getMessage());

            addToObservationContext(toolCallInfo);

            throw e;
        }
    }

    /**
     * 将工具调用信息添加到追踪上下文
     */
    private void addToObservationContext(ObservationContext.ToolCallInfo toolCallInfo) {
        try {
            ObservationContext context = ObservationContextManager.get();
            if (context != null) {
                context.getToolCalls().add(toolCallInfo);
            }
        } catch (Exception e) {
            log.warn("[可观测性] 写入工具调用信息到追踪上下文失败", e);
        }
    }
}
