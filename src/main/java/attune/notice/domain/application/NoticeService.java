package attune.notice.domain.application;

import attune.common.error.notfound.NoticeNotFoundException;
import attune.notice.domain.application.dto.request.CreateNoticeRequest;
import attune.notice.domain.application.dto.request.UpdateNoticeRequest;
import attune.notice.domain.application.dto.response.*;
import attune.notice.domain.model.Notice;
import attune.notice.domain.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public NoticeListResponse getNotices(int page, int size, String q) {
        Sort sort = Sort.by(Sort.Direction.DESC, "isPinned")
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable pageable = PageRequest.of(page, size, sort);
        String searchQuery = (q != null && !q.isBlank()) ? q : null;

        Page<NoticeListItemResponse> result = noticeRepository
                .findAllByIsDeletedFalseAndSearch(searchQuery, pageable)
                .map(NoticeListItemResponse::from);

        return NoticeListResponse.from(result);
    }

    @Transactional(readOnly = true)
    public NoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
        return NoticeDetailResponse.from(notice);
    }

    @Transactional
    public CreateNoticeResponse createNotice(CreateNoticeRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .isPinned(request.isPinned())
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return CreateNoticeResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public UpdateNoticeResponse updateNotice(Long noticeId, UpdateNoticeRequest request) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
        notice.update(request.title(), request.content(), request.isPinned());
        return UpdateNoticeResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
        notice.delete();
    }
}
