package pvt.mktech.petcare.points.service.impl;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.redis.RedissonLockUtil;
import pvt.mktech.petcare.points.dto.request.PointsConsumeRequest;
import pvt.mktech.petcare.points.dto.request.PointsRecordQueryRequest;
import pvt.mktech.petcare.points.dto.response.PointsAccountResponse;
import pvt.mktech.petcare.points.entity.PointsAccount;
import pvt.mktech.petcare.points.entity.PointsCoupon;
import pvt.mktech.petcare.points.entity.PointsRecord;
import pvt.mktech.petcare.points.entity.codelist.ActionTypeOfPointsRecord;
import pvt.mktech.petcare.points.mapper.PointsAccountMapper;
import pvt.mktech.petcare.points.mapper.PointsCouponMapper;
import pvt.mktech.petcare.points.mapper.PointsRecordMapper;
import pvt.mktech.petcare.points.service.PointsCacheService;
import pvt.mktech.petcare.points.service.PointsCouponService;
import pvt.mktech.petcare.points.service.PointsService;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_POINTS_LOCK_KEY_PREFIX;
import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_POINTS_RECORD_SAVE_TOPIC;
import static pvt.mktech.petcare.points.entity.table.PointsAccountTableDef.POINTS_ACCOUNT;
import static pvt.mktech.petcare.points.entity.table.PointsRecordTableDef.POINTS_RECORD;

/**
 * {@code @description}: 积分核心服务实现
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl extends ServiceImpl<PointsAccountMapper, PointsAccount> implements PointsService {

    private final PointsRecordMapper pointsRecordMapper;
    private final PointsCacheService pointsCacheService;
    private final RedissonLockUtil redissonLockUtil;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final PointsCouponService pointsCouponService;
    private final PointsCouponMapper pointsCouponMapper;

    @Override
    public void grantRegisterPoints(Long userId) {
        PointsRecord pointsRecord = transactionTemplate.execute(status ->
                redissonLockUtil.executeWithLock(CORE_POINTS_LOCK_KEY_PREFIX + userId, () -> {
                    PointsAccount account = getOrCreateAccount(userId);
                    int points = ActionTypeOfPointsRecord.REGISTER.getPoints();

                    updateChain().set(POINTS_ACCOUNT.AVAILABLE_POINTS, account.getAvailablePoints() + points)
                            .set(POINTS_ACCOUNT.TOTAL_POINTS, account.getTotalPoints() + points)
                            .where(POINTS_ACCOUNT.USER_ID.eq(userId))
                            .update();

                    PointsRecord record = new PointsRecord();
                    record.setUserId(account.getUserId());
                    record.setPoints(points);
                    record.setPointsBefore(account.getAvailablePoints());
                    record.setPointsAfter(account.getAvailablePoints() + points);
                    record.setActionType(ActionTypeOfPointsRecord.REGISTER);
                    record.setBizId(null);
                    return record;
                }, 1, 10));
        if (pointsRecord != null) {
            sendMessageToPointsRecordSave(pointsRecord);
        }
        // 发放新人劵
        pointsCouponService.issueCouponForNewComer(userId);

    }

    @Override
    public Integer earnByAction(Long userId, ActionTypeOfPointsRecord action, Long bizId) {
        // 检查是否超限
        int count = pointsCacheService.getActionCount(userId, action);
        int limit = getDailyLimit(action);
        if (count >= limit) {
            log.info("用户{}今日行为{}已达上限", userId, action);
            return 0;
        }

        // 二次检查（防止并发情况）
        int currentCount = pointsCacheService.getActionCount(userId, action);
        if (currentCount >= limit) {
            return 0;
        }

        PointsRecord pointsRecord = transactionTemplate.execute(status ->
                redissonLockUtil.executeWithLock(CORE_POINTS_LOCK_KEY_PREFIX + userId, () -> {
                    PointsAccount account = getOrCreateAccount(userId);
                    Integer points = action.getPoints();

                    updateChain()
                            .set(POINTS_ACCOUNT.AVAILABLE_POINTS, account.getAvailablePoints() + points)
                            .set(POINTS_ACCOUNT.TOTAL_POINTS, account.getTotalPoints() + points)
                            .where(POINTS_ACCOUNT.USER_ID.eq(userId))
                            .update();

                    PointsRecord record = new PointsRecord();
                    record.setUserId(account.getUserId());
                    record.setPoints(points);
                    record.setPointsBefore(account.getAvailablePoints());
                    record.setPointsAfter(account.getAvailablePoints() + points);
                    record.setActionType(action);
                    record.setBizId(bizId);
                    return record;
                }, 1, 10));

        // 增加行为计数
        pointsCacheService.incrementActionCount(userId, action);

        if (pointsRecord != null) {
            sendMessageToPointsRecordSave(pointsRecord);
        }
        return action.getPoints();
    }

    /**
     * {@code @description}: 券兑换获取积分（锁优化）
     * {@code @date}: 2025-02-21
     * {@code @author}: Michael Li
     */
    @Override
    public Integer earnByCoupon(Long userId, Long couponId) {
        // 锁外查询券表，减少锁持有时间
        PointsCoupon coupon = pointsCouponMapper.selectOneById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券不存在");
        }

        Integer points = coupon.getFaceValue();

        PointsRecord pointsRecord = transactionTemplate.execute(status ->
                redissonLockUtil.executeWithLock(CORE_POINTS_LOCK_KEY_PREFIX + userId, () -> {
                    PointsAccount account = getOrCreateAccount(userId);

                    updateChain()
                            .set(POINTS_ACCOUNT.AVAILABLE_POINTS, account.getAvailablePoints() + points)
                            .set(POINTS_ACCOUNT.TOTAL_POINTS, account.getTotalPoints() + points)
                            .where(POINTS_ACCOUNT.USER_ID.eq(userId))
                            .update();

                    PointsRecord record = new PointsRecord();
                    record.setUserId(account.getUserId());
                    record.setPoints(points);
                    record.setPointsBefore(account.getAvailablePoints());
                    record.setPointsAfter(account.getAvailablePoints() + points);
                    record.setActionType(ActionTypeOfPointsRecord.COUPON_REDEEM);
                    record.setBizId(couponId);
                    return record;
                }, 1, 10));

        if (pointsRecord != null) {
            sendMessageToPointsRecordSave(pointsRecord);
        }
        return pointsRecord != null ? pointsRecord.getPoints() : 0;
    }

    @Override
    public Integer earnByQuality(Long authorId, ActionTypeOfPointsRecord action, Long contentId, Long interactUserId) {
        // 检查是否首次互动
        boolean isFirst = pointsCacheService.checkAndAddInteraction(contentId, interactUserId, action);
        if (!isFirst) {
            log.info("用户{}已与内容{}互动过", interactUserId, contentId);
            return 0;
        }

        PointsRecord pointsRecord = transactionTemplate.execute(status ->
                redissonLockUtil.executeWithLock(CORE_POINTS_LOCK_KEY_PREFIX + authorId, () -> {
                    PointsAccount account = getOrCreateAccount(authorId);
                    Integer points = action.getPoints();

                    updateChain()
                            .set(POINTS_ACCOUNT.AVAILABLE_POINTS, account.getAvailablePoints() + points)
                            .set(POINTS_ACCOUNT.TOTAL_POINTS, account.getTotalPoints() + points)
                            .where(POINTS_ACCOUNT.USER_ID.eq(authorId))
                            .update();

                    PointsRecord record = new PointsRecord();
                    record.setUserId(account.getUserId());
                    record.setPoints(points);
                    record.setPointsBefore(account.getAvailablePoints());
                    record.setPointsAfter(account.getAvailablePoints() + points);
                    record.setActionType(action);
                    record.setBizId(contentId);
                    return record;
                }, 1, 10));

        if (pointsRecord != null) {
            sendMessageToPointsRecordSave(pointsRecord);
        }
        return action.getPoints();
    }

    /**
     * {@code @description}: 消费积分
     * 使用数据库层面原子更新，避免并发丢失更新
     * {@code @date}: 2026/02/11
     * {@code @author}: Michael Li
     */
    @Override
    public boolean consume(PointsConsumeRequest request) {
        PointsRecord pointsRecord = transactionTemplate.execute(status -> {
            Integer pointsToConsume = Math.abs(request.getPoints());

            // 数据库层面原子更新 + 余额检查一次完成
            boolean updated = UpdateChain.of(PointsAccount.class)
                    .set(POINTS_ACCOUNT.AVAILABLE_POINTS,
                            POINTS_ACCOUNT.AVAILABLE_POINTS.subtract(pointsToConsume))
                    .set(POINTS_ACCOUNT.TOTAL_POINTS,
                            POINTS_ACCOUNT.TOTAL_POINTS.subtract(pointsToConsume))
                    .where(POINTS_ACCOUNT.USER_ID.eq(request.getUserId()))
                    .and(POINTS_ACCOUNT.AVAILABLE_POINTS.ge(pointsToConsume))
                    .update();

            // updated = false 说明余额不足（WHERE条件不满足）
            if (!updated) {
                throw new BusinessException(ErrorCode.POINTS_NOT_ENOUGH);
            }

            // 查询扣减后的余额（获取实际更新后的值）
            PointsAccount accountAfter = getOne(QueryWrapper.create().where(POINTS_ACCOUNT.USER_ID.eq(request.getUserId())));

            Integer pointsAfter = accountAfter.getAvailablePoints();
            // 创建积分记录对象
            PointsRecord toSave = new PointsRecord();
            toSave.setUserId(request.getUserId());
            toSave.setPoints(request.getPoints());
            toSave.setPointsBefore(pointsAfter - request.getPoints());
            toSave.setPointsAfter(pointsAfter);
            toSave.setActionType(ActionTypeOfPointsRecord.fromCode(request.getActionType()).get());
            toSave.setBizType(request.getBizType());
            toSave.setBizId(request.getBizId());
            toSave.setCouponId(request.getCouponId());
            return toSave;
        });
        if (pointsRecord != null) {
            sendMessageToPointsRecordSave(pointsRecord);
        }
        return true;
    }

    /**
     * {@code @description}: 发送积分记录流水处理队列（异步优化）
     * {@code @date}: 2025-02-21
     * {@code @author}: Michael Li
     */
    private void sendMessageToPointsRecordSave(PointsRecord record) {
        String key = record.getBizId() != null ? record.getBizId().toString() : "0";
        String value = JSONUtil.toJsonStr(record);

        // 异步发送，不阻塞主流程
        kafkaTemplate.send(CORE_POINTS_RECORD_SAVE_TOPIC, key, value)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("发送 积分记录流水处理队列 成功，topic: {}, key: {}", CORE_POINTS_RECORD_SAVE_TOPIC, key);
                    } else {
                        log.error("发送 积分记录流水处理队列 失败，topic: {}, key: {}", CORE_POINTS_RECORD_SAVE_TOPIC, key, ex);
                    }
                });
    }


    @Override
    public PointsAccountResponse getAccount(Long userId) {
        PointsAccount account = getOrCreateAccount(userId);
        PointsAccountResponse response = new PointsAccountResponse();
        response.setAvailablePoints(account.getAvailablePoints());
        response.setTotalPoints(account.getTotalPoints());
        response.setLevel(calculateLevel(account.getTotalPoints()));
        return response;
    }

    @Override
    public Page<PointsRecord> pageRecords(Long userId, Long pageNumber, Long pageSize, PointsRecordQueryRequest request) {
        QueryWrapper queryWrapper = queryChain()
                .select(POINTS_RECORD.ALL_COLUMNS)
                .where(POINTS_RECORD.USER_ID.eq(userId));

        if (request.getActionType() != null) {
            queryWrapper.and(POINTS_RECORD.ACTION_TYPE.eq(request.getActionType()));
        }
        if (request.getStartTime() != null) {
            queryWrapper.and(POINTS_RECORD.CREATED_AT.ge(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            queryWrapper.and(POINTS_RECORD.CREATED_AT.le(request.getEndTime()));
        }

        // 按创建时间倒序
        queryWrapper.orderBy(POINTS_RECORD.CREATED_AT.desc());

        return pointsRecordMapper.paginate(Page.of(pageNumber, pageSize), queryWrapper);
    }

    /**
     * 获取或创建积分账户
     */
    private PointsAccount getOrCreateAccount(Long userId) {
        PointsAccount account = getOne(QueryWrapper.create()
                .where(POINTS_ACCOUNT.USER_ID.eq(userId)));
        if (account == null) {
            account = new PointsAccount();
            account.setUserId(userId);
            account.setAvailablePoints(0);
            account.setTotalPoints(0);
            save(account);
        }
        return account;
    }

    @Override
    public void saveRecord(PointsAccount account, Integer points, ActionTypeOfPointsRecord action, Long bizId) {
        PointsRecord record = new PointsRecord();
        record.setUserId(account.getUserId());
        record.setPoints(points);
        record.setPointsBefore(account.getAvailablePoints() - points);
        record.setPointsAfter(account.getAvailablePoints());
        record.setActionType(action);
        record.setBizId(bizId);
        pointsRecordMapper.insert(record);
    }

    /**
     * 获取每日行为上限
     */
    private int getDailyLimit(ActionTypeOfPointsRecord action) {
        return switch (action) {
            case CHECK_IN -> 1;
            case PUBLISH -> 5;
            case COMMENT -> 20;
            case LIKE -> 50;
            default -> Integer.MAX_VALUE;
        };
    }

    /**
     * 根据累计积分计算等级
     */
    private Integer calculateLevel(Integer totalPoints) {
        if (totalPoints < 100) return 1;
        if (totalPoints < 500) return 2;
        if (totalPoints < 1500) return 3;
        if (totalPoints < 3500) return 4;
        if (totalPoints < 7000) return 5;
        if (totalPoints < 15000) return 6;
        if (totalPoints < 30000) return 7;
        if (totalPoints < 60000) return 8;
        if (totalPoints < 120000) return 9;
        return 10;
    }
}
