package pvt.mktech.petcare.points.service;

import pvt.mktech.petcare.points.entity.codelist.PointsActionType;

/**
 * {@code @description}: 积分缓存服务接口
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public interface PointsCacheService {

    /**
     * 获取用户当日行为次数
     *
     * @param userId    用户ID
     * @param action    行为类型
     * @return 当日已执行次数
     */
    Integer getActionCount(Long userId, PointsActionType action);

    /**
     * 增加用户当日行为次数
     *
     * @param userId    用户ID
     * @param action    行为类型
     * @return 增加后的次数
     */
    Integer incrementActionCount(Long userId, PointsActionType action);

    /**
     * 检查内容是否被指定用户互动过（去重）
     *
     * @param contentId 内容ID
     * @param userId    用户ID
     * @param action    行为类型（被点赞/被评论）
     * @return true-首次互动，false-已互动过
     */
    boolean checkAndAddInteraction(Long contentId, Long userId, PointsActionType action);

    /**
     * 获取内容当日互动用户数
     *
     * @param contentId 内容ID
     * @param action    行为类型
     * @return 当日互动用户数
     */
    Integer getInteractionUserCount(Long contentId, PointsActionType action);
}
