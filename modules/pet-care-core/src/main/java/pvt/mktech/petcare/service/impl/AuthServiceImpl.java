package pvt.mktech.petcare.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.dto.response.ResultCode;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.util.JwtUtil;
import pvt.mktech.petcare.common.util.RedisCacheUtil;
import pvt.mktech.petcare.dto.LoginInfoDto;
import pvt.mktech.petcare.dto.request.LoginRequest;
import pvt.mktech.petcare.entity.User;
import pvt.mktech.petcare.mapper.UserMapper;
import pvt.mktech.petcare.service.AuthService;
import pvt.mktech.petcare.util.ValidatorUtil;

import java.time.Duration;

import static pvt.mktech.petcare.common.constant.CommonConstant.*;
import static pvt.mktech.petcare.constant.UserConstant.*;
import static pvt.mktech.petcare.entity.table.UsersTableDef.USERS;

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
            // 2.如果不符合，返回错误信息
            return Result.error(ResultCode.FAILED, "手机号格式错误！");
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
            // 如果不符合，返回错误信息
            return Result.error(ResultCode.FAILED, "手机号格式错误！");
        }
        String cacheCode = redisCacheUtil.get(LOGIN_CODE_KEY + phone);
        // TODO cacheCode为空，可能验证码过期；request.code为空，可能请求错误。这里简单判断
        if (!StrUtil.equals(code, cacheCode)) {
            return Result.error(ResultCode.FAILED, "验证码不正确！");
        }

        // 2.手机号用户是否存在
        User user = getOne(USERS.PHONE.eq(phone));
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

        // 1. 验证refresh token格式
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
//            throw new BusinessException(401, "Invalid refresh token");
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 2. 解析token获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 3. 验证Redis中的refresh token是否匹配
        String storedToken = redisCacheUtil.get(REFRESH_TOKEN_KEY + userId);
        if (!refreshToken.equals(storedToken)) {
//            throw new BusinessException(401, "Refresh token mismatch");
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 4. 查询用户信息
        User user = getById(userId);
        // 5. 生成新的access token
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), userId);

        dto.setAccessToken(newAccessToken);
        dto.setExpiresIn(86400L);
        return dto;
    }

    @Override
    public void logout(LoginInfoDto dto) {
        String refreshToken = dto.getRefreshToken();
        if (refreshToken != null && refreshToken.startsWith(TOKEN_PREFIX)) {
            // 从 token 中解析用户ID
            Long userId = jwtUtil.getUserIdFromToken(refreshToken.substring(TOKEN_PREFIX.length()));
            // 删除refresh token
            redisCacheUtil.delete(REFRESH_TOKEN_KEY + userId);
            // TODO 实现token失效逻辑，如加入黑名单或删除缓存
            // redisCacheUtil.set(CommonConstant.BLACKLIST_TOKEN_KEY + token, "invalid",
            // CommonConstant.ACCESS_TOKEN_TTL, TimeUnit.SECONDS);
        }
    }
}
