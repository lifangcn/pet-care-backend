package pvt.mktech.petcare.points.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.points.dto.request.PointsCouponQueryRequest;
import pvt.mktech.petcare.points.dto.response.PointsCouponResponse;
import pvt.mktech.petcare.points.entity.PointsCoupon;
import pvt.mktech.petcare.points.entity.PointsCouponTemplate;
import pvt.mktech.petcare.points.entity.codelist.StatusOfPointsCoupon;
import pvt.mktech.petcare.points.mapper.PointsCouponMapper;
import pvt.mktech.petcare.points.mapper.PointsCouponTemplateMapper;
import pvt.mktech.petcare.points.service.PointsCouponService;

import java.time.LocalDateTime;

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

    private final PointsCouponTemplateMapper templateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long issueCoupon(Long userId, Long templateId) {
        // 查询券模板
        PointsCouponTemplate template = templateMapper.selectOneById(templateId);
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
                .and(POINTS_COUPON.STATUS.eq(StatusOfPointsCoupon.UNUSED.getCode())));
        if (userCount >= template.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已达到领取上限");
        }

        // 创建代金券
        PointsCoupon coupon = new PointsCoupon();
        coupon.setUserId(userId);
        coupon.setTemplateId(templateId);
        coupon.setFaceValue(template.getFaceValue());
        coupon.setStatus(StatusOfPointsCoupon.UNUSED.getCode());
        coupon.setStartTime(LocalDateTime.now());
        coupon.setEndTime(LocalDateTime.now().plusDays(template.getValidDays()));
        save(coupon);

        // 更新已发放数量
        template.setIssuedCount(template.getIssuedCount() + 1);
        templateMapper.update(template);

        return coupon.getId();
    }

    @Override
    public Page<PointsCoupon> pageCoupons(Long userId, Long pageNumber, Long pageSize, PointsCouponQueryRequest request) {
        QueryWrapper queryWrapper = queryChain()
                .where(POINTS_COUPON.USER_ID.eq(userId));

        if (request.getStatus() != null) {
            queryWrapper.and(POINTS_COUPON.STATUS.eq(request.getStatus()));
        }

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

        if (!coupon.getStatus().equals(StatusOfPointsCoupon.UNUSED.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券已使用或已过期");
        }

        if (coupon.getEndTime().isBefore(LocalDateTime.now())) {
            // 标记为已过期
            coupon.setStatus(StatusOfPointsCoupon.EXPIRED.getCode());
            updateById(coupon);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券已过期");
        }

        coupon.setStatus(StatusOfPointsCoupon.USED.getCode());
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
                && coupon.getStatus().equals(StatusOfPointsCoupon.UNUSED.getCode())
                && coupon.getEndTime().isAfter(LocalDateTime.now());
    }
}
