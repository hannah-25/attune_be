package attune.alarm.adapter.web;

import attune.alarm.application.NotificationInboxService;
import attune.alarm.application.dto.response.NotificationInboxResponse;
import attune.alarm.domain.model.NotificationReadStatus;
import attune.common.ApiVersion;
import attune.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림함", description = "사용자 알림함 조회 및 읽음 처리 API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/notifications")
public class NotificationInboxController {

    private final NotificationInboxService notificationInboxService;

    @Operation(summary = "알림함 조회", description = "읽음 상태별 알림을 최신 발송 시각 순의 cursor 방식으로 조회합니다.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    public ResponseEntity<NotificationInboxResponse> getInbox(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) NotificationReadStatus status,
            @RequestParam(required = false) String cursor,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationInboxService.getInbox(userDetails.getId(), status, cursor, size));
    }

    @Operation(summary = "알림 읽음 처리", description = "현재 사용자의 알림을 읽음으로 처리합니다. 이미 읽은 알림은 그대로 성공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PatchMapping("/{notificationHistoryId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationHistoryId
    ) {
        notificationInboxService.markAsRead(userDetails.getId(), notificationHistoryId);
        return ResponseEntity.noContent().build();
    }
}
