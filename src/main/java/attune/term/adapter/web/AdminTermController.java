package attune.term.adapter.web;

import attune.common.ApiVersion;

import attune.term.application.TermService;
import attune.term.application.dto.request.CreateTermRequest;
import attune.term.application.dto.response.AdminTermResponse;
import attune.term.application.dto.response.CreateTermResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "관리자 약관", description = "관리자 약관 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/terms")
public class AdminTermController {

    private final TermService termService;

    @Operation(summary = "약관 목록 조회", description = "전체 약관을 최신 등록순으로 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping
    public ResponseEntity<List<AdminTermResponse>> getAllTerms() {
        return ResponseEntity.ok(termService.getAllTermsForAdmin());
    }

    @Operation(summary = "약관 등록 및 이메일 발송")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PostMapping
    public ResponseEntity<CreateTermResponse> createTerm(@Valid @RequestBody CreateTermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(termService.createTerm(request));
    }
}
