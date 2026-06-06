package attune.ai.adapter.web;

import attune.ai.application.AiService;
import attune.ai.application.dto.request.GenerateTextRequest;
import attune.ai.application.dto.response.GenerateTextResponse;
import attune.common.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "Generative AI API")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/ai")
public class AiController {

    private final AiService aiService;

    @Operation(summary = "Generate text with Gemini Flash")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Text generated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/generate")
    public ResponseEntity<GenerateTextResponse> generate(@Valid @RequestBody GenerateTextRequest request) {
        return ResponseEntity.ok(aiService.generate(request));
    }
}
