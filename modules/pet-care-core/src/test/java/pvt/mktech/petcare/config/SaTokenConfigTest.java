package pvt.mktech.petcare.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.shared.security.InternalApiAuthInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class SaTokenConfigTest {

    private final SaTokenConfig config = new SaTokenConfig(mock(InternalApiAuthInterceptor.class));

    @AfterEach
    void cleanUp() {
        UserContext.removeUserId();
    }

    @Test
    void authInterceptorAuthenticatesOnRequestDispatch() throws Exception {
        HandlerInterceptor authInterceptor = config.authInterceptor();
        SaRequest saRequest = mock(SaRequest.class);
        when(saRequest.getRequestPath()).thenReturn("/api/users");
        try (MockedStatic<SaHolder> saHolder = mockStatic(SaHolder.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            saHolder.when(SaHolder::getRequest).thenReturn(saRequest);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setDispatcherType(DispatcherType.REQUEST);
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            stp.verify(StpUtil::checkLogin);
            assertThat(UserContext.getUserId()).isEqualTo(42L);
        }
    }

    @Test
    void authInterceptorSkipsAuthenticationOnAsyncDispatch() throws Exception {
        HandlerInterceptor authInterceptor = config.authInterceptor();
        try (MockedStatic<SaHolder> saHolder = mockStatic(SaHolder.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setDispatcherType(DispatcherType.ASYNC);
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            saHolder.verify(SaHolder::getRequest, never());
            stp.verify(StpUtil::checkLogin, never());
            assertThat(UserContext.getUserId()).isNull();
        }
    }
}
