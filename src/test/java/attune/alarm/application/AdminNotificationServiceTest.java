package attune.alarm.application;

import attune.common.error.notfound.NoticeNotFoundException;
import attune.notice.domain.model.Notice;
import attune.notice.domain.repository.NoticeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminNotificationServiceTest {

    @Test
    void sendNoticeToAllThrowsBeforeAsyncSendWhenNoticeDoesNotExist() {
        NoticeRepository noticeRepository = mock(NoticeRepository.class);
        AdminNotificationSender adminNotificationSender = mock(AdminNotificationSender.class);
        AdminNotificationService service = new AdminNotificationService(noticeRepository, adminNotificationSender);
        Long noticeId = 1L;

        when(noticeRepository.findByIdAndIsDeletedFalse(noticeId)).thenReturn(Optional.empty());

        assertThrows(NoticeNotFoundException.class, () -> service.sendNoticeToAll(noticeId));
        verifyNoInteractions(adminNotificationSender);
    }

    @Test
    void sendNoticeToAllDelegatesToAsyncSenderAfterNoticeValidation() {
        NoticeRepository noticeRepository = mock(NoticeRepository.class);
        AdminNotificationSender adminNotificationSender = mock(AdminNotificationSender.class);
        AdminNotificationService service = new AdminNotificationService(noticeRepository, adminNotificationSender);
        Long noticeId = 1L;
        Notice notice = Notice.builder()
                .id(noticeId)
                .title("title")
                .content("content")
                .build();

        when(noticeRepository.findByIdAndIsDeletedFalse(noticeId)).thenReturn(Optional.of(notice));

        service.sendNoticeToAll(noticeId);

        verify(adminNotificationSender).sendNoticeToAll(noticeId, "title", "content");
    }
}
