package attune.term.adapter.web;

import attune.term.application.TermService;
import attune.term.application.dto.response.TermResponse;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Terms", description = "약관 API")
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @Operation(summary = "최신 약관 조회", description = "회원가입 시 표시할 최신 약관을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "약관 조회 성공")
    @GetMapping("/latest")
    public ResponseEntity<List<TermResponse>> getLatestTerm() {
        return ResponseEntity.ok(termService.getLatestTerm());
    }
}