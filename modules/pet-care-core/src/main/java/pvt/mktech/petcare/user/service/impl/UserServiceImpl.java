package pvt.mktech.petcare.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
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
import pvt.mktech.petcare.user.dto.response.AdminUserResponse;
import pvt.mktech.petcare.user.dto.response.UserResponse;
import pvt.mktech.petcare.user.entity.Role;
import pvt.mktech.petcare.user.entity.User;
import pvt.mktech.petcare.user.entity.UserRole;
import pvt.mktech.petcare.user.mapper.RoleMapper;
import pvt.mktech.petcare.user.mapper.UserMapper;
import pvt.mktech.petcare.user.mapper.UserRoleMapper;
import pvt.mktech.petcare.user.service.UserService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_USER_CHECK_IN_KEY;
import static pvt.mktech.petcare.user.entity.table.RoleTableDef.ROLE;
import static pvt.mktech.petcare.user.entity.table.UserRoleTableDef.USER_ROLE;
import static pvt.mktech.petcare.user.entity.table.UserTableDef.USER;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
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

    /**
     * 分页查询后台用户列表
     *
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @return 用户分页结果
     * @author Michael Li
     * @since 2026-03-27
     */
    @Override
    public Page<AdminUserResponse> pageAdminUsers(Long pageNumber, Long pageSize) {
        // 1. 分页查询用户
        Page<User> userPage = page(Page.of(pageNumber, pageSize), QueryWrapper.create().orderBy(USER.CREATED_AT.desc()));

        // 2. 查询 admin 角色
        Role adminRole = roleMapper.selectOneByQuery(QueryWrapper.create().where(ROLE.ROLE_CODE.eq("admin")));
        Long adminRoleId = adminRole != null ? adminRole.getId() : null;

        // 3. 获取用户ID列表，查询对应的用户角色关联
        List<Long> userIds = userPage.getRecords().stream().map(User::getId).toList();
        List<UserRole> userRoles = new ArrayList<>();
        if (adminRoleId != null && !userIds.isEmpty()) {
            userRoles = userRoleMapper.selectListByQuery(QueryWrapper.create()
                    .where(USER_ROLE.USER_ID.in(userIds))
                    .and(USER_ROLE.ROLE_ID.eq(adminRoleId)));
        }

        // 4. 构建管理员用户ID集合
        java.util.Set<Long> adminUserIds = userRoles.stream().map(UserRole::getUserId).collect(java.util.stream.Collectors.toSet());

        // 5. 映射为 AdminUserResponse
        List<AdminUserResponse> responses = userPage.getRecords().stream().map(user -> {
            AdminUserResponse response = new AdminUserResponse();
            BeanUtil.copyProperties(user, response);
            response.setIsAdmin(adminUserIds.contains(user.getId()));
            return response;
        }).toList();

        // 6. 构建返回的 Page 对象
        Page<AdminUserResponse> resultPage = new Page<>(userPage.getPageNumber(), userPage.getPageSize(), userPage.getTotalRow());
        resultPage.setRecords(responses);
        return resultPage;
    }

    /**
     * 更新用户管理员身份
     *
     * @param userId 用户ID
     * @param isAdmin 是否授予管理员
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAdminRole(Long userId, Boolean isAdmin) {
        // 1. 查询 admin 角色
        Role adminRole = roleMapper.selectOneByQuery(QueryWrapper.create().where(ROLE.ROLE_CODE.eq("admin")));
        if (adminRole == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        Long adminRoleId = adminRole.getId();

        if (isAdmin) {
            // 2. 授予管理员：检查是否已存在，避免重复插入
            long count = userRoleMapper.selectCountByQuery(QueryWrapper.create()
                    .where(USER_ROLE.USER_ID.eq(userId))
                    .and(USER_ROLE.ROLE_ID.eq(adminRoleId)));
            if (count == 0) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(adminRoleId);
                userRoleMapper.insert(userRole);
            }
        } else {
            // 3. 移除管理员：删除对应的 user_role
            userRoleMapper.deleteByQuery(QueryWrapper.create()
                    .where(USER_ROLE.USER_ID.eq(userId))
                    .and(USER_ROLE.ROLE_ID.eq(adminRoleId)));
        }

        return true;
    }

    /**
     * 更新用户启用状态
     *
     * @param userId 用户ID
     * @param enabled 启用状态
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEnabledStatus(Long userId, Integer enabled) {
        // 1. 查询用户是否存在
        User user = getOne(USER.ID.eq(userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 更新启用状态
        user.setEnabled(enabled);
        userMapper.update(user);

        return true;
    }

    /**
     * 根据用户ID获取管理员用户信息
     *
     * @param userId 用户ID
     * @return 管理员用户响应对象
     * @author Michael Li
     * @since 2026-03-28
     */
    @Override
    public AdminUserResponse getAdminUserById(Long userId) {
        User user = getOne(USER.ID.eq(userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 查询 admin 角色
        Role adminRole = roleMapper.selectOneByQuery(QueryWrapper.create().where(ROLE.ROLE_CODE.eq("admin")));
        Long adminRoleId = adminRole != null ? adminRole.getId() : null;

        // 判断是否是管理员
        boolean isAdmin = false;
        if (adminRoleId != null) {
            long count = userRoleMapper.selectCountByQuery(QueryWrapper.create()
                    .where(USER_ROLE.USER_ID.eq(userId))
                    .and(USER_ROLE.ROLE_ID.eq(adminRoleId)));
            isAdmin = count > 0;
        }

        AdminUserResponse response = new AdminUserResponse();
        BeanUtil.copyProperties(user, response);
        response.setIsAdmin(isAdmin);
        return response;
    }
}