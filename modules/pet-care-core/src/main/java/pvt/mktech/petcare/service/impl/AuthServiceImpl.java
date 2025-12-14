package pvt.mktech.petcare.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.dto.response.ResultCode;
import pvt.mktech.petcare.common.util.JwtUtil;
import pvt.mktech.petcare.context.UserHolder;
import pvt.mktech.petcare.dto.LoginInfoDto;
import pvt.mktech.petcare.dto.request.LoginRequest;
import pvt.mktech.petcare.entity.User;
import pvt.mktech.petcare.mapper.UserMapper;
import pvt.mktech.petcare.service.AuthService;
import pvt.mktech.petcare.util.ValidatorUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static pvt.mktech.petcare.context.UserConstants.*;
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

    private final StringRedisTemplate stringRedisTemplate;

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
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code,
                LOGIN_CODE_TTL, TimeUnit.SECONDS);
        log.info("向手机发送验证码: {}", code);
        return Result.success("发送短信验证码成功");
    }


    @Override
    public Result<LoginInfoDto> login(LoginRequest request, String clientIp) {
        String phone = request.getPhone();
        String code = request.getCode();
        // 1. 校验手机号和验证码是否正确
        if (ValidatorUtil.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.error(ResultCode.FAILED, "手机号格式错误！");
        }

        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
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
            user.setPasswordHash(RandomUtil.randomNumbers(6));
            save(user);
        }
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        LoginInfoDto loginInfoDto = new LoginInfoDto();
        BeanUtil.copyProperties(user, loginInfoDto);
        loginInfoDto.setToken(token);
        Map<String, Object> userMap = BeanUtil.beanToMap(loginInfoDto, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> StrUtil.toStringOrNull(fieldValue)));

        String tokenKey = LOGIN_TOKEN_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_TOKEN_TTL, TimeUnit.SECONDS);
        // 将验证码移除
        stringRedisTemplate.delete(LOGIN_CODE_KEY + code);
        return Result.success(loginInfoDto);
    }

    @Override
    public Result<Void> logout(String token) {
        // 移除线程缓存
        UserHolder.removeUser();
        // redis token缓存
        stringRedisTemplate.delete(LOGIN_TOKEN_KEY + token);
        return Result.success();
    }
}
