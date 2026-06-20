package attune.admin.audit.adapter.web;

import attune.admin.audit.application.AdminAuditLogService;
import attune.admin.audit.application.dto.AdminAuditLogResponse;
import attune.common.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "관리자 감사 로그", description = "관리자 회원 관리 작업 감사 로그 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/admin/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogService service;

    @GetMapping
    @Operation(summary = "최신 관리자 감사 로그 조회")
    public List<AdminAuditLogResponse> getAuditLogs(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "1 이상이어야 합니다.")
            @Max(value = 100, message = "100 이하여야 합니다.")
            int limit
    ) {
        return service.getLatest(limit);
    }
}
