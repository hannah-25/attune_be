package attune.alarm.application;

import attune.common.error.notfound.NoticeNotFoundException;
import attune.notice.domain.model.Notice;
import attune.notice.domain.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AdminNotificationService {

    private final NoticeRepository noticeRepository;
    private final AdminNotificationSender adminNotificationSender;

    @Transactional(readOnly = true)
    public void sendNoticeToAll(Long noticeId) {
        Notice notice = loadNotice(noticeId);
        adminNotificationSender.sendNoticeToAll(noticeId, notice.getTitle(), notice.getContent());
    }

    private Notice loadNotice(Long noticeId) {
        return noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
    }
}
