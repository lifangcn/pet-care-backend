package pvt.mktech.petcare.points.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.points.entity.PointsCoupon;
import pvt.mktech.petcare.points.entity.PointsCouponTemplate;
import pvt.mktech.petcare.points.entity.codelist.SourceTypeOfCouponTemplate;
import pvt.mktech.petcare.points.entity.codelist.StatusOfPointsCoupon;
import pvt.mktech.petcare.points.event.CouponRedeemEvent;
import pvt.mktech.petcare.points.mapper.PointsCouponMapper;
import pvt.mktech.petcare.points.mapper.PointsCouponTemplateMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointsCouponServiceImplTest {

    private final RedisUtil redisUtil = mock(RedisUtil.class);
    private final PointsCouponTemplateMapper templateMapper = mock(PointsCouponTemplateMapper.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final PointsCouponMapper couponMapper = mock(PointsCouponMapper.class);

    private final PointsCouponServiceImpl service = new PointsCouponServiceImpl(redisUtil, templateMapper, publisher);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mapper", couponMapper);
    }

    @Test
    void issueCouponForNewComerReturnsTrueWhenTemplateExistsAndIssueAndRedeemSucceed() {
        PointsCouponTemplate template = newComerTemplate();
        when(templateMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(template));
        when(templateMapper.selectOneById(1L)).thenReturn(template);
        when(couponMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(0L);
        when(couponMapper.insert(any(PointsCoupon.class), anyBoolean())).thenAnswer(inv -> {
            PointsCoupon coupon = inv.getArgument(0);
            coupon.setId(1000L);
            return 1;
        });
        when(templateMapper.update(any(PointsCouponTemplate.class), anyBoolean())).thenReturn(1);
        when(couponMapper.selectOneById(1000L)).thenReturn(issuedCoupon(1000L, 7L, 100));
        when(couponMapper.update(any(PointsCoupon.class), anyBoolean())).thenReturn(1);

        boolean result = service.issueCouponForNewComer(7L);

        assertThat(result).isTrue();
        ArgumentCaptor<CouponRedeemEvent> eventCaptor = ArgumentCaptor.forClass(CouponRedeemEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        CouponRedeemEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(7L);
        assertThat(event.getCouponId()).isEqualTo(1000L);
    }

    @Test
    void issueCouponForNewComerReturnsFalseWhenNoActiveTemplate() {
        when(templateMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of());

        boolean result = service.issueCouponForNewComer(7L);

        assertThat(result).isFalse();
        verify(couponMapper, never()).insert(any(), anyBoolean());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void issueCouponForNewComerPropagatesExceptionWhenIssueFails() {
        PointsCouponTemplate template = newComerTemplate();
        template.setPerUserLimit(1);
        when(templateMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(template));
        when(templateMapper.selectOneById(1L)).thenReturn(template);
        when(couponMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.issueCouponForNewComer(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("领取上限");

        verify(publisher, never()).publishEvent(any());
    }

    private PointsCouponTemplate newComerTemplate() {
        PointsCouponTemplate template = new PointsCouponTemplate();
        template.setId(1L);
        template.setName("新人注册券");
        template.setFaceValue(100);
        template.setValidDays(30);
        template.setTotalCount(0);
        template.setIssuedCount(0);
        template.setPerUserLimit(10);
        template.setSourceType(SourceTypeOfCouponTemplate.NEWCOMER);
        template.setStatus(1);
        return template;
    }

    private PointsCoupon issuedCoupon(Long couponId, Long userId, Integer faceValue) {
        PointsCoupon coupon = new PointsCoupon();
        coupon.setId(couponId);
        coupon.setUserId(userId);
        coupon.setTemplateId(1L);
        coupon.setFaceValue(faceValue);
        coupon.setStatus(StatusOfPointsCoupon.UNUSED);
        coupon.setStartTime(LocalDateTime.now());
        coupon.setEndTime(LocalDateTime.now().plusDays(30));
        return coupon;
    }
}
