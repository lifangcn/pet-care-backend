package pvt.mktech.petcare.points.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.redis.DistributedLock;
import pvt.mktech.petcare.points.dto.request.PointsConsumeRequest;
import pvt.mktech.petcare.points.dto.request.PointsRecordQueryRequest;
import pvt.mktech.petcare.points.dto.response.PointsAccountResponse;
import pvt.mktech.petcare.points.entity.PointsAccount;
import pvt.mktech.petcare.points.entity.PointsRecord;
import pvt.mktech.petcare.points.entity.codelist.PointsActionType;
import pvt.mktech.petcare.points.mapper.PointsAccountMapper;
import pvt.mktech.petcare.points.mapper.PointsRecordMapper;
import pvt.mktech.petcare.points.service.PointsCacheService;
import pvt.mktech.petcare.points.service.PointsService;

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

    private static final String LOCK_KEY_PREFIX = "points:lock:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(lockKey = LOCK_KEY_PREFIX + "#userId")
    public void grantRegisterPoints(Long userId) {
        PointsAccount account = getOrCreateAccount(userId);
        account.setTotalPoints(account.getTotalPoints() + PointsActionType.REGISTER.getPoints());
        account.setAvailablePoints(account.getAvailablePoints() + PointsActionType.REGISTER.getPoints());
        updateById(account);
        saveRecord(account, PointsActionType.REGISTER.getPoints(), PointsActionType.REGISTER, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(lockKey = LOCK_KEY_PREFIX + "#userId")
    public Integer earnByAction(Long userId, PointsActionType action, Long bizId) {
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

        PointsAccount account = getOrCreateAccount(userId);
        Integer points = action.getPoints();
        account.setTotalPoints(account.getTotalPoints() + points);
        account.setAvailablePoints(account.getAvailablePoints() + points);
        updateById(account);
        saveRecord(account, points, action, bizId);

        // 增加行为计数
        pointsCacheService.incrementActionCount(userId, action);
        return points;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(lockKey = LOCK_KEY_PREFIX + "#authorId")
    public Integer earnByQuality(Long authorId, PointsActionType action, Long contentId, Long interactUserId) {
        // 检查是否首次互动
        boolean isFirst = pointsCacheService.checkAndAddInteraction(contentId, interactUserId, action);
        if (!isFirst) {
            log.info("用户{}已与内容{}互动过", interactUserId, contentId);
            return 0;
        }

        PointsAccount account = getOrCreateAccount(authorId);
        Integer points = action.getPoints();
        account.setTotalPoints(account.getTotalPoints() + points);
        account.setAvailablePoints(account.getAvailablePoints() + points);
        updateById(account);
        saveRecord(account, points, action, contentId);
        return points;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(lockKey = LOCK_KEY_PREFIX + "#request.userId")
    public boolean consume(PointsConsumeRequest request) {
        PointsAccount account = getOrCreateAccount(request.getUserId());

        if (account.getAvailablePoints() < Math.abs(request.getPoints())) {
            throw new BusinessException(ErrorCode.POINTS_NOT_ENOUGH);
        }

        account.setAvailablePoints(account.getAvailablePoints() + request.getPoints());
        updateById(account);

        PointsRecord record = new PointsRecord();
        record.setUserId(request.getUserId());
        record.setPoints(request.getPoints());
        record.setPointsBefore(account.getAvailablePoints() - request.getPoints());
        record.setPointsAfter(account.getAvailablePoints());
        record.setActionType(request.getActionType());
        record.setBizType(request.getBizType());
        record.setBizId(request.getBizId());
        record.setCouponId(request.getCouponId());
        pointsRecordMapper.insert(record);
        return true;
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

    /**
     * 保存积分流水记录
     */
    private void saveRecord(PointsAccount account, Integer points, PointsActionType action, Long bizId) {
        PointsRecord record = new PointsRecord();
        record.setUserId(account.getUserId());
        record.setPoints(points);
        record.setPointsBefore(account.getAvailablePoints() - points);
        record.setPointsAfter(account.getAvailablePoints());
        record.setActionType(action.getCode());
        record.setBizId(bizId);
        pointsRecordMapper.insert(record);
    }

    /**
     * 获取每日行为上限
     */
    private int getDailyLimit(PointsActionType action) {
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
