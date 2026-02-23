package pvt.mktech.petcare.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.jwt.JwtUtil;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.infrastructure.ValidatorUtil;
import pvt.mktech.petcare.shared.dto.WechatQRCodeResponse;
import pvt.mktech.petcare.shared.dto.WechatScanStatus;
import pvt.mktech.petcare.user.dto.LoginInfoDto;
import pvt.mktech.petcare.user.dto.request.LoginRequest;
import pvt.mktech.petcare.user.entity.User;
import pvt.mktech.petcare.user.mapper.UserMapper;
import pvt.mktech.petcare.user.service.AuthService;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.*;
import static pvt.mktech.petcare.user.entity.table.UserTableDef.USER;

/**
 * {@code @description}:
 * {@code @date}: 2025/11/28 14:52
 *
 * @author Michael
 */
@Slf4j
@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;


    @Override
    public Result<String> sendCode(String phone, HttpSession httpSession) {
        // 1.校验手机号
        if (ValidatorUtil.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }
        // 3.生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4.保存验证码到 redis
        redisUtil.set(LOGIN_CODE_KEY + phone, code, Duration.ofSeconds(LOGIN_CODE_TTL));
        log.info("向手机发送验证码: {}", code);
        return Result.success(code);
    }


    @Transactional
    @Override
    public Result<LoginInfoDto> login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();
        // 1. 校验手机号和验证码是否正确
        if (ValidatorUtil.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }
        String cacheCode = redisUtil.get(LOGIN_CODE_KEY + phone);
        // cacheCode为空，可能验证码过期；request.code为空，可能请求错误。这里简单判断
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
            Long userId = user.getId();
            kafkaTemplate.send(CORE_USER_REGISTER_TOPIC, userId.toString(), userId.toString())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("发送 用户注册主题 失败，topic: {}, key: {}", CORE_USER_REGISTER_TOPIC, userId, ex);
                        }
                    });
        }
        LoginInfoDto loginInfoDto = new LoginInfoDto();
        BeanUtil.copyProperties(user, loginInfoDto);
        // 4.生成双token, 并保存 refreshToken 到 Redis中用于后续认证
        generateDoubleToken(loginInfoDto);
        // 将验证码移除
        redisUtil.delete(LOGIN_CODE_KEY + phone);
        return Result.success(loginInfoDto);
    }

    /**
     * 生成双token，即access token和refresh token，并保存 refresh token 到 Redis中用于后续认证
     *
     * @param loginInfoDto
     */
    private void generateDoubleToken(LoginInfoDto loginInfoDto) {
        String accessToken = jwtUtil.generateAccessToken(loginInfoDto.getId());
        String refreshToken = jwtUtil.generateRefreshToken(loginInfoDto.getId());
        loginInfoDto.setAccessToken(accessToken);
        loginInfoDto.setRefreshToken(refreshToken);
        loginInfoDto.setExpiresIn(ACCESS_TOKEN_TTL);
        redisUtil.set(REFRESH_TOKEN_KEY + loginInfoDto.getId(), refreshToken, Duration.ofSeconds(REFRESH_TOKEN_TTL));
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
        String storedToken = redisUtil.get(REFRESH_TOKEN_KEY + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 4. 查询用户信息，生成新的access token
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String newAccessToken = jwtUtil.generateAccessToken(userId);
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
            redisUtil.delete(REFRESH_TOKEN_KEY + userId);
        } catch (Exception e) {
            log.warn("登出时解析refresh token失败", e);
        }
    }

    @Override
    public Result<WechatQRCodeResponse> getWechatQRCode() {
        String ticket = "平台审核太苛刻，我只能假装调用接口";
        // TODO 后续替换为微信开放平台真实调用
        //  暂时生成二维码Base64
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        QrCodeUtil.generate(ticket, 300, 300, "jpg", stream);
        String base64 = Base64.getEncoder().encodeToString(stream.toByteArray());
        String qrcodeUrl = "data:image/jpeg;base64," + base64;
        redisUtil.set(WECHAT_LOGIN_TICKET_KEY + ticket, "WAITING", Duration.ofSeconds(WECHAT_LOGIN_TTL));
        return Result.success(WechatQRCodeResponse.builder()
                .qrcodeUrl(qrcodeUrl)
                .ticket(ticket)
                .expireTime(WECHAT_LOGIN_TTL)
                .build());
    }

    @Override
    public Result<WechatScanStatus> checkWechatScanStatus(String ticket) {
        // TODO 后续替换为微信开放平台真实调用
        String status = redisUtil.get(WECHAT_LOGIN_TICKET_KEY + ticket);
        if (status == null) {
            return Result.success(WechatScanStatus.builder()
                    .status("EXPIRED")
                    .build());
        }
        // 模拟状态：WAITING -> CONFIRMED（实际应接入微信回调更新状态）
        if ("WAITING".equals(status)) {
            // 模拟：首次查询后自动变为已确认状态（方便测试）
            redisUtil.set(WECHAT_LOGIN_TICKET_KEY + ticket, "CONFIRMED", Duration.ofSeconds(WECHAT_LOGIN_TTL));
            return Result.success(WechatScanStatus.builder()
                    .status("WAITING")
                    .build());
        }
        if ("CONFIRMED".equals(status)) {
            // 模拟创建用户并返回登录信息
            User user = new User();
            user.setUsername("wechat_user_" + RandomUtil.randomString(6));
            user.setNickname("WechatUser");
            save(user);
            LoginInfoDto loginInfoDto = new LoginInfoDto();
            BeanUtil.copyProperties(user, loginInfoDto);
            generateDoubleToken(loginInfoDto);
            // 清除ticket
            redisUtil.delete(WECHAT_LOGIN_TICKET_KEY + ticket);
            return Result.success(WechatScanStatus.builder()
                    .status("CONFIRMED")
                    .loginInfo(loginInfoDto)
                    .build());
        }
        return Result.success(WechatScanStatus.builder()
                .status(status)
                .build());
    }
}
