package pvt.mktech.petcare.points.service;

import pvt.mktech.petcare.points.dto.response.PointsLevelResponse;

/**
 * {@code @description}: 积分等级服务接口
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public interface PointsLevelService {

    /**
     * 根据累计积分计算等级
     *
     * @param totalPoints 累计积分
     * @return 等级（1-10）
     */
    Integer calculateLevel(Integer totalPoints);

    /**
     * 获取等级信息
     *
     * @param level 等级
     * @return 等级信息
     */
    PointsLevelResponse getLevelInfo(Integer level);
}
