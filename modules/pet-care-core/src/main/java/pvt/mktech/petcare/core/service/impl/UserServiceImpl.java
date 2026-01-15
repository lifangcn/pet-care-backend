package pvt.mktech.petcare.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.common.storage.OssTemplate;
import pvt.mktech.petcare.core.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.core.dto.response.CheckinStatsResponse;
import pvt.mktech.petcare.core.dto.response.UserResponse;
import pvt.mktech.petcare.core.entity.User;
import pvt.mktech.petcare.core.mapper.UserMapper;
import pvt.mktech.petcare.core.service.UserService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import static pvt.mktech.petcare.core.constant.CoreConstant.CORE_USER_CHECKIN_KEY;
import static pvt.mktech.petcare.core.entity.table.UserTableDef.USER;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final OssTemplate ossTemplate;
    private final ThreadPoolExecutor coreThreadPool;
    private final RedisUtil redisUtil;

    @Override
    public UserResponse getUserById(Long userId) {
        User user = getOne(USER.ID.eq(userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }
    
    @Override
    public UserResponse getUserByUsername(String username) {
        User user = getOne(USER.USERNAME.eq(username));


        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }
    
    @Transactional
    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = getOne(USER.ID.eq(userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查手机号是否已被其他用户使用
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .where(USER.PHONE.eq(request.getPhone()))
                    .and(USER.ID.eq(userId));
            if (userMapper.selectCountByQuery(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(request.getPhone());
        }

        // 删除旧头像（如果存在）
        if (StrUtil.isNotBlank(user.getAvatar())) {
            coreThreadPool.submit(() -> {
                try {
                    ossTemplate.deleteFile(user.getAvatar());
                } catch (Exception e) {
                    // 不阻断主流程
                    log.warn("删除旧头像失败: {}", user.getAvatar(), e);
                }
            });
        }
        // 更新基本信息
        Optional.ofNullable(request.getNickname()).ifPresent(user::setNickname);
        Optional.ofNullable(request.getAvatar()).ifPresent(user::setAvatar);
        Optional.ofNullable(request.getAddress()).ifPresent(user::setAddress);
        userMapper.update(user);
        // 发送用户更新事件到消息队列
//        userEventProducer.sendUserUpdateEvent(user);
        log.info("用户信息已更新: userId={}", userId);
        return convertToResponse(user);
    }
    
    @Override
    public boolean checkUsernameExists(String username) {
        return exists(USER.USERNAME.eq(username));
    }
    
    @Override
    public boolean checkPhoneExists(String phone) {
        return exists(USER.PHONE.eq(phone));
    }

    @Override
    public List<Long> getActiveUserIds() {
        QueryWrapper queryWrapper = QueryWrapper.create().select(USER.ID).where(USER.STATUS.eq(true)).from(USER);
        return userMapper.selectObjectListByQueryAs(queryWrapper, Long.class);
    }

    @Override
    public Boolean checkin(Long userId) {
        String key = String.format("%s%d:%d%02d",
                CORE_USER_CHECKIN_KEY, userId, DateUtil.thisYear(), DateUtil.thisMonth() + 1);

        boolean exists = redisUtil.getBitOffset(key, DateUtil.thisDayOfMonth() - 1);
        if (exists) {
            throw new BusinessException(ErrorCode.USER_ALREADY_CHECKIN);
        }

        // 1号对应索引下标0，2号对应索引下标1， 3号对应索引下标2，以此类推
        boolean flag = redisUtil.setBit(key, DateUtil.thisDayOfMonth() - 1, true);
        redisUtil.expire(key, Duration.ofDays(366));
        return flag;
    }

    @Override
    public CheckinStatsResponse getCheckinStats(Long userId, Long year, Long month) {
        CheckinStatsResponse checkinStatsResponse = new CheckinStatsResponse();
        String key = String.format("%s%d:%d%02d",
                CORE_USER_CHECKIN_KEY, userId, year, month);
        RBitSet bitSet = redisUtil.getBitSet(key);
        // 没有记录，返回空对象
        if (bitSet == null || bitSet.length() == 0) {
            return checkinStatsResponse;
        }
        // 1.获取本月打卡次数
        long bitCount = bitSet.cardinality();
        checkinStatsResponse.setMonthCheckinCount((int) bitCount);
        // 2.向前计算连续打卡时间和上次打卡时间
        int todayIndex = DateUtil.thisDayOfMonth() - 1;
        Integer continuousDays = 0;
        for (int i = todayIndex; i >= 0; i--) {
            if (bitSet.get(i)) {
                continuousDays++;
                continue;
            }
            // 如果今天没有打卡，继续循环不跳出
            if (i == todayIndex) {
                continue;
            }
            break;
        }
        checkinStatsResponse.setContinuousDays(continuousDays);
        // 3.获取最后一次打卡时间
        for (int i = todayIndex; i >= 0; i--) {
            if (bitSet.get(i)) {
                // 本月没有打卡，则不做处理
                checkinStatsResponse.setLastCheckinDate(String.format("%d-%02d-%02d", year, month, i + 1));
                break;
            }
        }

        return checkinStatsResponse;
    }

    private int calculateContinuousDays(RBitSet bitSet) {
        int continuousDays = 0;
        int todayIndex = DateUtil.thisDayOfMonth() - 1;

        // 从今天往前计算连续打卡天数
        for (int i = todayIndex; i >= 0; i--) {
            if (bitSet.get(i)) {
                continuousDays++;
            } else {
                break;
            }
        }
        return continuousDays;
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtil.copyProperties(user, response);
        return response;
    }
}