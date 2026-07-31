package attune.support.adapter.web;

import attune.common.ApiVersion;
import attune.support.application.SupportInquiryService;
import attune.support.application.dto.request.CreateSupportInquiryRequest;
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

@Tag(name = "Support", description = "Support inquiry API")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/support/inquiries")
public class SupportInquiryController {

    private final SupportInquiryService supportInquiryService;

    @Operation(summary = "Create support inquiry")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inquiry created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<Void> createInquiry(@Valid @RequestBody CreateSupportInquiryRequest request) {
        supportInquiryService.createInquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    

}
