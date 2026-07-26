package attune.alarm.adapter.web;

import attune.alarm.application.NotificationDeliveryReceiptService;
import attune.alarm.application.dto.request.NotificationDeliveryEventRequest;
import attune.common.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "알림 영수증", description = "서비스 워커의 push 도달 영수증 수집 API (인증 불필요)")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/notification-delivery-attempts")
public class NotificationDeliveryEventController {

    private final NotificationDeliveryReceiptService receiptService;

    @Operation(
            summary = "영수증 이벤트 기록",
            description = "서비스 워커가 RECEIVED/DISPLAYED/OPENED 영수증을 기록합니다. "
                    + "attempt 존재 여부를 노출하지 않기 위해 정상/중복/존재하지 않음/token 불일치/만료 요청 모두 204를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "요청 처리됨 (정상/중복/존재하지 않음/token 불일치/만료 모두 동일)"),
            @ApiResponse(responseCode = "429", description = "rate limit 초과")
    })
    @PostMapping("/{deliveryAttemptId}/events")
    public ResponseEntity<Void> recordEvent(
            @PathVariable UUID deliveryAttemptId,
            @Valid @RequestBody NotificationDeliveryEventRequest request,
            HttpServletRequest servletRequest
    ) {
        receiptService.recordEvent(deliveryAttemptId, request.event(), request.receiptToken(), clientIp(servletRequest));
        return ResponseEntity.noContent().build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
