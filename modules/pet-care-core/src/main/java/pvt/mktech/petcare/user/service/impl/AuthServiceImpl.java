package pvt.mktech.petcare.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.redis.RedisUtil;
import cn.dev33.satoken.stp.StpUtil;
import pvt.mktech.petcare.infrastructure.ValidatorUtil;
import pvt.mktech.petcare.shared.dto.WechatQRCodeResponse;
import pvt.mktech.petcare.shared.dto.WechatScanStatus;
import pvt.mktech.petcare.user.dto.LoginInfoDto;
import pvt.mktech.petcare.user.dto.request.LoginRequest;
import pvt.mktech.petcare.user.entity.User;
import pvt.mktech.petcare.user.mapper.UserMapper;
import pvt.mktech.petcare.user.service.AuthService;
import pvt.mktech.petcare.points.service.PointsService;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.*;
import static pvt.mktech.petcare.user.entity.table.UserTableDef.USER;

/**
 * 认证服务实现类
 * {@code @date}: 2025/11/28 14:52
 * @author Michael
 */
@Slf4j
@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    private static final long ACCESS_TOKEN_TTL = 86400L;
    private static final long REFRESH_TOKEN_TTL = 604800L;

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;
    @Resource
    private PointsService pointsService;
    @Resource
    private RedissonClient redissonClient;


    @Override
    public Result<String> sendCode(String phone, HttpSession httpSession) {
        if (ValidatorUtil.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }

        String rateLimitKey = "rate_limit:sendCode:phone:" + phone;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimitKey);
        rateLimiter.trySetRate(RateType.OVERALL, 3, Duration.ofMinutes(1));
        if (!rateLimiter.tryAcquire(1)) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "验证码发送过于频繁，请稍后再试");
        }

        String code = RandomUtil.randomNumbers(6);
        redisUtil.set(LOGIN_CODE_KEY + phone, code, Duration.ofSeconds(LOGIN_CODE_TTL));
        log.info("向手机发送验证码: {}", code);
        return Result.success(code);
    }

    @Transactional
    @Override
    public Result<LoginInfoDto> login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();

        if (ValidatorUtil.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }

        String cacheCode = redisUtil.get(LOGIN_CODE_KEY + phone);
        if (!StrUtil.equals(code, cacheCode)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }

        User user = getOne(USER.PHONE.eq(phone));
        if (user == null) {
            user = createNewUser(phone);
        }

        LoginInfoDto loginInfoDto = new LoginInfoDto();
        BeanUtil.copyProperties(user, loginInfoDto);
        generateTokens(loginInfoDto);

        redisUtil.delete(LOGIN_CODE_KEY + phone);
        return Result.success(loginInfoDto);
    }

    /**
     * 创建新用户
     */
    private User createNewUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setUsername(USER_DEFAULT_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);

        Long userId = user.getId();
        pointsService.createAccount(userId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(CORE_USER_REGISTER_TOPIC, userId.toString(), userId.toString())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("发送用户注册主题失败，topic: {}, key: {}", CORE_USER_REGISTER_TOPIC, userId, ex);
                            }
                        });
            }
        });
        return user;
    }

    /**
     * 生成双 Token（Access Token + Refresh Token）
     * Access Token: JWT 格式，短期有效，用于接口访问
     * Refresh Token: UUID 格式，长期有效，存储在 Redis，用于刷新 Access Token
     */
    private void generateTokens(LoginInfoDto loginInfoDto) {
        Long userId = loginInfoDto.getId();

        // Sa-Token 登录，生成 JWT 格式的 Access Token
        StpUtil.login(userId);
        String accessToken = StpUtil.getTokenValue();

        // 生成 Refresh Token（UUID 格式，存储在 Redis）
        String refreshToken = RandomUtil.randomString(32);
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        redisUtil.set(refreshTokenKey, userId.toString(), Duration.ofSeconds(REFRESH_TOKEN_TTL));

        loginInfoDto.setAccessToken(accessToken);
        loginInfoDto.setRefreshToken(refreshToken);
        loginInfoDto.setExpiresIn(ACCESS_TOKEN_TTL);

        log.debug("用户登录成功，userId: {}, accessToken 已生成", userId);
    }

    @Override
    public LoginInfoDto refreshToken(LoginInfoDto dto) {
        String refreshToken = dto.getRefreshToken();
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 从 Redis 获取 Refresh Token 对应的用户 ID
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userIdStr = redisUtil.get(refreshTokenKey);
        if (StrUtil.isBlank(userIdStr)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        Long userId = Long.parseLong(userIdStr);

        // 删除旧的 Refresh Token
        redisUtil.delete(refreshTokenKey);

        // 查询用户信息
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 生成新的双 Token
        LoginInfoDto newLoginInfo = new LoginInfoDto();
        BeanUtil.copyProperties(user, newLoginInfo);
        generateTokens(newLoginInfo);

        log.info("Token 刷新成功，userId: {}", userId);
        return newLoginInfo;
    }

    @Override
    public void logout(LoginInfoDto dto) {
        try {
            // 删除 Refresh Token
            if (dto != null && StrUtil.isNotBlank(dto.getRefreshToken())) {
                String refreshTokenKey = REFRESH_TOKEN_PREFIX + dto.getRefreshToken();
                redisUtil.delete(refreshTokenKey);
            }

            // Sa-Token 登出
            StpUtil.logout();
            log.info("用户登出成功");
        } catch (Exception e) {
            log.warn("登出失败", e);
        }
    }

    @Override
    public Result<WechatQRCodeResponse> getWechatQRCode() {
        String ticket = "platform_review_too_strict_mock_ticket";
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
        String status = redisUtil.get(WECHAT_LOGIN_TICKET_KEY + ticket);
        if (status == null) {
            return Result.success(WechatScanStatus.builder()
                    .status("EXPIRED")
                    .build());
        }

        if ("WAITING".equals(status)) {
            redisUtil.set(WECHAT_LOGIN_TICKET_KEY + ticket, "CONFIRMED", Duration.ofSeconds(WECHAT_LOGIN_TTL));
            return Result.success(WechatScanStatus.builder()
                    .status("WAITING")
                    .build());
        }

        if ("CONFIRMED".equals(status)) {
            User user = new User();
            user.setUsername("wechat_user_" + RandomUtil.randomString(6));
            user.setNickname("WechatUser");
            save(user);

            LoginInfoDto loginInfoDto = new LoginInfoDto();
            BeanUtil.copyProperties(user, loginInfoDto);
            generateTokens(loginInfoDto);

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
