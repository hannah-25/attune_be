package attune.term.adapter.web;

import attune.term.application.TermService;
import attune.term.application.dto.request.CreateTermRequest;
import attune.term.application.dto.response.CreateTermResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 약관", description = "관리자 약관 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/admin/terms")
public class AdminTermController {

    private final TermService termService;

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
