package pvt.mktech.petcare.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.jwt.JwtUtil;
import pvt.mktech.petcare.common.redis.RedisCacheUtil;
import pvt.mktech.petcare.core.dto.LoginInfoDto;
import pvt.mktech.petcare.core.dto.request.LoginRequest;
import pvt.mktech.petcare.core.entity.User;
import pvt.mktech.petcare.core.mapper.UserMapper;
import pvt.mktech.petcare.core.service.AuthService;
import pvt.mktech.petcare.core.util.ValidatorUtil;

import java.time.Duration;

import static pvt.mktech.petcare.core.constant.CoreConstant.*;
import static pvt.mktech.petcare.core.entity.table.UserTableDef.USER;

/**
 * {@code @description}:
 * {@code @date}: 2025/11/28 14:52
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    private final JwtUtil jwtUtil;
    private final RedisCacheUtil redisCacheUtil;

    @Override
    public Result<String> sendCode(String phone, HttpSession httpSession) {
        // 1.校验手机号
        if (ValidatorUtil.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }
        // 3.生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4.保存验证码到 redis
        redisCacheUtil.set(LOGIN_CODE_KEY + phone, code, Duration.ofSeconds(LOGIN_CODE_TTL));
        log.info("向手机发送验证码: {}", code);
        return Result.success("发送短信验证码成功");
    }


    @Override
    public Result<LoginInfoDto> login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();
        // 1. 校验手机号和验证码是否正确
        if (ValidatorUtil.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }
        String cacheCode = redisCacheUtil.get(LOGIN_CODE_KEY + phone);
        // TODO cacheCode为空，可能验证码过期；request.code为空，可能请求错误。这里简单判断
        if (!StrUtil.equals(code, cacheCode)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }

        // 2.手机号用户是否存在
        User user = getOne(USER.PHONE.eq(phone));
        // 3.不存在，创建用户
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setUsername(USER_DEFAULT_NAME_PREFIX + RandomUtil.randomString(10));
            save(user);
        }
        LoginInfoDto loginInfoDto = new LoginInfoDto();
        BeanUtil.copyProperties(user, loginInfoDto);
        // 4.生成双token, 并保存 refreshToken 到 Redis中用于后续认证
        generateDoubleToken(loginInfoDto);
        // 将验证码移除
        redisCacheUtil.delete(LOGIN_CODE_KEY + phone);
        return Result.success(loginInfoDto);
    }

    /**
     * 生成双token，即access token和refresh token，并保存 refresh token 到 Redis中用于后续认证
     *
     * @param user 数据库查询或新增的用户
     * @return 登录信息Dto
     */
    private void generateDoubleToken(LoginInfoDto loginInfoDto) {
        String accessToken = jwtUtil.generateAccessToken(loginInfoDto.getUsername(), loginInfoDto.getId());
        String refreshToken = jwtUtil.generateRefreshToken(loginInfoDto.getId());
        loginInfoDto.setAccessToken(accessToken);
        loginInfoDto.setRefreshToken(refreshToken);
        loginInfoDto.setExpiresIn(ACCESS_TOKEN_TTL);
        redisCacheUtil.set(REFRESH_TOKEN_KEY + loginInfoDto.getId(), refreshToken, Duration.ofSeconds(REFRESH_TOKEN_TTL));
    }

    @Override
    public LoginInfoDto refreshToken(LoginInfoDto dto) {
        String refreshToken = dto.getRefreshToken();
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 1. 验证refresh token格式
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 2. 解析token获取用户ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            log.error("解析refresh token失败", e);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 3. 验证Redis中的refresh token是否匹配
        String storedToken = redisCacheUtil.get(REFRESH_TOKEN_KEY + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 4. 查询用户信息，生成新的access token
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), userId);
        dto.setAccessToken(newAccessToken);
        dto.setExpiresIn(ACCESS_TOKEN_TTL);
        return dto;
    }

    @Override
    public void logout(LoginInfoDto dto) {
        if (StrUtil.isBlank(dto.getRefreshToken())) {
            return;
        }
        try {
            // 删除refresh token
            Long userId = jwtUtil.getUserIdFromToken(dto.getRefreshToken());
            redisCacheUtil.delete(REFRESH_TOKEN_KEY + userId);
        } catch (Exception e) {
            log.warn("登出时解析refresh token失败", e);
        }
    }
}
