package attune.admin.marketing.adapter.web;

import attune.admin.marketing.application.AdminMarketingPushService;
import attune.admin.marketing.application.dto.request.MarketingPushRequest;
import attune.admin.marketing.application.dto.response.MarketingPushResponse;
import attune.common.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 마케팅", description = "관리자 마케팅 발송 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/marketing")
public class AdminMarketingController {

    private final AdminMarketingPushService adminMarketingPushService;

    @Operation(summary = "마케팅 푸시 발송", description = "마케팅 수신 동의 ACTIVE 회원 전체에게 비동기 발송합니다.")
    @PostMapping("/push")
    public ResponseEntity<MarketingPushResponse> sendMarketingPush(
            @Valid @RequestBody MarketingPushRequest request
    ) {
        return ResponseEntity.ok(adminMarketingPushService.send(request));
    }
}
