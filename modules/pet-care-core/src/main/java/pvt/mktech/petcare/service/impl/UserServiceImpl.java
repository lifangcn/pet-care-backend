package pvt.mktech.petcare.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.dto.response.UserResponse;
import pvt.mktech.petcare.entity.User;
import pvt.mktech.petcare.mapper.UserMapper;
import pvt.mktech.petcare.service.UserService;

import java.time.LocalDateTime;
import java.util.Optional;

import static pvt.mktech.petcare.entity.table.UsersTableDef.USERS;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;

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

        // 更新基本信息
        Optional.ofNullable(request.getNickname()).ifPresent(user::setNickname);
        Optional.ofNullable(request.getAvatarUrl()).ifPresent(user::setAvatarUrl);
        Optional.ofNullable(request.getGender()).ifPresent(user::setGender);
        Optional.ofNullable(request.getBirthday()).ifPresent(user::setBirthday);

        // 检查邮箱是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            QueryWrapper queryWrapper = query()
                    .where(USERS.EMAIL.eq(request.getEmail()))
                    .and(USERS.ID.eq(userId));
            if (exists(queryWrapper)) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.getEmail());
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

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);

        // 发送用户更新事件到消息队列
//        userEventProducer.sendUserUpdateEvent(user);

        log.info("用户信息已更新: userId={}", userId);
        return convertToResponse(user);
    }
    
    @Transactional
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 验证旧密码
//        if (!PasswordUtil.matches(oldPassword, user.getPasswordHash())) {
//            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
//        }
        
        // 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUpdatedAt(LocalDateTime.now());
        userMapper.update(updateUser);
        
        // 发送密码修改事件
//        userEventProducer.sendPasswordChangeEvent(userId);
        
        log.info("用户密码已修改: userId={}", userId);
    }
    
    @Override
    public boolean checkUsernameExists(String username) {
        return exists(USERS.USERNAME.eq(username));
    }
    
    @Override
    public boolean checkEmailExists(String email) {
        return exists(USERS.EMAIL.eq(email));
    }
    
    @Override
    public boolean checkPhoneExists(String phone) {
        return exists(USERS.PHONE.eq(phone));
    }
    
    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtil.copyProperties(user, response);
        return response;
    }
}