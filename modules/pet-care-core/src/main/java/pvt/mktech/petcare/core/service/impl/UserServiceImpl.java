package pvt.mktech.petcare.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.storage.OssTemplate;
import pvt.mktech.petcare.core.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.core.dto.response.UserResponse;
import pvt.mktech.petcare.core.entity.User;
import pvt.mktech.petcare.core.mapper.UserMapper;
import pvt.mktech.petcare.core.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import static pvt.mktech.petcare.core.entity.table.UserTableDef.USER;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final OssTemplate ossTemplate;
    private final ThreadPoolExecutor coreThreadPool;
    private final StringRedisTemplate stringRedisTemplate;

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
    public void checkin(Long petId) {
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtil.copyProperties(user, response);
        return response;
    }
}