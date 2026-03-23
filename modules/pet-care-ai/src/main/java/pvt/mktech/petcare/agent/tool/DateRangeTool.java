package pvt.mktech.petcare.agent.tool;

import cn.hutool.core.date.DateUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
     * 日期范围计算函数接口
     */
    @FunctionalInterface
    private interface DateRangeCalculator {
        RangeResult calculate(LocalDate today);
    }

    /**
     * 日期范围计算结果
     */
    private record RangeResult(LocalDateTime start, LocalDateTime end) {}

    /**
     * 关键词规则注册表
     * 每个规则包含：匹配关键词列表、对应的日期范围计算器
     * 新增规则只需要在这里添加即可，无需修改主逻辑
     */
    private final List<Rule> rules = new ArrayList<>();

    /**
     * 规则定义
     */
    private record Rule(List<String> keywords, DateRangeCalculator calculator) {}

    /**
     * 构造函数，注册所有日期规则
     * @description 初始化所有日期解析规则，新增规则只需在这里添加
     * @date 2026-03-18
     * @author Michael Li
     */
    public DateRangeTool() {
        // 今天
        registerRule(List.of("今天", "今日"), today -> {
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(today)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(today)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 明天
        registerRule(List.of("明天"), today -> {
            LocalDate target = today.plusDays(1);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(target)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(target)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 后天
        registerRule(List.of("后天"), today -> {
            LocalDate target = today.plusDays(2);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(target)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(target)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 昨天
        registerRule(List.of("昨天"), today -> {
            LocalDate target = today.minusDays(1);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(target)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(target)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 本周末
        registerRule(List.of("周末", "本周末"), today -> {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate();
            LocalDate saturday = weekStart.plusDays(6);
            LocalDate sunday = weekStart.plusDays(7);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(saturday)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(sunday)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 下周
        registerRule(List.of("下周", "下星期"), today -> {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate().plusWeeks(1);
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(weekStart)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(weekEnd)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 上周
        registerRule(List.of("上周", "上星期"), today -> {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate().minusWeeks(1);
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(weekStart)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(weekEnd)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 本周
        registerRule(List.of("这一周", "本周"), today -> {
            Date todayDate = DateUtil.date(today);
            Date weekStartDate = DateUtil.beginOfWeek(todayDate);
            LocalDate weekStart = DateUtil.date(weekStartDate).toLocalDateTime().toLocalDate();
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(weekStart)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(weekEnd)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 下个月
        registerRule(List.of("下个月"), today -> {
            Date todayDate = DateUtil.date(today);
            LocalDate monthStart = DateUtil.beginOfMonth(todayDate).toLocalDateTime().toLocalDate().plusMonths(1);
            LocalDate monthEnd = DateUtil.endOfMonth(todayDate).toLocalDateTime().toLocalDate().plusMonths(1);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(monthStart)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(monthEnd)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 上个月
        registerRule(List.of("上个月"), today -> {
            Date todayDate = DateUtil.date(today);
            LocalDate monthStart = DateUtil.beginOfMonth(todayDate).toLocalDateTime().toLocalDate().minusMonths(1);
            LocalDate monthEnd = DateUtil.endOfMonth(todayDate).toLocalDateTime().toLocalDate().minusMonths(1);
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(monthStart)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(monthEnd)).toLocalDateTime();
            return new RangeResult(start, end);
        });

        // 本月
        registerRule(List.of("这个月", "本月"), today -> {
            Date todayDate = DateUtil.date(today);
            LocalDate monthStart = DateUtil.beginOfMonth(todayDate).toLocalDateTime().toLocalDate();
            LocalDate monthEnd = DateUtil.endOfMonth(todayDate).toLocalDateTime().toLocalDate();
            LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(monthStart)).toLocalDateTime();
            LocalDateTime end = DateUtil.endOfDay(DateUtil.date(monthEnd)).toLocalDateTime();
            return new RangeResult(start, end);
        });
    }

    /**
     * 注册一条日期解析规则
     * @description 将关键词和对应的计算器注册到规则表中
     * @date 2026-03-18
     * @author Michael Li
     * @param keywords 匹配关键词列表，任一关键词匹配即触发
     * @param calculator 日期范围计算器
     */
    private void registerRule(List<String> keywords, DateRangeCalculator calculator) {
        rules.add(new Rule(keywords, calculator));
    }

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

        // 遍历规则表，匹配第一个命中的关键词规则
        for (Rule rule : rules) {
            for (String keyword : rule.keywords()) {
                if (naturalLanguage.toLowerCase().contains(keyword)) {
                    RangeResult result = rule.calculator().calculate(today);
                    return new DateRangeResult(
                            result.start().format(ISO_FORMATTER),
                            result.end().format(ISO_FORMATTER),
                            naturalLanguage
                    );
                }
            }
        }

        // 默认：今天到明天
        LocalDateTime start = DateUtil.beginOfDay(DateUtil.date(today)).toLocalDateTime();
        LocalDateTime end = DateUtil.endOfDay(DateUtil.date(today.plusDays(1))).toLocalDateTime();

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
