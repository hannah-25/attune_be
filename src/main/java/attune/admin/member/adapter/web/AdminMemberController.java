package attune.admin.member.adapter.web;

import attune.admin.member.application.AdminMemberService;
import attune.admin.member.application.dto.request.CancelWithdrawalRequest;
import attune.admin.member.application.dto.request.ChangeStatusRequest;
import attune.admin.member.application.dto.request.CompleteWithdrawalRequest;
import attune.admin.member.application.dto.response.AdminMemberListResponse;
import attune.admin.member.application.dto.response.AdminMemberResponse;
import attune.common.ApiVersion;
import attune.common.util.SecurityUtils;
import attune.user.application.UserPermanentDeletionService;
import attune.user.domain.model.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/admin/members")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final UserPermanentDeletionService userPermanentDeletionService;

    @GetMapping
    public ResponseEntity<AdminMemberListResponse> getMembers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminMemberService.getMembers(query, status, page, size));
    }

    @PostMapping("/{memberId}/status")
    public ResponseEntity<AdminMemberResponse> changeStatus(
            @PathVariable UUID memberId,
            @Valid @RequestBody ChangeStatusRequest request
    ) {
        return ResponseEntity.ok(adminMemberService.changeStatus(
                SecurityUtils.getCurrentUserUuid(),
                memberId,
                request
        ));
    }

    @PostMapping("/{memberId}/withdrawal/cancel")
    public ResponseEntity<AdminMemberResponse> cancelWithdrawal(
            @PathVariable UUID memberId,
            @Valid @RequestBody CancelWithdrawalRequest request
    ) {
        return ResponseEntity.ok(adminMemberService.cancelWithdrawal(
                SecurityUtils.getCurrentUserUuid(),
                memberId,
                request
        ));
    }

    @PostMapping("/{memberId}/withdrawal/complete")
    public ResponseEntity<Void> completeWithdrawal(
            @PathVariable UUID memberId,
            @Valid @RequestBody CompleteWithdrawalRequest request
    ) {
        userPermanentDeletionService.completeWithdrawalByAdmin(
                SecurityUtils.getCurrentUserUuid(),
                memberId,
                request.reason()
        );
        return ResponseEntity.noContent().build();
    }
}
