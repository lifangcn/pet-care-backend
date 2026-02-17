package pvt.mktech.petcare.points.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.infrastructure.constant.CoreConstant;
import pvt.mktech.petcare.points.entity.codelist.ActionTypeOfPointsRecord;
import pvt.mktech.petcare.points.service.PointsCacheService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * {@code @description}: 积分缓存服务实现
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsCacheServiceImpl implements PointsCacheService {

    private final RedissonClient redissonClient;

    @Override
    public Integer getActionCount(Long userId, ActionTypeOfPointsRecord action) {
        String key = buildActionKey(userId);
        String field = getActionField(action);
        RMap<String, String> map = redissonClient.getMap(key);
        String count = map.get(field);
        return count == null ? 0 : Integer.parseInt(count);
    }

    @Override
    public Integer incrementActionCount(Long userId, ActionTypeOfPointsRecord action) {
        String key = buildActionKey(userId);
        String field = getActionField(action);
        RMap<String, Integer> map = redissonClient.getMap(key);
        // 使用 incr 保证原子性
        return map.addAndGet(field, 1);
    }

    @Override
    public boolean checkAndAddInteraction(Long contentId, Long userId, ActionTypeOfPointsRecord action) {
        String key = buildInteractionKey(contentId, action);
        RSet<String> set = redissonClient.getSet(key);
        // 设置过期时间为当日结束
        set.expire(getEndOfToday());
        // 添加用户ID，返回true表示首次添加
        return set.add(userId.toString());
    }

    @Override
    public Integer getInteractionUserCount(Long contentId, ActionTypeOfPointsRecord action) {
        String key = buildInteractionKey(contentId, action);
        RSet<String> set = redissonClient.getSet(key);
        return set.size();
    }

    /**
     * 构建用户行为缓存 Key
     * 格式: core:user:action:{userId}:{date}
     */
    private String buildActionKey(Long userId) {
        String date = LocalDate.now().toString().replace("-", "");
        return CoreConstant.CORE_USER_ACTION_KEY + userId + ":" + date;
    }

    /**
     * 构建内容互动去重 Key
     * 格式: core:content:like:{contentId}:{date} 或 core:content:comment:{contentId}:{date}
     */
    private String buildInteractionKey(Long contentId, ActionTypeOfPointsRecord action) {
        String date = LocalDate.now().toString().replace("-", "");
        String prefix = action == ActionTypeOfPointsRecord.LIKED
                ? CoreConstant.CORE_CONTENT_LIKE_KEY
                : CoreConstant.CORE_CONTENT_COMMENT_KEY;
        return prefix + contentId + ":" + date;
    }

    /**
     * 获取行为字段名
     */
    private String getActionField(ActionTypeOfPointsRecord action) {
        return switch (action) {
            case CHECK_IN -> "checkin";
            case PUBLISH -> "publish";
            case COMMENT -> "comment";
            case LIKE -> "like";
            default -> throw new IllegalArgumentException("不支持的行为类型: " + action);
        };
    }

    /**
     * 获取今日结束时间
     */
    private Duration getEndOfToday() {
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        return Duration.between(LocalDateTime.now(), endOfDay);
    }
}
