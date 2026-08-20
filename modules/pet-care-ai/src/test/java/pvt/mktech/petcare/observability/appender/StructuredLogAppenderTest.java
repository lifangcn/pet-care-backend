package pvt.mktech.petcare.observability.appender;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.content.MediaContent;
import pvt.mktech.petcare.observability.context.ObservationContext;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;
import pvt.mktech.petcare.observability.store.ChatTraceStore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredLogAppenderTest {

    @Test
    void convertsNullContentAndToolCallsToSafeDefaults() {
        StructuredLogAppender appender = new StructuredLogAppender(document -> { }, tokenEstimator());
        ObservationContext context = new ObservationContext();
        context.setRequestContent(null);
        context.setResponseContent(null);
        context.setToolCalls(null);

        ChatTraceDocument document = appender.toDocument(context);

        assertEquals("", document.getRequest().getContent());
        assertEquals("", document.getResponse().getContent());
        assertTrue(document.getToolCalls().isEmpty());
    }

    @Test
    void fallsBackOnceWhenStoreFails() throws InterruptedException {
        CountDownLatch fallback = new CountDownLatch(1);
        AtomicInteger fallbackCount = new AtomicInteger();
        ChatTraceStore failingStore = document -> { throw new IllegalStateException("unavailable"); };
        StructuredLogAppender appender = new StructuredLogAppender(failingStore, tokenEstimator()) {
            @Override
            protected void fallbackLog(ObservationContext context) {
                fallbackCount.incrementAndGet();
                fallback.countDown();
            }
        };

        appender.appendAsync(new ObservationContext());

        assertTrue(fallback.await(5, TimeUnit.SECONDS));
        assertEquals(1, fallbackCount.get());
    }

    @Test
    void doesNotFallbackWhenStoreSucceeds() throws InterruptedException {
        CountDownLatch saved = new CountDownLatch(1);
        AtomicInteger fallbackCount = new AtomicInteger();
        StructuredLogAppender appender = new StructuredLogAppender(document -> saved.countDown(), tokenEstimator()) {
            @Override
            protected void fallbackLog(ObservationContext context) {
                fallbackCount.incrementAndGet();
            }
        };

        appender.appendAsync(new ObservationContext());

        assertTrue(saved.await(5, TimeUnit.SECONDS));
        assertEquals(0, fallbackCount.get());
    }

    private TokenCountEstimator tokenEstimator() {
        return new TokenCountEstimator() {
            @Override
            public int estimate(String text) {
                return text.length();
            }

            @Override
            public int estimate(MediaContent mediaContent) {
                return 0;
            }

            @Override
            public int estimate(Iterable<MediaContent> mediaContents) {
                return 0;
            }
        };
    }
}
