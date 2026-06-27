package attune.alarm.application;

import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StubPushSenderTest {

    private final StubPushSender sender = new StubPushSender();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(StubPushSender.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void send_logsMetadataOnly_andNeverLogsTitleOrBodyContent() {
        UUID userId = UUID.randomUUID();
        NotificationSubscription subscription = NotificationSubscription.builder()
                .userId(userId)
                .platform(NotificationPlatform.WEB)
                .provider(NotificationProvider.WEB_PUSH)
                .build();
        // 본문에 건강 PII(복약 라벨 등)가 들어오는 상황을 모사
        PushMessage message = new PushMessage("타이레놀 복약 시간", "복약 시간이 됐어요.", "/medication");

        sender.send(subscription, message);

        assertThat(appender.list).hasSize(1);
        String log = appender.list.get(0).getFormattedMessage();

        // 메타데이터는 남는다
        assertThat(log).contains(userId.toString());
        assertThat(log).contains("WEB");
        assertThat(log).contains("WEB_PUSH");
        assertThat(log).contains("titleLen=" + "타이레놀 복약 시간".length());
        assertThat(log).contains("bodyLen=" + "복약 시간이 됐어요.".length());

        // 본문 콘텐츠(PII)는 절대 남지 않는다
        assertThat(log).doesNotContain("타이레놀");
        assertThat(log).doesNotContain("복약 시간이 됐어요");
    }

    @Test
    void send_handlesNullTitleAndBody() {
        NotificationSubscription subscription = NotificationSubscription.builder()
                .userId(UUID.randomUUID())
                .platform(NotificationPlatform.IOS)
                .provider(NotificationProvider.APNS)
                .build();

        sender.send(subscription, new PushMessage(null, null, null));

        assertThat(appender.list).hasSize(1);
        String log = appender.list.get(0).getFormattedMessage();
        assertThat(log).contains("titleLen=0");
        assertThat(log).contains("bodyLen=0");
    }
}
