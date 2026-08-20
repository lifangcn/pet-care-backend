package pvt.mktech.petcare.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import pvt.mktech.petcare.common.web.InternalApiHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiAuthInterceptorTest {

    private final InternalApiAuthInterceptor interceptor = new InternalApiAuthInterceptor("expected-token");

    @Test
    void rejectsMissingToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(new MockHttpServletRequest(), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsWrongToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(InternalApiHeaders.SERVICE_TOKEN, "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void acceptsMatchingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(InternalApiHeaders.SERVICE_TOKEN, "expected-token");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }
}
