package pvt.mktech.petcare.social.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.social.entity.Interaction;

/**
 * 互动表 服务层。
 */
public interface InteractionService extends IService<Interaction> {

    /**
     * 点赞/取消点赞（仅操作 Interaction 表）
     *
     * @param userId 用户ID
     * @param postId 动态ID
     * @return true=新增点赞, false=取消点赞
     */
    boolean toggleLike(Long userId, Long postId);

    /**
     * 评分
     */
    boolean rate(Long userId, Long postId, Integer rating);

    /**
     * 获取当前用户对动态的评分
     *
     * @param userId 用户ID
     * @param id     动态ID
     * @return 评分值 1-5，未评分为null
     */
    Integer getUserRating(Long userId, Long id);
}
