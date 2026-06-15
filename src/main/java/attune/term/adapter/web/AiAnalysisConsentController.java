package attune.term.adapter.web;

import attune.common.ApiVersion;
import attune.term.application.TermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Analysis Consent", description = "AI 분석 동의 API")
@RestController
@RequestMapping(ApiVersion.V1 + "/ai-analysis-consent")
@RequiredArgsConstructor
public class AiAnalysisConsentController {

    private final TermService termService;

    @Operation(summary = "AI 분석 동의", description = "약물 치료 경과 리포트의 AI 분석 기능에 동의합니다.")
    @ApiResponse(responseCode = "204", description = "동의 완료")
    @PutMapping
    public ResponseEntity<Void> consent() {
        termService.consentAiAnalysis();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "AI 분석 동의 철회", description = "AI 분석 동의를 철회합니다. 기존 리포트의 기본 통계는 계속 제공됩니다.")
    @ApiResponse(responseCode = "204", description = "철회 완료")
    @DeleteMapping
    public ResponseEntity<Void> revoke() {
        termService.revokeAiAnalysis();
        return ResponseEntity.noContent().build();
    }
}
