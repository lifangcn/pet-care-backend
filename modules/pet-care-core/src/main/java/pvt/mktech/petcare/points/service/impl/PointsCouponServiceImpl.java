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
import pvt.mktech.petcare.points.dto.request.PointsCouponTemplateRequest;
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
        PointsCouponTemplate template = pointsCouponTemplateMapper.selectOneById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券模板不存在或已停用");
        }
        if (template.getTotalCount() > 0 && template.getIssuedCount() >= template.getTotalCount()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券已发放完毕");
        }
        long userCount = count(QueryWrapper.create()
                .where(POINTS_COUPON.USER_ID.eq(userId))
                .and(POINTS_COUPON.TEMPLATE_ID.eq(templateId))
                .and(POINTS_COUPON.STATUS.eq(StatusOfPointsCoupon.UNUSED)));
        if (userCount >= template.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已达到领取上限");
        }
        PointsCoupon coupon = new PointsCoupon();
        coupon.setUserId(userId);
        coupon.setTemplateId(templateId);
        coupon.setFaceValue(template.getFaceValue());
        coupon.setStatus(StatusOfPointsCoupon.UNUSED);
        coupon.setStartTime(LocalDateTime.now());
        coupon.setEndTime(LocalDateTime.now().plusDays(template.getValidDays()));
        save(coupon);
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
        List<PointsCouponTemplate> newcomerTemplates = pointsCouponTemplateMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(POINTS_COUPON_TEMPLATE.SOURCE_TYPE.eq(SourceTypeOfCouponTemplate.NEWCOMER))
                        .and(POINTS_COUPON_TEMPLATE.STATUS.eq(1))
                        .limit(1)
        );
        if (!newcomerTemplates.isEmpty()) {
            PointsCouponTemplate template = newcomerTemplates.getFirst();
            Long couponId = issueCoupon(userId, template.getId());
            redeemCoupon(userId, couponId);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean redeemCoupon(Long userId, Long couponId) {
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
            coupon.setStatus(StatusOfPointsCoupon.EXPIRED);
            updateById(coupon);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代金券已过期");
        }
        coupon.setStatus(StatusOfPointsCoupon.USED);
        coupon.setUsedTime(LocalDateTime.now());
        updateById(coupon);
        applicationEventPublisher.publishEvent(new CouponRedeemEvent(userId, couponId));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long grabCoupon(Long userId, Long templateId) {
        PointsCouponTemplate template = pointsCouponTemplateMapper.selectOneById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券模板不存在或已停用");
        }
        String stockKey = CORE_COUPON_STOCK_KEY_PREFIX + templateId;
        Long remainingStock = redisUtil.decrement(stockKey, 1L);
        if (remainingStock < 0) {
            redisUtil.increment(stockKey, 1);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券已被抢完");
        }
        try {
            return issueCoupon(userId, templateId);
        } catch (Exception e) {
            redisUtil.increment(stockKey, 1);
            throw e;
        }
    }

    @Override
    public void setCouponStock(Long templateId, Integer stock) {
        String stockKey = CORE_COUPON_STOCK_KEY_PREFIX + templateId;
        redisUtil.setAtomicLong(stockKey, stock);
    }

    @Override
    public Long getCouponStock(Long templateId) {
        String stockKey = CORE_COUPON_STOCK_KEY_PREFIX + templateId;
        if (!redisUtil.exists(stockKey)) {
            return 0L;
        }
        return redisUtil.getAtomicLong(stockKey);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(PointsCouponTemplateRequest request) {
        PointsCouponTemplate template = new PointsCouponTemplate();
        template.setName(request.getName());
        template.setFaceValue(request.getFaceValue());
        template.setValidDays(request.getValidDays());
        template.setTotalCount(request.getTotalCount());
        template.setPerUserLimit(request.getPerUserLimit());
        template.setIssuedCount(0);
        Optional<SourceTypeOfCouponTemplate> sourceType = SourceTypeOfCouponTemplate.fromCode(request.getSourceType());
        if (sourceType.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "来源类型不合法");
        }
        template.setSourceType(sourceType.get());
        template.setStatus(request.getStatus());
        pointsCouponTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public Page<PointsCouponTemplate> pageTemplates(Long pageNumber, Long pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create().orderBy(POINTS_COUPON_TEMPLATE.CREATED_AT.desc());
        return pointsCouponTemplateMapper.paginate(Page.of(pageNumber, pageSize), queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(Long id, PointsCouponTemplateRequest request) {
        PointsCouponTemplate template = pointsCouponTemplateMapper.selectOneById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在");
        }
        template.setName(request.getName());
        template.setFaceValue(request.getFaceValue());
        template.setValidDays(request.getValidDays());
        template.setTotalCount(request.getTotalCount());
        template.setPerUserLimit(request.getPerUserLimit());
        Optional<SourceTypeOfCouponTemplate> sourceType = SourceTypeOfCouponTemplate.fromCode(request.getSourceType());
        if (sourceType.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "来源类型不合法");
        }
        template.setSourceType(sourceType.get());
        template.setStatus(request.getStatus());
        pointsCouponTemplateMapper.update(template);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean issueCoupons(Long templateId, List<Long> userIds) {
        PointsCouponTemplate template = pointsCouponTemplateMapper.selectOneById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券模板不存在或已停用");
        }
        int successCount = 0;
        for (Long userId : userIds) {
            try {
                issueCoupon(userId, templateId);
                successCount++;
            } catch (Exception e) {
                log.warn("发放券失败: userId={}", userId, e);
            }
        }
        log.info("批量发放券完成: templateId={}, successCount={}, totalCount={}", templateId, successCount, userIds.size());
        return true;
    }
}
