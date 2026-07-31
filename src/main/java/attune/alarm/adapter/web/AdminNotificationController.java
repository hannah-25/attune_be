package attune.alarm.adapter.web;

import attune.alarm.application.AdminNotificationService;
import attune.common.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 알람", description = "마케팅 푸시 알람 발송 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/notices")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @Operation(summary = "공지 푸시 발송", description = "marketingNotification=true인 전체 사용자에게 비동기 발송합니다.")
    @PostMapping("/{noticeId}/push")
    public ResponseEntity<Void> sendNoticePush(@PathVariable Long noticeId) {
        adminNotificationService.sendNoticeToAll(noticeId);
        return ResponseEntity.accepted().build();
    }
}
