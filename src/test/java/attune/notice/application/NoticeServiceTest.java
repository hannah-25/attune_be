package attune.notice.application;

import attune.alarm.application.event.NoticePushRequestedEvent;
import attune.common.mail.event.NoticePublishedEvent;
import attune.notice.application.dto.request.CreateNoticeRequest;
import attune.notice.domain.model.Notice;
import attune.notice.domain.repository.NoticeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeServiceTest {

    @Test
    void publishesPushAndEmailEventsAfterSavingNotice() {
        NoticeRepository repository = mock(NoticeRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        NoticeService service = new NoticeService(repository, eventPublisher);
        Notice saved = Notice.builder()
                .id(1L)
                .title("title")
                .content("content")
                .createdAt(LocalDateTime.of(2026, 6, 22, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 22, 10, 0))
                .build();
        when(repository.save(any(Notice.class))).thenReturn(saved);

        service.createNotice(new CreateNoticeRequest(
                "title",
                "content",
                false,
                true,
                true
        ));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2))
                .publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .containsExactly(
                        new NoticePushRequestedEvent(
                                saved.getId(),
                                saved.getTitle(),
                                saved.getContent(),
                                saved.getCreatedAt()
                        ),
                        new NoticePublishedEvent(saved.getTitle(), saved.getContent())
                );
    }
}
