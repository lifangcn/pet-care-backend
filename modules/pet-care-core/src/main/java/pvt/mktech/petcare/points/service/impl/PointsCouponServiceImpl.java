package pvt.mktech.petcare.points.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.points.dto.request.PointsCouponQueryRequest;
import pvt.mktech.petcare.points.dto.response.PointsCouponTemplateResponse;
import pvt.mktech.petcare.points.entity.PointsCoupon;
import pvt.mktech.petcare.points.entity.PointsCouponTemplate;
import pvt.mktech.petcare.points.entity.codelist.SourceTypeOfCouponTemplate;
import pvt.mktech.petcare.points.entity.codelist.StatusOfPointsCoupon;
import pvt.mktech.petcare.points.event.CouponRedeemEvent;
import pvt.mktech.petcare.points.mapper.PointsCouponMapper;
import pvt.mktech.petcare.points.mapper.PointsCouponTemplateMapper;
import pvt.mktech.petcare.points.service.PointsCouponService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_COUPON_STOCK_KEY_PREFIX;
import static pvt.mktech.petcare.points.entity.table.PointsCouponTableDef.POINTS_COUPON;
import static pvt.mktech.petcare.points.entity.table.PointsCouponTemplateTableDef.POINTS_COUPON_TEMPLATE;

/**
 * {@code @description}: 积分代金券服务实现
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsCouponServiceImpl extends ServiceImpl<PointsCouponMapper, PointsCoupon> implements PointsCouponService {

    private final RedisUtil redisUtil;
    private final PointsCouponTemplateMapper pointsCouponTemplateMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long issueCoupon(Long userId, Long templateId) {
        // 查询券模板
        PointsCouponTemplate template = pointsCouponTemplateMapper.selectOneById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券模板不存在或已停用");
        }

        // 检查发放总量
        if (template.getTotalCount() > 0 && template.getIssuedCount() >= template.getTotalCount()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券已发放完毕");
        }

        // 检查用户领取数量
        long userCount = count(QueryWrapper.create()
                .where(POINTS_COUPON.USER_ID.eq(userId))
                .and(POINTS_COUPON.TEMPLATE_ID.eq(templateId))
                .and(POINTS_COUPON.STATUS.eq(StatusOfPointsCoupon.UNUSED)));
        if (userCount >= template.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已达到领取上限");
        }

        // 创建代金券
        PointsCoupon coupon = new PointsCoupon();
        coupon.setUserId(userId);
        coupon.setTemplateId(templateId);
        coupon.setFaceValue(template.getFaceValue());
        coupon.setStatus(StatusOfPointsCoupon.UNUSED);
        coupon.setStartTime(LocalDateTime.now());
        coupon.setEndTime(LocalDateTime.now().plusDays(template.getValidDays()));
        save(coupon);

        // 更新已发放数量
        template.setIssuedCount(template.getIssuedCount() + 1);
        pointsCouponTemplateMapper.update(template);

        return coupon.getId();
    }

    @Override
    public Page<PointsCoupon> pageCoupons(Long userId, Long pageNumber, Long pageSize, PointsCouponQueryRequest request) {
        QueryWrapper queryWrapper = queryChain()
                .where(POINTS_COUPON.USER_ID.eq(userId));

        Optional<StatusOfPointsCoupon> statusOfPointsCoupon = StatusOfPointsCoupon.fromCode(request.getStatus());
        statusOfPointsCoupon.ifPresent(ofPointsCoupon -> queryWrapper.and(POINTS_COUPON.STATUS.eq(ofPointsCoupon)));

        // 默认按创建时间倒序
        queryWrapper.orderBy(POINTS_COUPON.CREATED_AT.desc());

        return page(Page.of(pageNumber, pageSize), queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useCoupon(Long couponId, Long recordId) {
        PointsCoupon coupon = getById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券不存在");
        }

        if (!coupon.getStatus().equals(StatusOfPointsCoupon.USED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券已使用或已过期");
        }

        if (coupon.getEndTime().isBefore(LocalDateTime.now())) {
            // 标记为已过期
            coupon.setStatus(StatusOfPointsCoupon.EXPIRED);
            updateById(coupon);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券已过期");
        }

        coupon.setStatus(StatusOfPointsCoupon.USED);
        coupon.setUsedTime(LocalDateTime.now());
        coupon.setUsedRecordId(recordId);
        return updateById(coupon);
    }

    @Override
    public boolean validateCoupon(Long couponId, Long userId) {
        PointsCoupon coupon = getById(couponId);
        if (coupon == null) {
            return false;
        }
        return coupon.getUserId().equals(userId)
                && coupon.getStatus().equals(StatusOfPointsCoupon.UNUSED)
                && coupon.getEndTime().isAfter(LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean issueCouponForNewComer(Long userId) {
        // 查询新人券模板并自动领取
        List<PointsCouponTemplate> newcomerTemplates = pointsCouponTemplateMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(POINTS_COUPON_TEMPLATE.SOURCE_TYPE.eq(SourceTypeOfCouponTemplate.NEWCOMER))
                        .and(POINTS_COUPON_TEMPLATE.STATUS.eq(1))
                        .limit(1)
        );
        if (!newcomerTemplates.isEmpty()) {
            PointsCouponTemplate template = newcomerTemplates.getFirst();
            // 发放新人券
            Long couponId = issueCoupon(userId, template.getId());
            // 立即领取
            redeemCoupon(userId, couponId);
        }
        return false;
    }

    /**
     * {@code @description}: 领取积分券（充值积分到账户）
     * 通过事件解耦，避免循环依赖
     * {@code @date}: 2026/02/13
     * {@code @author}: Michael
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean redeemCoupon(Long userId, Long couponId) {
        // 1. 校验券归属、状态、有效期
        PointsCoupon coupon = getById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券不存在");
        }
        if (!coupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无权操作此券");
        }
        if (!coupon.getStatus().equals(StatusOfPointsCoupon.UNUSED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券已使用或已过期");
        }
        if (coupon.getEndTime().isBefore(LocalDateTime.now())) {
            // 标记为已过期
            coupon.setStatus(StatusOfPointsCoupon.EXPIRED);
            updateById(coupon);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券已过期");
        }

        // 2. 更新券状态为已使用
        coupon.setStatus(StatusOfPointsCoupon.USED);
        coupon.setUsedTime(LocalDateTime.now());
        updateById(coupon);

        // 3. 发布券兑换事件（监听器在同一事务内处理余额更新）
        applicationEventPublisher.publishEvent(new CouponRedeemEvent(userId, couponId));
        return true;
    }

    /**
     * {@code @description}: 抢劵（发放未使用券到用户账户）
     * {@code @date}: 2026/02/13
     * {@code @author}: Michael
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long grabCoupon(Long userId, Long templateId) {
        // 1. 校验券模板
        PointsCouponTemplate template = pointsCouponTemplateMapper.selectOneById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券模板不存在或已停用");
        }

        // 2. Redis原子减库存
        String stockKey = CORE_COUPON_STOCK_KEY_PREFIX + templateId;
        Long remainingStock = redisUtil.decrement(stockKey, 1L);
        if (remainingStock < 0) {
            // 库存不足，回滚
            redisUtil.increment(stockKey, 1);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券已被抢完");
        }

        try {
            // 3. 发放未使用券
            Long couponId = issueCoupon(userId, templateId);
            return couponId;
        } catch (Exception e) {
            // 发券失败，回滚库存
            redisUtil.increment(stockKey, 1);
            throw e;
        }
    }

    /**
     * {@code @description}: 设置券库存（运营调用）
     * {@code @date}: 2026/02/13
     * {@code @author}: Michael
     */
    @Override
    public void setCouponStock(Long templateId, Integer stock) {
        String stockKey = CORE_COUPON_STOCK_KEY_PREFIX + templateId;
        redisUtil.setAtomicLong(stockKey, stock);
    }

    /**
     * {@code @description}: 查询券剩余库存
     * {@code @date}: 2026/02/13
     * {@code @author}: Michael
     */
    @Override
    public Long getCouponStock(Long templateId) {
        String stockKey = CORE_COUPON_STOCK_KEY_PREFIX + templateId;
        if (!redisUtil.exists(stockKey)) {
            return 0L;
        }
        return redisUtil.getAtomicLong(stockKey);
    }

    /**
     * {@code @description}: 查询可供抢券的生效模板列表
     * {@code @date}: 2026/02/16
     * {@code @author}: Michael Li
     */
    @Override
    public List<PointsCouponTemplateResponse> listActiveTemplates() {
        List<PointsCouponTemplate> templates = pointsCouponTemplateMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(POINTS_COUPON_TEMPLATE.SOURCE_TYPE.eq(SourceTypeOfCouponTemplate.ACTIVITY))
                        .and(POINTS_COUPON_TEMPLATE.STATUS.eq(1))
                        .orderBy(POINTS_COUPON_TEMPLATE.CREATED_AT.desc())
        );

        return templates.stream().map(template -> {
            Long stock = getCouponStock(template.getId());
            return PointsCouponTemplateResponse.builder()
                    .id(template.getId())
                    .name(template.getName())
                    .faceValue(template.getFaceValue())
                    .stock(stock)
                    .perUserLimit(template.getPerUserLimit())
                    .validDesc("领取后" + template.getValidDays() + "天有效")
                    .build();
        }).toList();
    }
}
