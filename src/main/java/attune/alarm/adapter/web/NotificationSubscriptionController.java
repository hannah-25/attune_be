package attune.alarm.adapter.web;

import attune.alarm.application.NotificationSubscriptionService;
import attune.alarm.application.dto.request.RegisterSubscriptionRequest;
import attune.alarm.application.dto.response.SubscriptionResponse;
import attune.common.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알람 구독", description = "푸시 알람 구독 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/alarm/subscriptions")
public class NotificationSubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    @Operation(summary = "구독 등록/갱신", description = "푸시 알람 수신을 위한 구독 정보를 등록하거나 갱신합니다.")
    @PostMapping
    public ResponseEntity<SubscriptionResponse> register(@Valid @RequestBody RegisterSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.register(request));
    }

    @Operation(summary = "구독 해제", description = "endpoint 또는 token 값으로 구독을 비활성화합니다.")
    @DeleteMapping
    public ResponseEntity<Void> unregister(@RequestParam String endpointOrToken) {
        subscriptionService.unregister(endpointOrToken);
        return ResponseEntity.noContent().build();
    }
}
