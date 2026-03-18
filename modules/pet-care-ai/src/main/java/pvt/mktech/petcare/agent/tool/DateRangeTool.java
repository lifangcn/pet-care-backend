package pvt.mktech.petcare.agent.tool;

import cn.hutool.core.date.DateUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * {@code @description}: 日期范围解析工具 - 将自然语言描述的时间转换为具体日期范围
 * 例如："这个周末" -> 计算出本周六和周日的日期范围
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Component
public class DateRangeTool {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * 计算日期范围，将自然语言描述转换为具体的开始和结束时间
     * @param naturalLanguage 自然语言描述，例如："这个周末"、"下周"、"明天"、"下周一开始"
     * @return 日期范围结果（ISO 格式）
     */
    @Tool(name = "calculateDateRange", description = "计算日期范围，将自然语言描述（如'周末'、'下周'、'明天'）转换为具体的开始和结束时间")
    public DateRangeResult calculateDateRange(
            @ToolParam(description = "自然语言描述的时间范围，例如：'这个周末'、'下周'、'明天'") String naturalLanguage) {

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime start;
        LocalDateTime end;

        String lower = naturalLanguage.toLowerCase();

        if (lower.contains("今天") || lower.contains("今日")) {
            start = DateUtil.beginOfDay(DateUtil.date(today)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(today)).toLocalDateTime();
        } else if (lower.contains("明天")) {
            LocalDate tomorrow = today.plusDays(1);
            start = DateUtil.beginOfDay(DateUtil.date(tomorrow)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(tomorrow)).toLocalDateTime();
        } else if (lower.contains("后天")) {
            LocalDate afterTomorrow = today.plusDays(2);
            start = DateUtil.beginOfDay(DateUtil.date(afterTomorrow)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(afterTomorrow)).toLocalDateTime();
        } else if (lower.contains("昨天")) {
            LocalDate yesterday = today.minusDays(1);
            start = DateUtil.beginOfDay(DateUtil.date(yesterday)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(yesterday)).toLocalDateTime();
        } else if (lower.contains("周末") || lower.contains("本周末")) {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate();
            LocalDate saturday = weekStart.plusDays(6);
            LocalDate sunday = weekStart.plusDays(7);
            start = DateUtil.beginOfDay(DateUtil.date(saturday)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(sunday)).toLocalDateTime();
        } else if (lower.contains("下周") || lower.contains("下星期")) {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate().plusWeeks(1);
            LocalDate weekEnd = weekStart.plusDays(6);
            start = DateUtil.beginOfDay(DateUtil.date(weekStart)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(weekEnd)).toLocalDateTime();
        } else if (lower.contains("上周") || lower.contains("上星期")) {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate().minusWeeks(1);
            LocalDate weekEnd = weekStart.plusDays(6);
            start = DateUtil.beginOfDay(DateUtil.date(weekStart)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(weekEnd)).toLocalDateTime();
        } else if (lower.contains("这一周") || lower.contains("本周")) {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate();
            LocalDate weekEnd = weekStart.plusDays(6);
            start = DateUtil.beginOfDay(DateUtil.date(weekStart)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(weekEnd)).toLocalDateTime();
        } else if (lower.contains("下个月")) {
            Date todayDate = DateUtil.date(today);
            LocalDate monthStart = DateUtil.beginOfMonth(todayDate).toLocalDateTime().toLocalDate().plusMonths(1);
            LocalDate monthEnd = DateUtil.endOfMonth(todayDate).toLocalDateTime().toLocalDate().plusMonths(1);
            start = DateUtil.beginOfDay(DateUtil.date(monthStart)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(monthEnd)).toLocalDateTime();
        } else if (lower.contains("上个月")) {
            Date todayDate = DateUtil.date(today);
            LocalDate monthStart = DateUtil.beginOfMonth(todayDate).toLocalDateTime().toLocalDate().minusMonths(1);
            LocalDate monthEnd = DateUtil.endOfMonth(todayDate).toLocalDateTime().toLocalDate().minusMonths(1);
            start = DateUtil.beginOfDay(DateUtil.date(monthStart)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(monthEnd)).toLocalDateTime();
        } else if (lower.contains("这个月") || lower.contains("本月")) {
            Date todayDate = DateUtil.date(today);
            LocalDate monthStart = DateUtil.beginOfMonth(todayDate).toLocalDateTime().toLocalDate();
            LocalDate monthEnd = DateUtil.endOfMonth(todayDate).toLocalDateTime().toLocalDate();
            start = DateUtil.beginOfDay(DateUtil.date(monthStart)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(monthEnd)).toLocalDateTime();
        } else {
            start = DateUtil.beginOfDay(DateUtil.date(today)).toLocalDateTime();
            end = DateUtil.endOfDay(DateUtil.date(today.plusDays(1))).toLocalDateTime();
        }

        return new DateRangeResult(
                start.format(ISO_FORMATTER),
                end.format(ISO_FORMATTER),
                naturalLanguage
        );
    }

    /**
     * 日期范围结果
     */
    public record DateRangeResult(
            String startTime,
            String endTime,
            String originalDescription
    ) {}
}
