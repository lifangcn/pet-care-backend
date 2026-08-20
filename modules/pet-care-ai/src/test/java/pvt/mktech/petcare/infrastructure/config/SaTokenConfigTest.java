package pvt.mktech.petcare.infrastructure.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import pvt.mktech.petcare.common.web.UserContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class SaTokenConfigTest {

    private final SaTokenConfig config = new SaTokenConfig();

    @AfterEach
    void cleanUp() {
        UserContext.removeUserId();
    }

    @Test
    void setsUserContextAfterSuccessfulAuthentication() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);

            config.authenticateRequest();

            stp.verify(StpUtil::checkLogin);
            assertThat(UserContext.getUserId()).isEqualTo(42L);
        }
    }

    @Test
    void propagatesAuthenticationFailure() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::checkLogin).thenThrow(NotLoginException.class);

            assertThatThrownBy(config::authenticateRequest).isInstanceOf(NotLoginException.class);
            assertThat(UserContext.getUserId()).isNull();
        }
    }
}
