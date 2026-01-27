package attune.common.web;


import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health", description = "헬스체크 API")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Operation(summary = "백엔드 헬스체크", description = "백엔드 애플리케이션의 상태를 확인합니다.")
    @Hidden
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "백엔드 정상 동작", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    @GetMapping
    public ResponseEntity<Map<String, String>> beHealthCheck() {
        Map<String, String> healthStatus = new HashMap<>();
        healthStatus.put("status", "OK");
        healthStatus.put("message", "Backend is running");
        return ResponseEntity.ok(healthStatus);
    }

    @Operation(summary = "데이터베이스 헬스체크", description = "데이터베이스 연결 상태를 확인합니다.")
    @Hidden
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "데이터베이스 연결 정상", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "503", description = "데이터베이스 연결 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/db")
    public ResponseEntity<Map<String, String>> dbHealthCheck() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Database connection is successful."));
        } catch (DataAccessException e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "ERROR", "message", "Database connection failed."));
        }
    }

}
