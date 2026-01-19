package pvt.mktech.petcare.club.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.club.entity.Interaction;

import java.math.BigDecimal;

/**
 * 互动表 服务层。
 */
public interface InteractionService extends IService<Interaction> {

    /**
     * 点赞/取消点赞
     */
    boolean toggleLike(Long userId, Long postId);

    /**
     * 评分
     */
    boolean rate(Long userId, Long postId, Integer rating);

    /**
     * 获取评分详情
     */
    BigDecimal getRatingAvg(Long postId);
}
