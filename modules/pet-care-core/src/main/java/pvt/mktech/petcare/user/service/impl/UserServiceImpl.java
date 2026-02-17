package pvt.mktech.petcare.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.common.storage.OssTemplate;
import pvt.mktech.petcare.points.service.PointsService;
import pvt.mktech.petcare.user.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.shared.dto.CheckInStatsResponse;
import pvt.mktech.petcare.user.dto.response.UserResponse;
import pvt.mktech.petcare.user.entity.User;
import pvt.mktech.petcare.user.mapper.UserMapper;
import pvt.mktech.petcare.user.service.UserService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_USER_CHECK_IN_KEY;
import static pvt.mktech.petcare.user.entity.table.UserTableDef.USER;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private OssTemplate ossTemplate;
    @Resource
    private ThreadPoolExecutor coreThreadPoolExecutor;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private PointsService pointsService;

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
            coreThreadPoolExecutor.submit(() -> {
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
        QueryWrapper queryWrapper = QueryWrapper.create().select(USER.ID).where(USER.ENABLED.eq(true)).from(USER);
        return userMapper.selectObjectListByQueryAs(queryWrapper, Long.class);
    }

    @Override
    public Boolean checkIn(Long userId) {
        String key = String.format("%s%d:%d%02d",
                CORE_USER_CHECK_IN_KEY, userId, DateUtil.thisYear(), DateUtil.thisMonth() + 1);
        int offset = DateUtil.thisDayOfMonth() - 1;
        // 校验今天是否已经打卡
        if (redisUtil.getBit(key, offset)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_CHECKIN);
        }
        // 1号对应索引下标0，2号对应索引下标1， 3号对应索引下标2，以此类推
        boolean flag = redisUtil.setBit(key, offset, true);
        redisUtil.expire(key, Duration.ofDays(366));

        return flag;
    }

    @Override
    public CheckInStatsResponse getCheckInStats(Long userId, Long year, Long month) {
        CheckInStatsResponse checkInStatsResponse = new CheckInStatsResponse();
        String key = String.format("%s%d:%d%02d",
                CORE_USER_CHECK_IN_KEY, userId, year, month);

        RBitSet bitSet = redisUtil.getBitSet(key);
        if (!bitSet.isExists()) {
            return checkInStatsResponse;
        }

        // 获取BitMap数据并转为二进制字符串（如"1000000100001000"）
        byte[] data = bitSet.toByteArray();
        StringBuilder binaryStr = new StringBuilder();
        for (byte b : data) {
            binaryStr.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        String bits = binaryStr.toString();

        int todayIndex = DateUtil.thisDayOfMonth();
        // 二进制字符串索引从0开始，日期从1开始，需-1
        int maxIndex = Math.min(todayIndex, bits.length()) - 1;

        // 1.获取本月签到次数和签到记录
        int monthCheckInCount = 0;
        if (checkInStatsResponse.getCheckInDates() == null) {
            checkInStatsResponse.setCheckInDates(new ArrayList<>());
        }
        for (int i = 0; i <= maxIndex; i++) {
            if (bits.charAt(i) == '1') {
                monthCheckInCount++;
                checkInStatsResponse.getCheckInDates().add(String.format("%d-%02d-%02d", year, month, i + 1));
            }
        }
        checkInStatsResponse.setMonthCheckInCount(monthCheckInCount);

        // 2.向前计算连续签到天数
        Integer continuousDays = 0;
        for (int i = maxIndex; i >= 0; i--) {
            if (bits.charAt(i) == '1') {
                continuousDays++;
                continue;
            }
            if (i == maxIndex) {
                continue;
            }
            break;
        }
        checkInStatsResponse.setContinuousDays(continuousDays);

        // 3.获取最后一次签到时间
        for (int i = maxIndex; i >= 0; i--) {
            if (bits.charAt(i) == '1') {
                checkInStatsResponse.setLastCheckInDate(String.format("%d-%02d-%02d", year, month, i + 1));
                break;
            }
        }

        return checkInStatsResponse;
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtil.copyProperties(user, response);
        return response;
    }
}