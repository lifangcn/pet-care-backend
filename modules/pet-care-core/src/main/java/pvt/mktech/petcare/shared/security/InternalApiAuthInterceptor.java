package pvt.mktech.petcare.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pvt.mktech.petcare.common.web.InternalApiHeaders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Authenticates requests made between PetCare services. */
@Component
public class InternalApiAuthInterceptor implements HandlerInterceptor {

    private final byte[] expectedToken;

    public InternalApiAuthInterceptor(@Value("${internal.api.token:}") String expectedToken) {
        this.expectedToken = expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String suppliedToken = request.getHeader(InternalApiHeaders.SERVICE_TOKEN);
        boolean valid = expectedToken.length > 0
                && suppliedToken != null
                && MessageDigest.isEqual(expectedToken, suppliedToken.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }
}
