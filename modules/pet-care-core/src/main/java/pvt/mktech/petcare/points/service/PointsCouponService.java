package pvt.mktech.petcare.points.service;

import com.mybatisflex.core.paginate.Page;
import pvt.mktech.petcare.points.dto.request.PointsCouponQueryRequest;
import pvt.mktech.petcare.points.dto.response.PointsCouponTemplateResponse;
import pvt.mktech.petcare.points.entity.PointsCoupon;

import java.util.List;

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
     * 领取代金券（充值积分到账户）
     *
     * @param userId   用户ID
     * @param couponId 券ID
     * @return true-成功，false-失败
     */
    boolean redeemCoupon(Long userId, Long couponId);

    /**
     * 抢劵（发放未使用券到用户账户）
     *
     * @param userId     用户ID
     * @param templateId 券模板ID
     * @return 抢到的券ID，失败返回null
     */
    Long grabCoupon(Long userId, Long templateId);

    /**
     * 设置券库存（运营调用）
     *
     * @param templateId 券模板ID
     * @param stock      库存数量
     */
    void setCouponStock(Long templateId, Integer stock);

    /**
     * 查询券剩余库存
     *
     * @param templateId 券模板ID
     * @return 剩余库存
     */
    Long getCouponStock(Long templateId);

    /**
     * 分页查询用户代金券
     *
     * @param userId     用户ID
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @param request    查询条件
     * @return 分页结果
     */
    Page<PointsCoupon> pageCoupons(Long userId, Long pageNumber, Long pageSize, PointsCouponQueryRequest request);

    /**
     * 使用代金券
     *
     * @param couponId 券ID
     * @param recordId 流水记录ID
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

    /**
     * 为新用户发放优惠券的方法
     *
     * @param userId 用户ID，用于标识需要发放优惠券的目标用户
     */
    boolean issueCouponForNewComer(Long userId);

    /**
     * 查询可供抢券的生效模板列表
     *
     * @return 生效模板列表
     */
    List<PointsCouponTemplateResponse> listActiveTemplates();
}
