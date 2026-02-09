package pvt.mktech.petcare.points.service;

import com.mybatisflex.core.paginate.Page;
import pvt.mktech.petcare.points.dto.request.PointsCouponQueryRequest;
import pvt.mktech.petcare.points.entity.PointsCoupon;

/**
 * {@code @description}: 积分代金券服务接口
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public interface PointsCouponService {

    /**
     * 发放代金券给用户
     *
     * @param userId     用户ID
     * @param templateId 券模板ID
     * @return 发放的代金券ID
     */
    Long issueCoupon(Long userId, Long templateId);

    /**
     * 分页查询用户代金券
     *
     * @param userId 用户ID
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @param request 查询条件
     * @return 分页结果
     */
    Page<PointsCoupon> pageCoupons(Long userId, Long pageNumber, Long pageSize, PointsCouponQueryRequest request);

    /**
     * 使用代金券
     *
     * @param couponId   券ID
     * @param recordId   流水记录ID
     * @return true-成功，false-失败
     */
    boolean useCoupon(Long couponId, Long recordId);

    /**
     * 校验代金券是否可用
     *
     * @param couponId 券ID
     * @param userId   用户ID
     * @return true-可用，false-不可用
     */
    boolean validateCoupon(Long couponId, Long userId);
}
