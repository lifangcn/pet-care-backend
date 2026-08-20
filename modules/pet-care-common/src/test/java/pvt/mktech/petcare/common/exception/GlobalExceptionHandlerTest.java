package pvt.mktech.petcare.common.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.dev33.satoken.exception.NotLoginException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pvt.mktech.petcare.common.dto.response.Result;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
        appender = new ListAppender<>();
        appender.setName("test-appender");
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        if (appender != null) {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void handlesNotLoginExceptionWithGenericUnauthorizedResponseAndSafeLogs() throws NoSuchMethodException {
        String sensitiveToken = "eyJhbGciOiJIUzI1NiIs-" + UUID.randomUUID();
        NotLoginException ex = NotLoginException.newInstance(
                "login",
                NotLoginException.INVALID_TOKEN,
                "token 无效",
                sensitiveToken
        );

        Result<String> result = handler.handleNotLoginException(ex);

        assertThat(result.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
        assertThat(result.getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());

        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(
                GlobalExceptionHandler.class.getMethod("handleNotLoginException", NotLoginException.class),
                ResponseStatus.class
        );
        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String allLogs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertThat(allLogs)
                .contains("Not logged in")
                .contains("type=" + NotLoginException.INVALID_TOKEN)
                .contains("loginType=login")
                .doesNotContain(sensitiveToken)
                .doesNotContain(ex.getMessage());

        for (ILoggingEvent event : appender.list) {
            assertThat(event.getThrowableProxy()).isNull();
        }
    }
}
