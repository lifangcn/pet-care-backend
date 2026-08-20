package pvt.mktech.petcare.user.service.impl;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.user.dto.LoginInfoDto;
import pvt.mktech.petcare.user.dto.request.LoginRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceImplDemoModeTest {

    private final RedisUtil redisUtil = mock(RedisUtil.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final HttpSession session = mock(HttpSession.class);
    private final AuthServiceImpl service = new AuthServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);
    }

    @Test
    void rejectsCodeGenerationWhenDemoModeIsDisabled() {
        ReflectionTestUtils.setField(service, "demoEnabled", false);

        assertThatThrownBy(() -> service.sendCode("13800138000", session))
                .isInstanceOf(BusinessException.class)
                .hasMessage("演示登录未启用");

        verifyNoInteractions(redisUtil, redissonClient);
    }

    @Test
    void returnsCodeOnlyWhenDemoModeIsEnabled() {
        ReflectionTestUtils.setField(service, "demoEnabled", true);
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);

        Result<String> result = service.sendCode("13800138000", session);

        assertThat(result.getData()).matches("\\d{6}");
        verify(redisUtil).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void rejectsWechatMockWhenDemoModeIsDisabled() {
        ReflectionTestUtils.setField(service, "demoEnabled", false);

        assertThatThrownBy(service::getWechatQRCode)
                .isInstanceOf(BusinessException.class)
                .hasMessage("演示登录未启用");
    }

    @Test
    void rejectsLoginAndRefreshWhenDemoModeIsDisabled() {
        ReflectionTestUtils.setField(service, "demoEnabled", false);

        assertThatThrownBy(() -> service.login(new LoginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("演示登录未启用");
        assertThatThrownBy(() -> service.refreshToken(new LoginInfoDto()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("演示登录未启用");
        assertThatThrownBy(() -> service.checkWechatScanStatus("ticket"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("演示登录未启用");
    }
}
