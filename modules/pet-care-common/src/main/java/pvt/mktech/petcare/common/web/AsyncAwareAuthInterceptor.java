package pvt.mktech.petcare.common.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * {@code @description} HandlerInterceptor 包装器：仅对 ASYNC 派发跳过认证逻辑，
 * REQUEST/FORWARD/INCLUDE 等正常委托，避免异步二次派发时重复执行 SA-Token 鉴权。
 * {@code @date} 2026-08-20
 * {@code @author} Michael Li
 */
public class AsyncAwareAuthInterceptor implements HandlerInterceptor {

    private final HandlerInterceptor delegate;

    /**
     * @param delegate 实际执行认证的拦截器
     */
    public AsyncAwareAuthInterceptor(HandlerInterceptor delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }
        return delegate.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (request.getDispatcherType() != DispatcherType.ASYNC) {
            delegate.postHandle(request, response, handler, modelAndView);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (request.getDispatcherType() != DispatcherType.ASYNC) {
            delegate.afterCompletion(request, response, handler, ex);
        }
    }
}
