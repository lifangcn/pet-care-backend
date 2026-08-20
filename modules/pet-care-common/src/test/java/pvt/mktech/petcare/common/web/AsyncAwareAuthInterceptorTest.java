package pvt.mktech.petcare.common.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncAwareAuthInterceptorTest {

    @Mock
    private HandlerInterceptor delegate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private final Object handler = new Object();
    private final ModelAndView modelAndView = new ModelAndView();

    @Test
    void asyncDispatchSkipsPreHandleAndReturnsTrue() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        var interceptor = new AsyncAwareAuthInterceptor(delegate);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(delegate, never()).preHandle(request, response, handler);
    }

    @Test
    void requestDispatchDelegatesPreHandle() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(delegate.preHandle(request, response, handler)).thenReturn(true);
        var interceptor = new AsyncAwareAuthInterceptor(delegate);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(delegate).preHandle(request, response, handler);
    }

    @Test
    void forwardDispatchDelegatesPreHandle() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.FORWARD);
        when(delegate.preHandle(request, response, handler)).thenReturn(true);
        var interceptor = new AsyncAwareAuthInterceptor(delegate);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(delegate).preHandle(request, response, handler);
    }

    @Test
    void includeDispatchDelegatesPreHandle() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.INCLUDE);
        when(delegate.preHandle(request, response, handler)).thenReturn(true);
        var interceptor = new AsyncAwareAuthInterceptor(delegate);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(delegate).preHandle(request, response, handler);
    }

    @Test
    void asyncDispatchSkipsPostHandleAndAfterCompletion() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        var interceptor = new AsyncAwareAuthInterceptor(delegate);

        interceptor.postHandle(request, response, handler, modelAndView);
        interceptor.afterCompletion(request, response, handler, null);

        verify(delegate, never()).postHandle(request, response, handler, modelAndView);
        verify(delegate, never()).afterCompletion(request, response, handler, null);
    }

    @Test
    void requestDispatchDelegatesPostHandleAndAfterCompletion() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        var interceptor = new AsyncAwareAuthInterceptor(delegate);

        interceptor.postHandle(request, response, handler, modelAndView);
        interceptor.afterCompletion(request, response, handler, null);

        verify(delegate).postHandle(request, response, handler, modelAndView);
        verify(delegate).afterCompletion(request, response, handler, null);
    }
}
