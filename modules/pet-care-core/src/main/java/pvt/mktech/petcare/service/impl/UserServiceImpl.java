package pvt.mktech.petcare.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.util.MinioUtil;
import pvt.mktech.petcare.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.dto.response.UserResponse;
import pvt.mktech.petcare.entity.User;
import pvt.mktech.petcare.mapper.UserMapper;
import pvt.mktech.petcare.service.UserService;

import java.util.List;
import java.util.Optional;

import static pvt.mktech.petcare.entity.table.UsersTableDef.USERS;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final MinioUtil minioUtil;

    @Override
    public UserResponse getUserById(Long userId) {
        User user = getOne(USERS.ID.eq(userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }
    
    @Override
    public UserResponse getUserByUsername(String username) {
        User user = getOne(USERS.USERNAME.eq(username));


        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }
    
    @Transactional
    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = getOne(USERS.ID.eq(userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查手机号是否已被其他用户使用
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .where(USERS.PHONE.eq(request.getPhone()))
                    .and(USERS.ID.eq(userId));
            if (userMapper.selectCountByQuery(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(request.getPhone());
        }

        // 删除旧头像（如果存在）
        if (StrUtil.isNotBlank(user.getAvatar())) {
            try {
                minioUtil.deleteAvatar(user.getAvatar());
            } catch (Exception e) {
                log.warn("删除旧头像失败: {}", user.getAvatar(), e);
                // 不阻断主流程
            }
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
        return exists(USERS.USERNAME.eq(username));
    }
    
    @Override
    public boolean checkPhoneExists(String phone) {
        return exists(USERS.PHONE.eq(phone));
    }

    @Override
    public List<Long> getActiveUserIds() {
        QueryWrapper queryWrapper = QueryWrapper.create().select(USERS.ID).from(USERS);
        return userMapper.selectObjectListByQueryAs(queryWrapper, Long.class);
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtil.copyProperties(user, response);
        return response;
    }
}